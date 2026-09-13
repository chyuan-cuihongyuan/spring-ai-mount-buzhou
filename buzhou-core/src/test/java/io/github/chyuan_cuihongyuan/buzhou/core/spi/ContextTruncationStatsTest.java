package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 834 / T1170：截断统计回归——chars 降序/事件与字符累计/溢出桶/脏入参/空真。
 */
class ContextTruncationStatsTest {

    @Test
    void sortedByCharsWithTotals() {
        ContextTruncationStats stats = new ContextTruncationStats();
        stats.record("memory-compaction", 500);
        stats.record("spill-offload", 3000);
        stats.record("memory-compaction", 700);
        stats.record("hard-trim", 100);

        var report = stats.snapshot();
        assertThat(report.totalEvents()).isEqualTo(4);
        assertThat(report.totalCharsDropped()).isEqualTo(4300);
        assertThat(report.strategies().get(0).strategy()).isEqualTo("spill-offload");
        assertThat(report.strategies().get(0).charsDropped()).isEqualTo(3000);
        assertThat(report.strategies().get(1).strategy()).isEqualTo("memory-compaction");
        assertThat(report.strategies().get(1).events()).isEqualTo(2);
        assertThat(report.strategies().get(1).charsDropped()).isEqualTo(1200);
        assertThat(report.strategies().get(2).charsDropped()).isEqualTo(100);
    }

    @Test
    void overflowBucketAfterCap() {
        ContextTruncationStats stats = new ContextTruncationStats();
        for (int i = 0; i < ContextTruncationStats.MAX_STRATEGIES; i++) {
            stats.record("strategy" + i, 100);
        }
        stats.record("new-strategy", 5000); // 超封顶并入溢出桶

        var report = stats.snapshot();
        assertThat(report.strategies()).hasSize(ContextTruncationStats.MAX_STRATEGIES + 1);
        var overflow = report.strategies().stream()
                .filter(s -> s.strategy().equals(ContextTruncationStats.OVERFLOW))
                .findFirst().orElseThrow();
        assertThat(overflow.charsDropped()).isEqualTo(5000);
        assertThat(report.totalCharsDropped()).isEqualTo(ContextTruncationStats.MAX_STRATEGIES * 100L + 5000);
    }

    @Test
    void dirtyInputsAndEmptyTruth() {
        ContextTruncationStats stats = new ContextTruncationStats();
        stats.record(null, 100);
        stats.record("  ", 100);
        stats.record("s", -50);
        var report = stats.snapshot();
        assertThat(report.totalEvents()).isZero();
        assertThat(report.totalCharsDropped()).isZero();
        assertThat(report.strategies()).isEmpty();
    }
}
