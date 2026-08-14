package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 13 §stores-7 / ticket 32：摘要版本号原子生成（FOR UPDATE 序列化 + 撞唯一索引重试）
 * 的并发正确性证明——双线程各压 N 次 save，全部成功且版本无重复。
 */
class H2SummaryConcurrencyTest {

    /** 每线程压测次数（双线程合计 50 版本，足够暴露旧 MAX(version)+1 读改写竞态）。 */
    private static final int SAVES_PER_THREAD = 25;

    private static final int THREAD_COUNT = 2;

    private JdbcBuzhouRecoveryStores stores;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:summary-conc-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        stores = JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.H2);
    }

    @Test
    void shouldSaveWithUniqueVersions_whenTwoThreadsSaveConcurrently() throws Exception {
        String sessionId = "sum-conc-" + UUID.randomUUID();
        Set<Long> allVersions = ConcurrentHashMap.newKeySet();
        CyclicBarrier startGate = new CyclicBarrier(THREAD_COUNT);
        try (ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT)) {
            Future<?>[] futures = new Future<?>[THREAD_COUNT];
            for (int t = 0; t < THREAD_COUNT; t++) {
                futures[t] = pool.submit(() -> {
                    await(startGate);
                    for (int i = 0; i < SAVES_PER_THREAD; i++) {
                        long version = stores.summaryStore().save(sessionId,
                                new StructuredSummary(sessionId, 0L, Map.of("P0", "v" + i), 10, Instant.now()));
                        allVersions.add(version);
                    }
                    return null;
                });
            }
            // 任一线程抛异常（含并发撞唯一索引未恢复）都会在此 FAIL
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        }

        int total = THREAD_COUNT * SAVES_PER_THREAD;
        assertThat(allVersions).hasSize(total); // 版本号无重复
        assertThat(stores.summaryStore().history(sessionId, total)).hasSize(total);
        assertThat(stores.summaryStore().latest(sessionId))
                .hasValueSatisfying(s -> assertThat(s.version()).isEqualTo(total));
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("并发起跑门等待失败", e);
        }
    }
}
