package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 10037 / X10075：DctType2 合同验证——常数信号直流手锚+基正交归一
 * +Parseval 能量守恒+斜坡低频集中+确定性+fail-fast。
 */
class DctType2Test {

    @Test
    void shouldHaveDcOnly_whenConstantSignal() {
        double[] coefficients = DctType2.transform(new double[]{2, 2, 2, 2, 2, 2, 2, 2});
        assertThat(coefficients[0]).isCloseTo(2.0 * Math.sqrt(8), within(1e-12));
        for (int k = 1; k < 8; k++) {
            assertThat(coefficients[k]).isCloseTo(0.0, within(1e-12));
        }
    }

    @Test
    void shouldGiveOrthonormalBasis_whenUnitVectors() {
        int n = 8;
        double[][] basis = new double[n][];
        for (int i = 0; i < n; i++) {
            double[] e = new double[n];
            e[i] = 1.0;
            basis[i] = DctType2.transform(e);
        }
        for (int i = 0; i < n; i++) {
            double norm = 0;
            for (double v : basis[i]) {
                norm += v * v;
            }
            assertThat(norm).as("基 %d 范数平方", i).isCloseTo(1.0, within(1e-12));
            for (int j = i + 1; j < n; j++) {
                double dot = 0;
                for (int t = 0; t < n; t++) {
                    dot += basis[i][t] * basis[j][t];
                }
                assertThat(dot).as("基 %d·%d", i, j).isCloseTo(0.0, within(1e-12));
            }
        }
    }

    @Test
    void shouldConserveEnergy_whenParseval() {
        double[] x = {1, 2, 3, 4, 5, 6, 7, 8};
        double timeEnergy = 0;
        for (double v : x) {
            timeEnergy += v * v;
        }
        double[] coefficients = DctType2.transform(x);
        double freqEnergy = 0;
        for (double v : coefficients) {
            freqEnergy += v * v;
        }
        assertThat(freqEnergy).isCloseTo(timeEnergy, within(1e-12));
    }

    @Test
    void shouldConcentrateEnergyAtLowFrequencies_whenRamp() {
        double[] ramp = new double[8];
        for (int i = 0; i < 8; i++) {
            ramp[i] = i;
        }
        double[] coefficients = DctType2.transform(ramp);
        double lowHalf = 0;
        double highHalf = 0;
        for (int k = 0; k < 8; k++) {
            if (k < 4) {
                lowHalf += coefficients[k] * coefficients[k];
            } else {
                highHalf += coefficients[k] * coefficients[k];
            }
        }
        assertThat(lowHalf).isGreaterThan(highHalf * 10);
    }

    @Test
    void shouldReproduceIdenticalCoefficients_whenSameInputTwice() {
        double[] x = {0.5, 1.5, 2.5, 3.5};
        double[] first = DctType2.transform(x);
        double[] second = DctType2.transform(x);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldFailFast_whenNullOrEmptyOrNonFinite() {
        assertThatThrownBy(() -> DctType2.transform(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DctType2.transform(new double[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非空");
        assertThatThrownBy(() -> DctType2.transform(new double[]{1, Double.POSITIVE_INFINITY}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
