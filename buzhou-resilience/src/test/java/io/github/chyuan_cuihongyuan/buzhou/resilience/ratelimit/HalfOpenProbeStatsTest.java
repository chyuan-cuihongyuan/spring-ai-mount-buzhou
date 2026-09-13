package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 836 / T1174：半开探测读数回归——成败计数/近窗率/连续失败 streak/
 * streak 归零/封顶/脏入参。
 */
class HalfOpenProbeStatsTest {

    @Test
    void countersStreakAndRecentRate() {
        HalfOpenProbeStats stats = new HalfOpenProbeStats();
        stats.record("m", true);
        stats.record("m", false);
        stats.record("m", false);
        stats.record("m", true);
        stats.record("m", true);

        HalfOpenProbeStats.ProbeStats s = stats.stats("m");
        assertThat(s.successes()).isEqualTo(3);
        assertThat(s.failures()).isEqualTo(2);
        assertThat(s.consecutiveFailures()).isZero(); // 末次成功清零
        assertThat(s.recentSuccessRate()).isCloseTo(0.6, within(1e-9));
    }

    @Test
    void streakCountsConsecutiveFailures() {
        HalfOpenProbeStats stats = new HalfOpenProbeStats();
        stats.record("m", true);
        stats.record("m", false);
        stats.record("m", false);
        assertThat(stats.stats("m").consecutiveFailures()).isEqualTo(2);
        stats.record("m", true);
        assertThat(stats.stats("m").consecutiveFailures()).isZero();
    }

    @Test
    void windowSlidesRecentBehavior() {
        HalfOpenProbeStats stats = new HalfOpenProbeStats();
        stats.record("m", false); // 全败早期（滑出后近窗全成功）
        for (int i = 0; i < HalfOpenProbeStats.WINDOW; i++) {
            stats.record("m", true);
        }
        assertThat(stats.stats("m").recentSuccessRate()).isCloseTo(1.0, within(1e-9));
        assertThat(stats.stats("m").failures()).isEqualTo(1); // 累计保留
    }

    @Test
    void modelsCappedAndUnknownNull() {
        HalfOpenProbeStats stats = new HalfOpenProbeStats();
        for (int i = 0; i < HalfOpenProbeStats.MAX_MODELS + 3; i++) {
            stats.record("x" + i, true);
        }
        assertThat(stats.truncated()).isTrue();
        assertThat(stats.stats("x0")).isNotNull();
        assertThat(stats.stats("x" + (HalfOpenProbeStats.MAX_MODELS + 2))).isNull();
        assertThat(stats.stats(null)).isNull();
        stats.record(null, true);
    }
}
