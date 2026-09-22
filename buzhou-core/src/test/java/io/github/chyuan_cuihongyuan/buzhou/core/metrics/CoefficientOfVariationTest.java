package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1902 / T3006：波动系数——CV、分档、畸形。 */
class CoefficientOfVariationTest {

    /** 三档：0.1 STABLE / 0.3 MODERATE / 0.5 VOLATILE（边界严格小于）。 */
    @Test
    void threeBands() {
        assertThat(CoefficientOfVariation.band(
                CoefficientOfVariation.cv(100, 10))).isEqualTo(CoefficientOfVariation.Volatility.STABLE);
        assertThat(CoefficientOfVariation.band(
                CoefficientOfVariation.cv(50, 15))).isEqualTo(CoefficientOfVariation.Volatility.MODERATE);
        assertThat(CoefficientOfVariation.band(
                CoefficientOfVariation.cv(50, 25))).isEqualTo(CoefficientOfVariation.Volatility.VOLATILE);
    }

    /** 边界严格小于：恰 0.15 不含 STABLE、恰 0.5 不含 MODERATE。 */
    @Test
    void boundariesStrict() {
        assertThat(CoefficientOfVariation.band(0.15)).isEqualTo(CoefficientOfVariation.Volatility.MODERATE);
        assertThat(CoefficientOfVariation.band(0.5)).isEqualTo(CoefficientOfVariation.Volatility.VOLATILE);
    }

    /** 负均值取绝对值；同 stddev 不同均值两重天。 */
    @Test
    void negativeMeanUsesAbs() {
        assertThat(CoefficientOfVariation.cv(-100, 10)).isCloseTo(0.1, within(1e-12));
        assertThat(CoefficientOfVariation.cv(50, 10)).isCloseTo(0.2, within(1e-12));
        assertThat(CoefficientOfVariation.cv(5000, 10)).isCloseTo(0.002, within(1e-12));
    }

    /** 畸形入参 fail-fast：mean=0（CV 未定义）、负 stddev。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> CoefficientOfVariation.cv(0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mean 不能为 0");
        assertThatThrownBy(() -> CoefficientOfVariation.cv(100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stddev 不能为负");
    }
}
