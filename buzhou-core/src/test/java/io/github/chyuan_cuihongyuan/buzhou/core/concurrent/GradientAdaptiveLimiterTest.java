package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 梯度式自适应并发测试（spec 1617 / T2385–T2386 / impl 1170）：延迟劣化乘性下调
 * （失败前规避）、明显变快加性上调、容错带防抖、warmup 学习期、上限联动 tryAcquire、
 * 配置校验。Netflix Gradient2 / Envoy adaptive_concurrency 思想。
 */
class GradientAdaptiveLimiterTest {

    /** 喂 n 个恒定延迟（把两窗 EMA 稳定到 value 附近）。 */
    private static void feed(GradientAdaptiveLimiter limiter, long millis, int n) {
        for (int i = 0; i < n; i++) {
            limiter.record(millis);
        }
    }

    @Test
    void latencyDegradationShrinksLimitBeforeFailure() {
        GradientAdaptiveLimiter limiter =
                new GradientAdaptiveLimiter(new GradientAdaptiveLimiter.Config(4, 64, 0.2));
        feed(limiter, 100, 30); // 基线 100ms
        feed(limiter, 20, 40);  // 先变快——加性上涨（脱离 minLimit 才有下调空间）
        int before = limiter.limit();
        assertThat(before).isGreaterThan(4);
        feed(limiter, 300, 10); // 劣化 3 倍：recent EMA 快速升 → gradient << 0.8
        assertThat(limiter.limit()).isLessThan(before); // 乘性下调（零失败发生）
        assertThat(limiter.view().adjustmentsDown()).isPositive();
    }

    @Test
    void latencyImprovementGrowsLimitAdditively() {
        GradientAdaptiveLimiter limiter =
                new GradientAdaptiveLimiter(new GradientAdaptiveLimiter.Config(4, 64, 0.2));
        feed(limiter, 100, 30); // 基线
        feed(limiter, 20, 30);  // 变快 5 倍：recent EMA 快速降 → gradient >> 1.2
        assertThat(limiter.limit()).isGreaterThan(4); // 加性上涨
        assertThat(limiter.view().adjustmentsUp()).isPositive();
        assertThat(limiter.view().gradient()).isGreaterThan(1.2);
    }

    @Test
    void toleranceBandHoldsLimitSteady() {
        GradientAdaptiveLimiter limiter =
                new GradientAdaptiveLimiter(new GradientAdaptiveLimiter.Config(4, 64, 0.5));
        feed(limiter, 100, 30);
        int before = limiter.limit();
        feed(limiter, 130, 20); // 劣化 30% < 50% 容错带——不动
        assertThat(limiter.limit()).isEqualTo(before);
        assertThat(limiter.view().adjustmentsDown()).isZero();
    }

    @Test
    void warmupLearnsWithoutAdjusting() {
        GradientAdaptiveLimiter limiter =
                new GradientAdaptiveLimiter(new GradientAdaptiveLimiter.Config(4, 64, 0.2));
        feed(limiter, 5, GradientAdaptiveLimiter.WARMUP_SAMPLES); // 恰在学习期内（极快也不升）
        assertThat(limiter.limit()).isEqualTo(4);
        assertThat(limiter.view().adjustmentsUp()).isZero();
        assertThat(limiter.view().baselineEmaMillis()).isEqualTo(5.0); // 学习已发生
    }

    @Test
    void acquireRespectsDynamicLimitAndReleases() {
        GradientAdaptiveLimiter limiter =
                new GradientAdaptiveLimiter(new GradientAdaptiveLimiter.Config(4, 64, 0.2));
        feed(limiter, 100, 30);
        int limit = limiter.limit();
        for (int i = 0; i < limit; i++) {
            assertThat(limiter.tryAcquire()).isTrue();
        }
        assertThat(limiter.tryAcquire()).isFalse(); // 超动态上限 fail-fast
        limiter.release();
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void maxLimitCapsAdditiveGrowth() {
        GradientAdaptiveLimiter limiter =
                new GradientAdaptiveLimiter(new GradientAdaptiveLimiter.Config(2, 6, 0.2));
        feed(limiter, 100, 30);
        feed(limiter, 10, 200); // 持续极快——加性上涨封顶 maxLimit
        assertThat(limiter.limit()).isEqualTo(6);
    }

    @Test
    void invalidConfigFailsFast() {
        assertThatThrownBy(() -> new GradientAdaptiveLimiter.Config(0, 8, 0.2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GradientAdaptiveLimiter.Config(4, 64, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
