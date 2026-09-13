package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-694 续 / spec 947：ElasticBudgetPool 并发借还守恒压测——多会话并发
 * tryAcquire/release 下「Σheld ≤ capacity」与「surplus 不变式」恒成立
 * （池级单锁语义的并发正确性实证；G r47 压测模式）。
 */
class ElasticBudgetPoolStressTest {

    @Test
    void concurrentAcquireReleaseConervesCapacity() throws Exception {
        ElasticBudgetPool pool = new ElasticBudgetPool(40L, java.util.Map.of(
                "s1", 10L, "s2", 10L, "s3", 10L, "s4", 10L), null);
        long capacity = 40L;

        ExecutorService pool_exec = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

        for (int t = 0; t < 8; t++) {
            final String agent = "s" + (t % 4 + 1);
            futures.add(pool_exec.submit(() -> {
                try {
                    start.await();
                } catch (InterruptedException ignored) {
                }
                for (int i = 0; i < 500; i++) {
                    attempts.incrementAndGet();
                    if (pool.tryAcquire(agent, 1)) {
                        // 持有一段时间后归还（模拟在飞预算占用）
                        if ((i & 3) == 0) {
                            pool.release(agent, 1);
                        }
                    }
                }
            }));
        }
        start.countDown();
        pool_exec.shutdown();
        assertThat(pool_exec.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        // 终态守恒：Σheld + surplus == capacity（预算不灭不失）
        long sumHeld = 0;
        for (ElasticBudgetPool.Row row : pool.snapshot().values()) {
            sumHeld += row.held();
        }
        assertThat(sumHeld + pool.surplus()).isEqualTo(capacity);
        // 尝试数正确驱动（8 线程 × 500）
        assertThat(attempts.get()).isEqualTo(4000);
    }

    @Test
    void baseQuotaNeverEatenByOtherBorrowing() {
        // capacity=10，a/b 各 base=5，Σbase=capacity → surplus=0：
        // a 拿满自己 base 5 后，再借必须失败（不能吃 b 的 base）
        ElasticBudgetPool pool = new ElasticBudgetPool(10L, java.util.Map.of(
                "a", 5L, "b", 5L), null);
        for (int i = 0; i < 5; i++) {
            assertThat(pool.tryAcquire("a", 1)).isTrue();
        }
        assertThat(pool.tryAcquire("a", 1)).isFalse(); // surplus=0 拒借
        // b 的 base 保底可拿满
        for (int i = 0; i < 5; i++) {
            assertThat(pool.tryAcquire("b", 1)).isTrue();
        }
        assertThat(pool.heldOf("b")).isEqualTo(5L);
        assertThat(pool.heldOf("a")).isEqualTo(5L);
        // 守恒：10 held + 0 surplus == capacity
        assertThat(pool.snapshot().values().stream().mapToLong(r -> r.held()).sum()
                + pool.surplus()).isEqualTo(10L);
    }
}
