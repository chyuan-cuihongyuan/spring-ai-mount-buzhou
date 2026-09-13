package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.github.fppt.jedismock.RedisServer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStoreContract;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-683 / spec 930：租约契约接入 Redis store——RedisSessionLeaseStore 过
 * SessionLeaseStoreContract 九项检查（jedismock hermetic 基建，RedisStoresContractTest
 * 同款；真实 fence 语义自证）。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisLeaseContractTest {

    private RedisServer server;
    private RedisClient client;
    private BuzhouStores stores;

    @BeforeAll
    void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = RedisClient.create("redis://" + server.getHost() + ":" + server.getBindPort());
        stores = RedisBuzhouStores.createPooled(client, "buzhou:", Duration.ZERO,
                RedisBuzhouStores.DEFAULT_POOL_MAX_SIZE, null);
    }

    @AfterAll
    void tearDown() throws IOException {
        client.shutdown();
        server.stop();
    }

    @Test
    void redisLeaseStorePassesAllNineChecks() {
        var report = SessionLeaseStoreContract.verify(stores.sessionLeaseStore());

        assertThat(report.allPassed())
                .as(() -> "未过项：" + report.checks().stream()
                        .filter(c -> !c.passed()).map(SessionLeaseStoreContract.CheckResult::name)
                        .toList())
                .isTrue();
        assertThat(report.total()).isEqualTo(9);
        assertThat(report.passed()).isEqualTo(9);
    }
}
