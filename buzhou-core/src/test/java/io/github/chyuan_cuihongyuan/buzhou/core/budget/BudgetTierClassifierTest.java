package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1419 / T2140：预算分档分类器——三档阈值边界精确断言（0.8/1.0 边界
 * 归属）、畸形对 UNKNOWN、饱和度降序+tightest、四桶计数。
 */
class BudgetTierClassifierTest {

    @Test
    void tierBoundariesAreExact() {
        var report = BudgetTierClassifier.classify(Map.of(
                "just-under-warn", new long[]{79, 100},
                "warn-edge", new long[]{80, 100},
                "just-under-hard", new long[]{99, 100},
                "hard-edge", new long[]{100, 100}));
        var byName = report.verdicts().stream().collect(java.util.stream.Collectors
                .toMap(BudgetTierClassifier.BudgetTierVerdict::budget, v -> v));
        // 边界归属：≥0.8 即 WARN、≥1.0 即 HARD（含端点）
        assertThat(byName.get("just-under-warn").tier())
                .isEqualTo(BudgetTierClassifier.Tier.GREEN);
        assertThat(byName.get("warn-edge").tier())
                .isEqualTo(BudgetTierClassifier.Tier.WARN);
        assertThat(byName.get("just-under-hard").tier())
                .isEqualTo(BudgetTierClassifier.Tier.WARN);
        assertThat(byName.get("hard-edge").tier())
                .isEqualTo(BudgetTierClassifier.Tier.HARD);
    }

    @Test
    void tiersCountedInFourBuckets() {
        var report = BudgetTierClassifier.classify(Map.of(
                "g1", new long[]{10, 100},
                "g2", new long[]{50, 100},
                "w1", new long[]{85, 100},
                "h1", new long[]{120, 100},
                "x1", new long[]{5, 0}));
        assertThat(report.greenCount()).isEqualTo(2);
        assertThat(report.warnCount()).isEqualTo(1);
        assertThat(report.hardCount()).isEqualTo(1);
        assertThat(report.unknownCount()).isEqualTo(1);
    }

    @Test
    void tightestIsSortedByRatioDescending() {
        var report = BudgetTierClassifier.classify(Map.of(
                "a", new long[]{90, 100},
                "b", new long[]{50, 100},
                "c", new long[]{99, 100},
                "d", new long[]{10, 100}));
        var top2 = report.tightest(2);
        assertThat(top2).extracting(BudgetTierClassifier.BudgetTierVerdict::budget)
                .containsExactly("c", "a");
    }

    @Test
    void malformedPairsAreUnknownWithoutRatio() {
        var report = BudgetTierClassifier.classify(Map.of(
                "nolimit", new long[]{42, 0},
                "empty", new long[]{}));
        assertThat(report.unknownCount()).isEqualTo(2);
        assertThat(report.verdicts()).allSatisfy(v -> {
            if (v.tier() == BudgetTierClassifier.Tier.UNKNOWN) {
                assertThat(v.ratio()).isEqualTo(-1d);
            }
        });
    }

    @Test
    void overBudgetIsHardNotUnknown() {
        var report = BudgetTierClassifier.classify(Map.of("x", new long[]{250, 100}));
        assertThat(report.verdicts().get(0).tier()).isEqualTo(BudgetTierClassifier.Tier.HARD);
        assertThat(report.verdicts().get(0).ratio()).isEqualTo(2.5d);
    }
}
