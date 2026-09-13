package io.github.chyuan_cuihongyuan.buzhou.observability;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 812 / T1126：内存限流器回归——限额判定与精确边界/拒收不记账/
 * release 归账防负/并发守恒/零权重恒过/fail-fast。
 */
class PipelineMemoryLimiterTest {

    @Test
    void admitsUnderAndAtLimitRefusesOver() {
        PipelineMemoryLimiter limiter = new PipelineMemoryLimiter(100);

        assertThat(limiter.tryAdmit(60)).isTrue();
        assertThat(limiter.tryAdmit(40)).isTrue(); // 恰达上限
        assertThat(limiter.currentBytes()).isEqualTo(100);
        assertThat(limiter.tryAdmit(1)).isFalse(); // 超限拒收
        assertThat(limiter.currentBytes()).isEqualTo(100); // 拒收不记账
        assertThat(limiter.stats().refused()).isEqualTo(1);
        assertThat(limiter.stats().admitted()).isEqualTo(2);
    }

    @Test
    void oversizeItemRefused() {
        PipelineMemoryLimiter limiter = new PipelineMemoryLimiter(50);
        assertThat(limiter.tryAdmit(51)).isFalse(); // 单项超上限——永不入账
        assertThat(limiter.currentBytes()).isZero();
        assertThat(limiter.tryAdmit(50)).isTrue();
    }

    @Test
    void releaseRestoresBudgetWithoutGoingNegative() {
        PipelineMemoryLimiter limiter = new PipelineMemoryLimiter(100);
        limiter.tryAdmit(70);
        limiter.release(70);
        assertThat(limiter.currentBytes()).isZero();
        assertThat(limiter.stats().released()).isEqualTo(1);
        limiter.release(999); // 超发归账防御
        assertThat(limiter.currentBytes()).isZero();
        assertThat(limiter.tryAdmit(100)).isTrue(); // 预算完整恢复
    }

    @Test
    void zeroAndNegativeWeightsPassFreely() {
        PipelineMemoryLimiter limiter = new PipelineMemoryLimiter(10);
        assertThat(limiter.tryAdmit(0)).isTrue();
        assertThat(limiter.tryAdmit(-5)).isTrue();
        assertThat(limiter.currentBytes()).isZero();
        assertThat(limiter.stats().admitted()).isZero(); // 忽略不计 admit
        limiter.release(0);
        limiter.release(-1);
        assertThat(limiter.stats().released()).isZero();
    }

    @Test
    void concurrentAdmitsNeverExceedBudget() throws Exception {
        PipelineMemoryLimiter limiter = new PipelineMemoryLimiter(1_000);
        int threads = 8;
        int perThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicLong refusedCount = new AtomicLong();
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        if (!limiter.tryAdmit(1)) {
                            refusedCount.incrementAndGet();
                        }
                    }
                    done.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // 不变量：成功入账恰好 = 预算容量（1000 × 1 字节）
        assertThat(limiter.currentBytes()).isEqualTo(1_000);
        assertThat(limiter.stats().admitted()).isEqualTo(1_000);
        assertThat(limiter.stats().refused()).isEqualTo(threads * perThread - 1_000);
        assertThat(refusedCount.get()).isEqualTo(threads * perThread - 1_000);
    }

    @Test
    void failFastOnBadMax() {
        assertThatThrownBy(() -> new PipelineMemoryLimiter(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PipelineMemoryLimiter(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
