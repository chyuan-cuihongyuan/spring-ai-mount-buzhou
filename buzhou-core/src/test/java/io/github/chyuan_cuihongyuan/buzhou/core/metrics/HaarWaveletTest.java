package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11003 / Y11007：HaarWavelet 合同验证——手锚+常量细节零+Parseval
 * 守恒+确定性+fail-fast。
 */
class HaarWaveletTest {

    @Test
    void shouldMatchHandAnchor_whenFourPointSignal() {
        double[] coefficients = HaarWavelet.forward(new double[]{1, 2, 3, 4});
        assertThat(coefficients[0]).isCloseTo(5.0, within(1e-12));
        assertThat(coefficients[1]).isCloseTo(-2.0, within(1e-12));
        assertThat(coefficients[2]).isCloseTo(-1.0 / Math.sqrt(2), within(1e-12));
        assertThat(coefficients[3]).isCloseTo(-1.0 / Math.sqrt(2), within(1e-12));
    }

    @Test
    void shouldHaveZeroDetails_whenConstantSignal() {
        double[] coefficients = HaarWavelet.forward(new double[]{3, 3, 3, 3, 3, 3, 3, 3});
        // 常量信号细节全零（差值为 0），近似链 3√2^k 归一后 3·√8
        assertThat(coefficients[0]).isCloseTo(3.0 * Math.sqrt(8), within(1e-12));
        for (int i = 1; i < 8; i++) {
            assertThat(coefficients[i]).isCloseTo(0.0, within(1e-12));
        }
    }

    @Test
    void shouldConserveEnergy_whenParseval() {
        Random random = new Random(11003L);
        double[] x = new double[8];
        for (int i = 0; i < 8; i++) {
            x[i] = random.nextGaussian();
        }
        double timeEnergy = 0;
        for (double v : x) {
            timeEnergy += v * v;
        }
        double[] coefficients = HaarWavelet.forward(x);
        double freqEnergy = 0;
        for (double v : coefficients) {
            freqEnergy += v * v;
        }
        assertThat(freqEnergy).isCloseTo(timeEnergy, within(1e-9));
    }

    @Test
    void shouldReproduceIdenticalCoefficients_whenSameInputTwice() {
        double[] x = {1, 5, 2, 7, 0, 3, 9, 4};
        assertThat(HaarWavelet.forward(x)).isEqualTo(HaarWavelet.forward(x));
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> HaarWavelet.forward(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HaarWavelet.forward(new double[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非空");
        assertThatThrownBy(() -> HaarWavelet.forward(new double[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 的幂");
        double[] withNaN = {1, Double.NaN};
        assertThatThrownBy(() -> HaarWavelet.forward(withNaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
