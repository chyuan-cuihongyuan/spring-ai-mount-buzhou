package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.contract.AbstractBuzhouStoresContractTest;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
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
class PostgreSqlStoresContractTest extends AbstractBuzhouStoresContractTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    private BuzhouStores stores;

    @Override
    protected BuzhouStores stores() {
        if (stores == null) {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(org.postgresql.Driver.class);
            dataSource.setUrl(POSTGRES.getJdbcUrl());
            dataSource.setUsername(POSTGRES.getUsername());
            dataSource.setPassword(POSTGRES.getPassword());
            stores = JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.POSTGRESQL).stores();
        }
        return stores;
    }

    @Override
    protected void cleanUp() {
    }

    /** ticket 32 / spec 13 §stores-7：真实 PG 上的并发摘要版本唯一性（FOR UPDATE 序列化）。 */
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
