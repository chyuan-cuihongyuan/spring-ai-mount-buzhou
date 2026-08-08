package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.github.fppt.jedismock.RedisServer;
import io.github.chyuan_cuihongyuan.buzhou.core.contract.AbstractBuzhouStoresContractTest;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis 契约套件（hermetic：进程内 jedis-mock，H2 之于 JDBC 的等价物）。
 * 继承 {@link AbstractBuzhouStoresContractTest} 跑全量 SPI 契约；额外补 unit-of-work 原子提交/回滚。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisStoresContractTest extends AbstractBuzhouStoresContractTest {

    private RedisServer server;
    private RedisClient client;
    private BuzhouStores stores;
    private StatefulRedisConnection<String, String> adminConn;
    private RedisCommands<String, String> admin;

    @BeforeAll
    void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = RedisClient.create("redis://" + server.getHost() + ":" + server.getBindPort());
        stores = RedisBuzhouStores.create(client, "buzhou:");
        adminConn = client.connect();
        admin = adminConn.sync();
    }

    @AfterAll
    void tearDown() throws IOException {
        adminConn.close();
        client.shutdown();
        server.stop();
    }

    @BeforeEach
    void flushBetweenTests() {
        admin.flushdb();
    }

    @Override
    protected BuzhouStores stores() {
        return stores;
    }

    @Override
    protected void cleanUp() {
        admin.flushdb();
    }

    @Test
    void unitOfWorkCommitsAtomicallyOnSuccess() {
        String sessionId = "commit-" + UUID.randomUUID();
        stores().unitOfWork().executeInTransaction(sessionId, () -> {
            stores().messageStore().append(sessionId, List.of(msg(sessionId, 1, 0, Role.USER)));
            stores().sessionStateStore().put(sessionId,
                    new StateEntry("fact.x", "v", "hook", 1, null, Instant.now()));
            return null;
        });

        assertThat(stores().messageStore().load(sessionId)).hasSize(1);
        assertThat(stores().sessionStateStore().getAll(sessionId)).hasSize(1);
    }

    @Test
    void unitOfWorkRollsBackAllWritesOnFailure() {
        String sessionId = "rollback-" + UUID.randomUUID();
        assertThatThrownBy(() -> stores().unitOfWork().executeInTransaction(sessionId, () -> {
            stores().messageStore().append(sessionId, List.of(msg(sessionId, 1, 0, Role.USER)));
            stores().sessionStateStore().put(sessionId,
                    new StateEntry("fact.x", "v", "hook", 1, null, Instant.now()));
            throw new IllegalStateException("simulated mid-turn failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(stores().messageStore().load(sessionId)).isEmpty();
        assertThat(stores().sessionStateStore().getAll(sessionId)).isEmpty();
    }
}
