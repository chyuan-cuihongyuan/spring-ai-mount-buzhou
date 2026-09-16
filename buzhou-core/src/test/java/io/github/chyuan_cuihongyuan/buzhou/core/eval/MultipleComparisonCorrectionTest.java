package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.eval.MultipleComparisonCorrection.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2058 / T3218：多重校正合同——Bonferroni 保守、Holm 逐步停步、
 * Holm ⊇ Bonferroni（不弱于）、全不显著、单测退化、畸形 fail-fast。
 */
class MultipleComparisonCorrectionTest {

    @Test
    void bonferroniShouldScaleByCount() {
        // m=5，α=0.05：p×5≤0.05 → p≤0.01
        Verdict v = MultipleComparisonCorrection.bonferroni(
                new double[]{0.005, 0.01, 0.02, 0.04, 0.5}, 0.05);
        assertThat(v.significant()).containsExactly(true, true, false, false, false);
        assertThat(v.significantCount()).isEqualTo(2);
        assertThat(v.method()).isEqualTo("bonferroni");
    }

    @Test
    void holmShouldBeSteppedAndMonotone() {
        // m=4，α=0.05：阈值序列 0.0125 / 0.0167 / 0.025 / 0.05
        Verdict v = MultipleComparisonCorrection.holm(
                new double[]{0.008, 0.015, 0.03, 0.2}, 0.05);
        // 排序 [0.008, 0.015, 0.03, 0.2]：0.008≤0.0125 ✓、0.015≤0.0167 ✓、0.03>0.025 停
        assertThat(v.significant()).containsExactly(true, true, false, false);
        assertThat(v.significantCount()).isEqualTo(2);
    }

    @Test
    void holmShouldStopAllAfterFirstFailure() {
        // 小 p 在后位（乱序入参）：停步后其后的更大 p 全不显著
        Verdict v = MultipleComparisonCorrection.holm(
                new double[]{0.9, 0.03, 0.01}, 0.05);
        // 排序 [0.01, 0.03, 0.9]：0.01≤0.0167 ✓、0.03≤0.025 ✗ 停——0.9 不显著
        assertThat(v.significant()).containsExactly(false, false, true); // 索引序：0.9, 0.03, 0.01
    }

    @Test
    void holmShouldNeverBeWeakerThanBonferroni() {
        double[] ps = {0.001, 0.01, 0.02, 0.03, 0.04, 0.049};
        Verdict bonf = MultipleComparisonCorrection.bonferroni(ps, 0.05);
        Verdict holm = MultipleComparisonCorrection.holm(ps, 0.05);
        for (int i = 0; i < ps.length; i++) {
            assertThat(holm.significant()[i] || !bonf.significant()[i])
                    .as("holm 显著 ⊇ bonferroni 显显（索引 %d）", i).isTrue();
        }
        assertThat(holm.significantCount()).isGreaterThanOrEqualTo(bonf.significantCount());
    }

    @Test
    void allLargePShouldBeAllInsignificant() {
        double[] ps = {0.3, 0.4, 0.6};
        assertThat(MultipleComparisonCorrection.bonferroni(ps, 0.05).significantCount()).isZero();
        assertThat(MultipleComparisonCorrection.holm(ps, 0.05).significantCount()).isZero();
    }

    @Test
    void singleTestShouldMatchRawAlpha() {
        // m=1：两法都退化为 p ≤ α 原始口径
        assertThat(MultipleComparisonCorrection.bonferroni(new double[]{0.04}, 0.05)
                .significant()[0]).isTrue();
        assertThat(MultipleComparisonCorrection.holm(new double[]{0.04}, 0.05)
                .significant()[0]).isTrue();
        assertThat(MultipleComparisonCorrection.bonferroni(new double[]{0.06}, 0.05)
                .significant()[0]).isFalse();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> MultipleComparisonCorrection.bonferroni(null, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MultipleComparisonCorrection.bonferroni(new double[0], 0.05))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MultipleComparisonCorrection.bonferroni(new double[]{0.5}, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MultipleComparisonCorrection.bonferroni(new double[]{1.5}, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MultipleComparisonCorrection.holm(new double[]{-0.1}, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MultipleComparisonCorrection.holm(new double[]{Double.NaN}, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
