package io.github.chyuan_cuihongyuan.buzhou.store.redis;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 真实 Redis 契约套件（Testcontainers，{@code redis:7-alpine}）。
 *
 * <p>本机无 Docker 时自动跳过（{@code disabledWithoutDocker=true}，与 jdbc 模块 Postgres/MySQL 同口径）；
 * CI 环境跑全量契约 + unit-of-work 原子提交/回滚，确认与 {@link RedisStoresContractTest}（jedis-mock）
 * 在真实 Redis 语义上一致。ticket 32：经 {@link RedisBuzhouStores#createPooled} 装配（池化 UoW 路径）。
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisStoresTestcontainersTest extends AbstractBuzhouStoresContractTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private RedisClient client;
    private BuzhouStores stores;
    private StatefulRedisConnection<String, String> adminConn;
    private RedisCommands<String, String> admin;

    @BeforeAll
    void setUp() {
        // REDIS 已由 @Container 启动
        client = RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        stores = RedisBuzhouStores.createPooled(client, "buzhou:", Duration.ZERO,
                RedisBuzhouStores.DEFAULT_POOL_MAX_SIZE, null);
        adminConn = client.connect();
        admin = adminConn.sync();
    }

    @AfterAll
    void tearDown() {
        adminConn.close();
        client.shutdown();
        // REDIS 由 @Container 管理，无需手动 stop
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
