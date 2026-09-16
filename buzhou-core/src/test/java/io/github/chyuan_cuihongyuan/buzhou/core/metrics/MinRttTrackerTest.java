package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2015 / T3132：min-RTT 滑窗滤波合同——窗内最小跟踪、滑出失效
 * 重算（网络改善可追认）、新鲜度时刻、空窗语义、畸形 fail-fast。
 */
class MinRttTrackerTest {

    @Test
    void minimumWithinWindowShouldBeTracked() {
        MinRttTracker tracker = new MinRttTracker(10_000L);
        tracker.observe(120, 0);
        tracker.observe(80, 1_000);
        tracker.observe(150, 2_000);
        assertThat(tracker.minRtt(3_000)).isEqualTo(80L); // 均值被膨胀样本拉高，最小值稳定
        assertThat(tracker.sampleCount()).isEqualTo(3);
        assertThat(tracker.lastFreshMinAt()).isEqualTo(1_000L); // 80 在 t=1000 见到
    }

    @Test
    void slidOutMinimumShouldExpireAndRecompute() {
        MinRttTracker tracker = new MinRttTracker(1_000L);
        tracker.observe(50, 0);    // 将滑出的最小
        tracker.observe(200, 500);
        assertThat(tracker.minRtt(999)).isEqualTo(50L);   // 界内（<window）
        assertThat(tracker.minRtt(1_000)).isEqualTo(200L); // 50 滑出 → 次小接管
        assertThat(tracker.sampleCount()).isEqualTo(1);
    }

    @Test
    void networkImprovementShouldBeReadopted() {
        MinRttTracker tracker = new MinRttTracker(10_000L);
        tracker.observe(300, 0);
        tracker.observe(90, 5_000);  // 网络改善——新最小
        assertThat(tracker.minRtt(6_000)).isEqualTo(90L);
        assertThat(tracker.lastFreshMinAt()).isEqualTo(5_000L);
    }

    @Test
    void emptyWindowShouldReturnZeroWithNoSamples() {
        MinRttTracker tracker = new MinRttTracker(100L);
        assertThat(tracker.minRtt(0)).isZero();
        assertThat(tracker.sampleCount()).isZero();
        assertThat(tracker.lastFreshMinAt()).isEqualTo(-1L); // 从未见过
        tracker.observe(10, 0);
        assertThat(tracker.minRtt(1_000)).isZero(); // 全部滑出后回零
        assertThat(tracker.sampleCount()).isZero();
    }

    @Test
    void equalMinimumShouldNotRefreshFreshness() {
        MinRttTracker tracker = new MinRttTracker(10_000L);
        tracker.observe(100, 0);    // 首个即最小（新鲜 t=0）
        tracker.observe(100, 5_000); // 持平非新最小——不刷新
        assertThat(tracker.lastFreshMinAt()).isEqualTo(0L);
        tracker.observe(99, 6_000);  // 更小——刷新
        assertThat(tracker.lastFreshMinAt()).isEqualTo(6_000L);
    }

    @Test
    void zeroRttShouldBeTrackable() {
        MinRttTracker tracker = new MinRttTracker(10_000L);
        tracker.observe(0, 0); // 零 RTT（本地命中）合法
        assertThat(tracker.minRtt(1)).isZero();
        assertThat(tracker.sampleCount()).isEqualTo(1);
    }

    @Test
    void malformedInputsShouldFailFast() {
        MinRttTracker tracker = new MinRttTracker();
        assertThatThrownBy(() -> new MinRttTracker(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.observe(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.observe(1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
