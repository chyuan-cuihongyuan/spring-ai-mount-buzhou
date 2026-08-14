package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.contract.AbstractBuzhouStoresContractTest;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlStoresContractTest extends AbstractBuzhouStoresContractTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    private BuzhouStores stores;

    @Override
    protected BuzhouStores stores() {
        if (stores == null) {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
            dataSource.setUrl(MYSQL.getJdbcUrl());
            dataSource.setUsername(MYSQL.getUsername());
            dataSource.setPassword(MYSQL.getPassword());
            stores = JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.MYSQL).stores();
        }
        return stores;
    }

    @Override
    protected void cleanUp() {
    }

    /** ticket 32 / spec 13 §stores-7：真实 MySQL 上的并发摘要版本唯一性（FOR UPDATE + 撞键重试）。 */
    @Test
    void shouldSaveWithUniqueVersions_whenTwoThreadsSaveConcurrently() throws Exception {
        int savesPerThread = 25;
        int threadCount = 2;
        String sessionId = "sum-conc-" + UUID.randomUUID();
        Set<Long> versions = ConcurrentHashMap.newKeySet();
        CyclicBarrier gate = new CyclicBarrier(threadCount);
        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            Future<?>[] futures = new Future<?>[threadCount];
            for (int t = 0; t < threadCount; t++) {
                futures[t] = pool.submit(() -> {
                    try {
                        gate.await(30, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new IllegalStateException("并发起跑门等待失败", e);
                    }
                    for (int i = 0; i < savesPerThread; i++) {
                        versions.add(stores().summaryStore().save(sessionId, summary(sessionId, i)));
                    }
                    return null;
                });
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        }
        assertThat(versions).hasSize(threadCount * savesPerThread);
    }

    private io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary summary(String sessionId, int i) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary(
                sessionId, 0L, java.util.Map.of("P0", "v" + i), 10, java.time.Instant.now());
    }
}
