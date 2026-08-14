package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.github.fppt.jedismock.RedisServer;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 13 §stores-7 / ticket 32：Redis 脏数据隔离契约（hermetic，jedis-mock）——
 * 一条坏 JSON → load 成功返回其余消息 + {@code corruptionCount()} 计数不静默。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisDirtyDataIsolationTest {

    private RedisServer server;
    private RedisClient client;
    private RedisMessageStore messageStore;
    private RedisKeys keys;
    private StatefulRedisConnection<String, String> adminConn;

    @BeforeAll
    void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = RedisClient.create("redis://" + server.getHost() + ":" + server.getBindPort());
        BuzhouStores stores = RedisBuzhouStores.createPooled(client, "buzhou:", Duration.ZERO,
                RedisBuzhouStores.DEFAULT_POOL_MAX_SIZE, null);
        messageStore = (RedisMessageStore) stores.messageStore();
        keys = new RedisKeys("buzhou:");
        adminConn = client.connect();
    }

    @AfterAll
    void tearDown() throws IOException {
        adminConn.close();
        client.shutdown();
        server.stop();
    }

    @BeforeEach
    void flushBetweenTests() {
        adminConn.sync().flushdb();
    }

    @Test
    void shouldLoadRemainingMessages_whenOneListEntryHasCorruptJson() {
        String sessionId = "dirty-" + UUID.randomUUID();
        BuzhouMessage good1 = msg(sessionId, 1, 0);
        BuzhouMessage good2 = msg(sessionId, 3, 0);
        // 直接向 LIST 头部塞坏 JSON（模拟外部写入 / 序列化版本不兼容遗留数据）
        adminConn.sync().rpush(keys.messageList(sessionId),
                "{not-valid-json", RedisJson.write(good1), RedisJson.write(good2));
        long corruptedBefore = messageStore.corruptionCount();

        List<BuzhouMessage> loaded = messageStore.load(sessionId);

        assertThat(loaded).extracting(BuzhouMessage::id)
                .containsExactly(good1.id(), good2.id());
        // 计数器为 store 级累计（PER_CLASS 生命周期跨用例共享），按增量断言
        assertThat(messageStore.corruptionCount()).isEqualTo(corruptedBefore + 1L); // 跳过不静默
    }

    @Test
    void shouldReturnEmpty_whenFindByIdHitsCorruptJson() {
        String id = "corrupt-id-" + UUID.randomUUID();
        adminConn.sync().set(keys.messageById(id), "not-json{");
        long corruptedBefore = messageStore.corruptionCount();

        assertThat(messageStore.findById(id)).isEmpty();
        assertThat(messageStore.corruptionCount()).isEqualTo(corruptedBefore + 1L);
    }

    private BuzhouMessage msg(String sessionId, int turnSeq, int seqInTurn) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turnSeq, seqInTurn,
                Role.USER, "content-" + turnSeq, List.of(), null, null, null, Map.of(), Instant.now());
    }
}
