package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7026：TimeBucketReservoir 合同——滑动时间窗样本库。
 * 窗口过滤手锚；水位单调 fail-fast；陈旧清理；边界含；
 * fail-fast。
 */
class TimeBucketReservoirTest {

    @Test
    void windowFilteringHandAnchors() {
        TimeBucketReservoir reservoir = new TimeBucketReservoir();
        reservoir.record(1, 100);
        reservoir.record(2, 200);
        reservoir.record(3, 300);
        assertThat(reservoir.snapshot(300, 50)).containsExactly(3L);
        assertThat(reservoir.snapshot(300, 150)).containsExactly(2L, 3L);
        assertThat(reservoir.snapshot(300, 1000)).containsExactly(1L, 2L, 3L);
        assertThat(reservoir.snapshot(250, 1000)).containsExactly(1L, 2L);
        assertThat(reservoir.size()).isEqualTo(3);
    }

    @Test
    void monotonicWatermarkFailsFast() {
        TimeBucketReservoir reservoir = new TimeBucketReservoir();
        reservoir.record(1, 1000);
        assertThatThrownBy(() -> reservoir.record(2, 999))
                .isInstanceOf(IllegalArgumentException.class);
        reservoir.record(2, 1000);
        assertThat(reservoir.size()).isEqualTo(2);
    }

    @Test
    void evictionPhysicallyRemovesStale() {
        TimeBucketReservoir reservoir = new TimeBucketReservoir();
        reservoir.record(1, 100);
        reservoir.record(2, 200);
        reservoir.record(3, 300);
        assertThat(reservoir.evictBefore(250)).isEqualTo(2);
        assertThat(reservoir.size()).isEqualTo(1);
        assertThat(reservoir.snapshot(400, 1000)).containsExactly(3L);
    }

    @Test
    void failFastContract() {
        TimeBucketReservoir reservoir = new TimeBucketReservoir();
        assertThatThrownBy(() -> reservoir.snapshot(100, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reservoir.snapshot(100, -5)).isInstanceOf(IllegalArgumentException.class);
        assertThat(reservoir.snapshot(100, 10)).isEmpty();
    }
}
