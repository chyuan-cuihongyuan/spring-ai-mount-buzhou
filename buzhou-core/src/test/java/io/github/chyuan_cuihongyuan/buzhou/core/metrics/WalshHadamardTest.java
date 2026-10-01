package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11002 / Y11005：WalshHadamard 合同验证——脉冲/常量手锚+对合性质
 * +Parseval+确定性+fail-fast。
 */
class WalshHadamardTest {

    @Test
    void shouldGiveAllOnes_whenUnitImpulse() {
        double[] x = {1, 0, 0, 0};
        WalshHadamard.transform(x);
        assertThat(x).containsExactly(1.0, 1.0, 1.0, 1.0);
    }

    @Test
    void shouldHaveDcOnly_whenConstantSignal() {
        double[] x = {1, 1, 1, 1};
        WalshHadamard.transform(x);
        assertThat(x[0]).isCloseTo(4.0, within(1e-12));
        assertThat(x[1]).isCloseTo(0.0, within(1e-12));
        assertThat(x[2]).isCloseTo(0.0, within(1e-12));
        assertThat(x[3]).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void shouldBeInvolutive_whenAppliedTwice() {
        Random random = new Random(11002L);
        for (int trial = 0; trial < 20; trial++) {
            double[] x = new double[16];
            for (int i = 0; i < 16; i++) {
                x[i] = random.nextGaussian();
            }
            double[] original = x.clone();
            WalshHadamard.transform(x);
            WalshHadamard.transform(x);
            for (int i = 0; i < 16; i++) {
                assertThat(x[i]).as("试验 %d 位 %d", trial, i)
                        .isCloseTo(original[i] * 16, within(1e-9));
            }
        }
    }

    @Test
    void shouldConserveEnergyScaled_whenParseval() {
        double[] x = {1, 2, 3, 4, 5, 6, 7, 8};
        double timeEnergy = 0;
        for (double v : x) {
            timeEnergy += v * v;
        }
        WalshHadamard.transform(x);
        double freqEnergy = 0;
        for (double v : x) {
            freqEnergy += v * v;
        }
        assertThat(freqEnergy).isCloseTo(timeEnergy * 8, within(1e-9));
    }

    @Test
    void shouldReproduceIdenticalSpectrum_whenSameInputTwice() {
        double[] a = {1, 2, 3, 4};
        double[] b = {1, 2, 3, 4};
        WalshHadamard.transform(a);
        WalshHadamard.transform(b);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> WalshHadamard.transform(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WalshHadamard.transform(new double[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 的幂");
        double[] withNaN = {1, Double.NaN};
        assertThatThrownBy(() -> WalshHadamard.transform(withNaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
