package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10040 / X10081：LombScargle 合同验证——均匀/不均匀采样正弦峰位复原
 * +白噪无伪峰+确定性+fail-fast。
 */
class LombScargleTest {

    private static final double F0 = 4.0;

    private static double[] sweep() {
        double[] frequencies = new double[201];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = 1.0 + i * 0.05;
        }
        return frequencies;
    }

    private static int argMax(double[] values) {
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[best]) {
                best = i;
            }
        }
        return best;
    }

    private static double frequencyAt(int index) {
        return 1.0 + index * 0.05;
    }

    @Test
    void shouldRecoverFrequency_whenUniformSampling() {
        int n = 256;
        double[] times = new double[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            times[i] = i * 0.25;
            values[i] = Math.sin(2.0 * Math.PI * F0 * times[i]);
        }
        int peak = argMax(LombScargle.periodogram(times, values, 1.0, 11.0, 201));
        assertThat(Math.abs(frequencyAt(peak) - F0)).isLessThan(0.051);
    }

    @Test
    void shouldRecoverFrequency_whenIrregularSampling() {
        Random random = new Random(10040L);
        double[] times = new double[180];
        double[] values = new double[180];
        double t = 0;
        for (int i = 0; i < 180; i++) {
            t += 0.2 + random.nextDouble() * 0.3;
            times[i] = t;
            values[i] = Math.sin(2.0 * Math.PI * F0 * t);
        }
        int peak = argMax(LombScargle.periodogram(times, values, 1.0, 11.0, 201));
        assertThat(Math.abs(frequencyAt(peak) - F0)).isLessThan(0.051);
    }

    @Test
    void shouldSuppressSpuriousPeaks_whenWhiteNoise() {
        Random random = new Random(10041L);
        double[] times = new double[128];
        double[] values = new double[128];
        for (int i = 0; i < 128; i++) {
            times[i] = i * 0.3;
            values[i] = random.nextGaussian();
        }
        double[] power = LombScargle.periodogram(times, values, 1.0, 10.0, 91);
        // 白噪功率指数分布：91 频点 max 期望 ≈ ln91≈4.5（极值口径），<8 为 3% 尾界
        assertThat(power[argMax(power)]).isLessThan(8.0);
    }

    @Test
    void shouldReproduceIdenticalSpectrum_whenSameInputTwice() {
        double[] times = {0, 1, 2, 3, 4, 5};
        double[] values = {0, 1, 0, -1, 0, 1};
        assertThat(LombScargle.periodogram(times, values, 0.5, 3.0, 26))
                .isEqualTo(LombScargle.periodogram(times, values, 0.5, 3.0, 26));
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        double[] times = {0, 1, 2};
        double[] values = {0, 1};
        assertThatThrownBy(() -> LombScargle.periodogram(times, values, 1, 2, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("长度不配");
        assertThatThrownBy(() -> LombScargle.periodogram(times, new double[]{0, 1, 0}, 0, 2, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("下界为正");
        assertThatThrownBy(() -> LombScargle.periodogram(times, new double[]{0, 1, 0}, 3, 2, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越序");
        assertThatThrownBy(() -> LombScargle.periodogram(
                new double[2], new double[]{7, 7}, 1, 2, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("方差为零");
    }
}
