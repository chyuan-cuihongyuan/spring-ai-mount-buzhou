package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.eval.MultipleComparisonCorrection.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2060 / T3222：BH-FDR 合同——截止序判定、比 Holm 松（功效高）、
 * 全大 p 零显著、m=1 退化、畸形 fail-fast。
 */
class FalseDiscoveryRateTest {

    @Test
    void bhShouldPassSmallPsUpToCutoff() {
        // m=5，q=0.05：阈值 [0.01, 0.02, 0.03, 0.04, 0.05]
        boolean[] sig = FalseDiscoveryRate.benjaminiHochberg(
                new double[]{0.005, 0.019, 0.031, 0.2, 0.9}, 0.05);
        // p(1)=0.005≤0.01 ✓、p(2)=0.019≤0.02 ✓、p(3)=0.031>0.03 止——最大 j=2
        assertThat(sig).containsExactly(true, true, false, false, false);
    }

    @Test
    void lateSmallPShouldNotRescue() {
        // 截止后的孤立小 p 不能独立显著（BH 截止序语义——非逐个判定）
        boolean[] sig = FalseDiscoveryRate.benjaminiHochberg(
                new double[]{0.5, 0.5, 0.5, 0.01}, 0.05);
        // m=4 阈值 [0.0125..0.05]：p(1)=0.01≤0.0125 ✓ j=1 截止——仅最小者显著
        assertThat(sig).containsExactly(false, false, false, true);
    }

    @Test
    void bhShouldBeMorePowerfulThanHolm() {
        // 经典场景：多个中等小 p——BH 显著 ⊇ Holm 显著
        double[] ps = {0.01, 0.02, 0.03, 0.04};
        boolean[] bh = FalseDiscoveryRate.benjaminiHochberg(ps, 0.05);
        Verdict holm = MultipleComparisonCorrection.holm(ps, 0.05);
        int bhCount = count(bh);
        assertThat(bhCount).isGreaterThanOrEqualTo(holm.significantCount());
        // m=4 BH 阈值 [0.0125,0.025,0.0375,0.05]：四全过——j=4；Holm 阈值
        // [0.0125,0.0167,0.025,0.05]：0.01✓ 0.02>0.0167 即止——仅 1
        assertThat(bhCount).isEqualTo(4);
        assertThat(holm.significantCount()).isEqualTo(1);
    }

    @Test
    void allLargePShouldBeZeroSignificant() {
        assertThat(count(FalseDiscoveryRate.benjaminiHochberg(
                new double[]{0.3, 0.5, 0.7}, 0.05))).isZero();
    }

    @Test
    void singleTestShouldDegradeToRawAlpha() {
        assertThat(FalseDiscoveryRate.benjaminiHochberg(new double[]{0.04}, 0.05)[0]).isTrue();
        assertThat(FalseDiscoveryRate.benjaminiHochberg(new double[]{0.06}, 0.05)[0]).isFalse();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> FalseDiscoveryRate.benjaminiHochberg(null, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FalseDiscoveryRate.benjaminiHochberg(new double[0], 0.05))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FalseDiscoveryRate.benjaminiHochberg(new double[]{0.5}, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FalseDiscoveryRate.benjaminiHochberg(new double[]{0.5}, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FalseDiscoveryRate.benjaminiHochberg(new double[]{-0.1}, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FalseDiscoveryRate.benjaminiHochberg(new double[]{1.1}, 0.05))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static int count(boolean[] arr) {
        int c = 0;
        for (boolean b : arr) {
            if (b) {
                c++;
            }
        }
        return c;
    }
}
