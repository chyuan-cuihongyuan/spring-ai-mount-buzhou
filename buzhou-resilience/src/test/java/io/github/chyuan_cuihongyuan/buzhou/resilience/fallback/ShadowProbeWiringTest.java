package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 影子探针接线测试（spec 1623 / T2397–T2398 / impl 1176）：采样对照语义
 * （agreed/diverged 双计数 + 采样确定性）+ 旁路异常吞计——spec 189 孤类接线。
 */
class ShadowProbeWiringTest {

    @Test
    void sampledProbeComparesAndCountsAgreement() throws Exception {
        ShadowProbe probe = new ShadowProbe(100); // 全采样
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            probe.probe("k1", "answer", () -> "answer", executor);   // 一致
            probe.probe("k2", "answer", () -> "different", executor); // 分歧
            Thread.sleep(200); // 旁路异步——等结算
        }
        ShadowProbe.Snapshot snapshot = probe.snapshot();
        assertThat(snapshot.sampled()).isEqualTo(2);
        assertThat(snapshot.agreed()).isEqualTo(1);
        assertThat(snapshot.diverged()).isEqualTo(1);
        assertThat(snapshot.recentDivergedKeys()).containsExactly("k2");
    }

    @Test
    void deterministicSamplingStablePerKey() {
        ShadowProbe probe = new ShadowProbe(50);
        boolean first = probe.sampled("stable-key");
        for (int i = 0; i < 10; i++) {
            assertThat(probe.sampled("stable-key")).isEqualTo(first); // 同 key 同判定
        }
    }

    @Test
    void zeroRateNeverSamples() {
        ShadowProbe probe = new ShadowProbe(0);
        assertThat(probe.sampled("any")).isFalse();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            probe.probe("any", "x", () -> "y", executor); // 未命中零执行
        }
        assertThat(probe.snapshot().sampled()).isZero();
    }

    @Test
    void shadowFailureCountedAsErrorNotThrown() throws Exception {
        ShadowProbe probe = new ShadowProbe(100);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            probe.probe("k", "answer", () -> {
                throw new IllegalStateException("影子路故障");
            }, executor);
            Thread.sleep(200);
        }
        assertThat(probe.snapshot().errors()).isEqualTo(1); // 旁路异常全吞计 error
    }
}
