package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 841 / T1184：空闲直方回归——默认边界落桶/自定义边界/负值忽略/
 * 总数与最长/标签人话。
 */
class IdleDurationHistogramTest {

    @Test
    void defaultBucketsPlacement() {
        IdleDurationHistogram hist = new IdleDurationHistogram();
        hist.record(30_000);    // [0,1m)
        hist.record(60_000);    // [1m,5m)  —— 恰达边界归右桶
        hist.record(299_000);   // [1m,5m)
        hist.record(15 * 60_000); // [15m,60m)
        hist.record(3_600_000); // ≥1h

        long[] counts = hist.bucketCounts();
        assertThat(counts).hasSize(5);
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(2);
        assertThat(counts[2]).isZero();
        assertThat(counts[3]).isEqualTo(1);
        assertThat(counts[4]).isEqualTo(1);
        assertThat(hist.total()).isEqualTo(5);
        assertThat(hist.longestIdleMillis()).isEqualTo(3_600_000);
    }

    @Test
    void customBoundsAndNegativeIgnored() {
        IdleDurationHistogram hist = new IdleDurationHistogram(new long[]{100});
        hist.record(50);
        hist.record(150);
        hist.record(-1);
        long[] counts = hist.bucketCounts();
        assertThat(counts).hasSize(2);
        assertThat(counts[0]).isEqualTo(1);
        assertThat(counts[1]).isEqualTo(1);
        assertThat(hist.total()).isEqualTo(2);
    }

    @Test
    void labelsAreHumanReadable() {
        long[] bounds = IdleDurationHistogram.DEFAULT_BOUNDS;
        assertThat(IdleDurationHistogram.bucketLabel(bounds, 0)).isEqualTo("[0,1m)");
        assertThat(IdleDurationHistogram.bucketLabel(bounds, 1)).isEqualTo("[1m,5m)");
        assertThat(IdleDurationHistogram.bucketLabel(bounds, 4)).isEqualTo("≥1h");
    }

    @Test
    void labeledSnapshotCoversAllBuckets() {
        IdleDurationHistogram hist = new IdleDurationHistogram(new long[]{10, 20});
        hist.record(5);
        var rows = hist.labeledSnapshot();
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).label()).isEqualTo("[0,10)");
        assertThat(rows.get(1).label()).isEqualTo("[10,20)");
        assertThat(rows.get(2).label()).isEqualTo("≥20");
        assertThat(rows.get(0).count()).isEqualTo(1);
    }
}
