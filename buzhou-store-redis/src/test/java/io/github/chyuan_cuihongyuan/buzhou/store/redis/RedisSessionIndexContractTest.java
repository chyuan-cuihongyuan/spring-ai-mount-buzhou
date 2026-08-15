package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.contract.AbstractSessionIndexContractTest;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 实现契约接入（Testcontainers redis:7-alpine，无 Docker 跳过）+ 持久语义
 * （新 store 实例同 Redis = 重启后索引仍在）。
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisSessionIndexContractTest extends AbstractSessionIndexContractTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static final String PREFIX = "contract:" + System.nanoTime() + ":";

    private RedisClient client;
    private RedisSessionIndexStore store;

    @Override
    protected SessionIndexStore index() {
        if (store == null) {
            client = RedisClient.create(
                    "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
            store = RedisSessionIndexStore.create(client, PREFIX);
        }
        return store;
    }

    @Override
    @AfterEach
    protected void cleanUp() {
        if (store != null) {
            // 前缀清场：删本套件键空间（ZSET 成员 + info 行）
            store.list(new SessionIndexQuery(null, null, null, null, null, 0, 200))
                    .forEach(info -> store.delete(info.sessionId()));
        }
    }

    /** 持久语义：新 store 实例（模拟重启）同 Redis 可见全部行。 */
    @Test
    void rowsSurviveNewStoreInstanceOverSameRedis() {
        SessionIndexStore index = index();
        index.upsert(new SessionInfo("persist-1", "app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, System.currentTimeMillis(), 3, Map.of("k", "v")));

        RedisSessionIndexStore restarted = RedisSessionIndexStore.create(client, PREFIX);

        assertThat(restarted.get("persist-1")).isPresent();
        assertThat(restarted.list(new SessionIndexQuery(
                null, null, null, "k", "v", 0, 10)))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("persist-1");
    }
}
