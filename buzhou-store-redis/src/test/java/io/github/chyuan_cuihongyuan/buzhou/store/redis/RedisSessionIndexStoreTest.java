package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话索引 Redis 实现行为测试（spec 30 / T109 / impl-84）：Testcontainers redis:7-alpine，
 * 与既有契约套件同守卫（无 Docker 环境跳过）。
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisSessionIndexStoreTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private RedisSessionIndexStore index;

    private void connect() {
        RedisClient client = RedisClient.create(
                "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        index = RedisSessionIndexStore.create(client, "buzhou:");
    }

    @AfterEach
    void tearDown() {
        if (index != null) {
            index.close();
        }
    }

    private static SessionInfo info(String sessionId, String appId, String status, long lastActive,
            Map<String, String> tags) {
        return new SessionInfo(sessionId, appId, "ag", status, 1000L, lastActive, 1, tags);
    }

    @Test
    void upsertListTagFilterAndDelete() {
        connect();
        index.upsert(info("r-old", "app-a", SessionInfo.STATUS_ACTIVE, 1000L, Map.of()));
        index.upsert(info("r-new", "app-a", SessionInfo.STATUS_ACTIVE, 3000L, Map.of("env", "prod")));
        index.upsert(info("r-b", "app-b", SessionInfo.STATUS_ACTIVE, 4000L, Map.of("env", "prod")));

        // lastActive 倒序 + appId 过滤
        List<SessionInfo> appA = index.list(new SessionIndexQuery(
                "app-a", null, null, null, null, 0, 10));
        assertThat(appA).extracting(SessionInfo::sessionId).containsExactly("r-new", "r-old");

        // tag 过滤（跨 app）
        List<SessionInfo> prod = index.list(new SessionIndexQuery(
                null, null, null, "env", "prod", 0, 10));
        assertThat(prod).extracting(SessionInfo::sessionId).containsExactly("r-b", "r-new");

        // get / delete 幂等
        assertThat(index.get("r-old")).isPresent();
        index.delete("r-old");
        index.delete("r-old");
        assertThat(index.get("r-old")).isEmpty();
        assertThat(index.list(new SessionIndexQuery(
                "app-a", null, null, null, null, 0, 10)))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("r-new");
    }
}
