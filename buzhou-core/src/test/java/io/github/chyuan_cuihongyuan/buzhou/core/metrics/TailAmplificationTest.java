package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1894 / T2990：尾时延放大——幂次放大、SLO 反解、恒等、畸形。 */
class TailAmplificationTest {

    /** 经典：q=0.99、N=100 → 端到端仅 ~36.6%。 */
    @Test
    void ninetyNineOverHundredBoxesAmplifies() {
        assertThat(TailAmplification.endToEndProbability(0.99, 100))
                .isCloseTo(0.366, within(1e-3));
    }

    /** SLO 反解：端到端 99% / 100 盒 → 单盒需 4 个 9。 */
    @Test
    void sloDecompositionRequiresMoreNines() {
        assertThat(TailAmplification.requiredPerBoxQuantile(0.99, 100))
                .isCloseTo(0.9999, within(1e-5));
    }

    /** N=1 恒等；互逆交叉验证。 */
    @Test
    void identityAndInverseConsistency() {
        assertThat(TailAmplification.endToEndProbability(0.95, 1)).isCloseTo(0.95, within(1e-12));
        double q = TailAmplification.requiredPerBoxQuantile(0.999, 50);
        assertThat(TailAmplification.endToEndProbability(q, 50)).isCloseTo(0.999, within(1e-9));
    }

    /** 畸形入参 fail-fast：分位越界、盒子数 0。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> TailAmplification.endToEndProbability(1.0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boxCount 不能小于 1");
        assertThatThrownBy(() -> TailAmplification.endToEndProbability(0.0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perBoxQuantile 须在 (0,1]");
        assertThatThrownBy(() -> TailAmplification.requiredPerBoxQuantile(1.5, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endToEndTarget 须在 (0,1]");
    }
}
