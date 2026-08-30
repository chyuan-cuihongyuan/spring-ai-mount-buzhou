package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 189 / T562：影子读回归——采样确定性 / 一致与分歧计数+样本 / 异常吞 /
 * 零采样零执行 / 异步不阻塞。
 */
class ShadowProbeTest {

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
    }

    @Test
    void samplingIsDeterministicAndBounded() {
        ShadowProbe all = new ShadowProbe(100);
        ShadowProbe none = new ShadowProbe(0);
        ShadowProbe some = new ShadowProbe(30);

        assertThat(all.sampled("any-key")).isTrue();
        assertThat(none.sampled("any-key")).isFalse();
        for (int i = 0; i < 100; i++) {
            assertThat(some.sampled("k" + i))
                    .isEqualTo(some.sampled("k" + i)); // 同 key 同判定
        }
        assertThatThrownBy(() -> new ShadowProbe(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void agreedAndDivergedCountedWithSampleRing() throws Exception {
        ShadowProbe probe = new ShadowProbe(100);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            probe.probe("same", "答案", () -> "答案", pool);
            probe.probe("diff-1", "答案 A", () -> "答案 B", pool);
            probe.probe("diff-2", "答案 A", () -> "答案 C", pool);
            await(() -> probe.snapshot().agreed() + probe.snapshot().diverged() == 3);
        }

        ShadowProbe.Snapshot snapshot = probe.snapshot();
        assertThat(snapshot.sampled()).isEqualTo(3);
        assertThat(snapshot.agreed()).isEqualTo(1);
        assertThat(snapshot.diverged()).isEqualTo(2);
        assertThat(snapshot.recentDivergedKeys()).containsExactly("diff-2", "diff-1"); // 新→旧
    }

    @Test
    void shadowFailureIsSwallowedAsErrorCount() throws Exception {
        ShadowProbe probe = new ShadowProbe(100);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            probe.probe("boom", "ok", () -> {
                throw new IllegalStateException("shadow down");
            }, pool);
            await(() -> probe.snapshot().errors() == 1);
            assertThat(probe.snapshot().diverged()).isZero(); // 异常≠分歧
        }
    }

    @Test
    void zeroRateNeverExecutesShadow() throws Exception {
        ShadowProbe probe = new ShadowProbe(0);
        AtomicInteger executions = new AtomicInteger();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            probe.probe("k", "v", () -> {
                executions.incrementAndGet();
                return "v";
            }, pool);
            Thread.sleep(100); // 给异步窗口（不应有任何提交）
        }
        assertThat(executions.get()).isZero();
        assertThat(probe.snapshot().sampled()).isZero(); // 零成本
    }

    @Test
    void probeIsAsyncAndDoesNotBlockMainPath() throws Exception {
        ShadowProbe probe = new ShadowProbe(100);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            long start = System.nanoTime();
            probe.probe("slow-shadow", "v", () -> {
                release.await();
                return "v";
            }, pool);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertThat(elapsedMs).isLessThan(500); // 影子慢不拖主路
            release.countDown();
            await(() -> probe.snapshot().agreed() == 1);
        }
    }

    @Test
    void divergenceRingIsBounded() throws Exception {
        ShadowProbe probe = new ShadowProbe(100);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 40; i++) {
                probe.probe("k" + i, "a", () -> "b", pool);
            }
            await(() -> probe.snapshot().diverged() == 40);
            List<String> ring = probe.snapshot().recentDivergedKeys();
            // 环形有界：入环序=异步完成序（非确定），只断言容量与键不重
            assertThat(ring).hasSize(32);
            assertThat(java.util.Set.copyOf(ring)).hasSize(32);
        }
    }
}
