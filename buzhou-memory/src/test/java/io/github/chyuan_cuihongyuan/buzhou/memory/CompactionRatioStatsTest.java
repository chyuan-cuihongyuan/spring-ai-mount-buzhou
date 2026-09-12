package io.github.chyuan_cuihongyuan.buzhou.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 517 / T777：压缩规模分布观测——回收字符 exact 分位、逐出比直方图、
 * 折入 trigger 计数、窗口有界、零样本 null 诚实空值、折入样本独立窗。
 */
class CompactionRatioStatsTest {

    @Test
    void reclaimedDistributionPercentiles() {
        CompactionRatioStats stats = new CompactionRatioStats();
        for (long i = 1; i <= 100; i++) {
            stats.recordCompaction((int) i, 0.9);
        }
        var snap = stats.snapshot();
        assertThat(snap.totalReclaimedChars()).isEqualTo(5050);
        assertThat(snap.p50ReclaimedChars()).isEqualTo(50);
        assertThat(snap.p95ReclaimedChars()).isEqualTo(95);
        assertThat(snap.evictRatioHistogram().get(0.9)).isEqualTo(100);
    }

    @Test
    void foldSamplesInSeparateWindowWithTriggerCounts() {
        CompactionRatioStats stats = new CompactionRatioStats();
        stats.recordFold(800, "budget");
        stats.recordFold(1200, "backlog");
        stats.recordFold(1500, "drift");
        var snap = stats.snapshot();
        assertThat(snap.totalFolds()).isEqualTo(3);
        assertThat(snap.foldTriggerCounts().get("budget")).isEqualTo(1);
        assertThat(snap.foldTriggerCounts().get("drift")).isEqualTo(1);
        // 折入样本独立窗：p50 = 1200（不与回收字符混染）
        assertThat(snap.p50FoldChars()).isEqualTo(1200);
    }

    @Test
    void windowBoundedAndNegativeIgnored() {
        CompactionRatioStats stats = new CompactionRatioStats(16, 8);
        for (int i = 1; i <= 40; i++) {
            stats.recordCompaction(i, 1.0);
        }
        stats.recordCompaction(-3, 1.0); // 负值忽略
        assertThat(stats.snapshot().totalReclaimedChars()).isEqualTo(820); // 25+..+40 = 820
        assertThat(stats.snapshot().p50ReclaimedChars()).isEqualTo(32);
    }

    @Test
    void emptyStatsHaveNullPercentilesAndZeroTotals() {
        var snap = new CompactionRatioStats().snapshot();
        assertThat(snap.totalReclaimedChars()).isZero();
        assertThat(snap.p50ReclaimedChars()).isNull();
        assertThat(snap.p95ReclaimedChars()).isNull();
        assertThat(snap.p50FoldChars()).isNull();
        assertThat(snap.totalFolds()).isZero();
    }

    @Test
    void unknownEvictRatioLevelStillCounted() {
        CompactionRatioStats stats = new CompactionRatioStats();
        stats.recordCompaction(10, 0.7); // 非梯子三级值——直方图新键计数（观测诚实）
        assertThat(stats.snapshot().evictRatioHistogram().get(0.7)).isEqualTo(1);
    }
}
