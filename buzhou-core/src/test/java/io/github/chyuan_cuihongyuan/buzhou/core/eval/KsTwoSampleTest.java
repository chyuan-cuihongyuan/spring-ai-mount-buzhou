package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.eval.KsTwoSample.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4004 / T6010：KS 两样本合同——同分布不拒、完全分离全拒、
 * 半错位居中、畸形 fail-fast。
 */
class KsTwoSampleTest {

    @Test
    void identicalSamplesShouldNotReject() {
        Result same = KsTwoSample.test(
                new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
                new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertThat(same.statistic()).isZero();   // ECDF 处处重合
        assertThat(same.pValue()).isEqualTo(1.0);
    }

    @Test
    void disjointSamplesShouldFullyReject() {
        Result disjoint = KsTwoSample.test(
                new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
                new double[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20});
        assertThat(disjoint.statistic()).isEqualTo(1.0);   // ECDF 全程互不重叠
        assertThat(disjoint.pValue()).isLessThan(0.001);
    }

    @Test
    void halfShiftShouldLandInMiddle() {
        Result shifted = KsTwoSample.test(
                new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
                new double[] {6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
        assertThat(shifted.statistic()).isEqualTo(0.5);   // x=5 处 F₁=0.5 F₂=0
        assertThat(shifted.pValue()).isBetween(0.02, 0.2);   // 似与不似之间
    }

    @Test
    void interleavedHalvesShouldLookAlike() {
        double[] evens = new double[50];
        double[] odds = new double[50];
        for (int k = 0; k < 50; k++) {
            evens[k] = (k + 1) * 2.0;
            odds[k] = (k + 1) * 2.0 - 1.0;
        }
        Result alike = KsTwoSample.test(evens, odds);
        assertThat(alike.statistic()).isLessThan(0.1);
        assertThat(alike.pValue()).isGreaterThan(0.5);
    }

    @Test
    void invalidSamplesShouldFailFast() {
        assertThatThrownBy(() -> KsTwoSample.test(null, new double[] {1.0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KsTwoSample.test(new double[] {1.0}, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KsTwoSample.test(new double[] {}, new double[] {1.0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KsTwoSample.test(new double[] {1.0}, new double[] {}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
