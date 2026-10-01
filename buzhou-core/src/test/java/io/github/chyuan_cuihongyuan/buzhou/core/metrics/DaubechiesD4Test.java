package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11006 / Y11013：DaubechiesD4 合同验证——双消失矩手锚+常量细节零
 * +Parseval 守恒+最短输入面+确定性+fail-fast。
 */
class DaubechiesD4Test {

    @Test
    void shouldHaveZeroLevelOneDetails_whenLinearSignal() {
        // 双消失矩：8 点线性信号一层细节不回绕窗全零（周期回绕窗除外——口径注记）
        double[] coefficients = DaubechiesD4.forward(new double[]{0, 1, 2, 3, 4, 5, 6, 7});
        assertThat(coefficients[4]).isCloseTo(0.0, within(1e-9));
        assertThat(coefficients[5]).isCloseTo(0.0, within(1e-9));
        assertThat(coefficients[6]).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void shouldHaveZeroDetails_whenConstantSignal() {
        // n=4 布局 [a0,a1,d0,d1]（4 点单层止——窗长 4 下限）；Σc=√2
        double[] coefficients = DaubechiesD4.forward(new double[]{5, 5, 5, 5});
        assertThat(coefficients[0]).isCloseTo(5.0 * Math.sqrt(2), within(1e-12));
        assertThat(coefficients[1]).isCloseTo(5.0 * Math.sqrt(2), within(1e-12));
        assertThat(coefficients[2]).isCloseTo(0.0, within(1e-12));
        assertThat(coefficients[3]).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void shouldConserveEnergy_whenParseval() {
        Random random = new Random(11006L);
        double[] x = new double[8];
        for (int i = 0; i < 8; i++) {
            x[i] = random.nextGaussian();
        }
        double timeEnergy = 0;
        for (double v : x) {
            timeEnergy += v * v;
        }
        double[] coefficients = DaubechiesD4.forward(x);
        double freqEnergy = 0;
        for (double v : coefficients) {
            freqEnergy += v * v;
        }
        assertThat(freqEnergy).isCloseTo(timeEnergy, within(1e-9));
    }

    @Test
    void shouldHandleShortestInput_whenFourPoints() {
        double[] coefficients = DaubechiesD4.forward(new double[]{1, 0, 0, 0});
        double energy = 0;
        for (double v : coefficients) {
            energy += v * v;
        }
        assertThat(energy).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void shouldReproduceIdenticalCoefficients_whenSameInputTwice() {
        double[] x = {1, 5, 2, 7, 0, 3, 9, 4};
        assertThat(DaubechiesD4.forward(x)).isEqualTo(DaubechiesD4.forward(x));
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> DaubechiesD4.forward(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DaubechiesD4.forward(new double[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("≥4");
        double[] withNaN = {1, 2, 3, Double.NaN};
        assertThatThrownBy(() -> DaubechiesD4.forward(withNaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
