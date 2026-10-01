package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11004 / Y11009：GoertzelAlgorithm 合同验证——同频/异频响应比
 * +直接 DFT 频仓模平方交叉互证圣像+边界拒绝+确定性+fail-fast。
 */
class GoertzelAlgorithmTest {

    private static final double SAMPLE_RATE = 1000.0;
    private static final int N = 256;

    private static double[] sineAt(double frequency, double amplitude) {
        double[] samples = new double[N];
        for (int i = 0; i < N; i++) {
            samples[i] = amplitude * Math.sin(2.0 * Math.PI * frequency * i / SAMPLE_RATE);
        }
        return samples;
    }

    @Test
    void shouldRespondStrongly_whenFrequencyMatches() {
        // 125 Hz×N=256/fs=1000 = 整 32 周期（非整周期面泄漏——口径注记）
        double power = GoertzelAlgorithm.power(sineAt(125, 2.0), 125, SAMPLE_RATE);
        assertThat(power).isCloseTo(Math.pow(2.0 * N / 2, 2), within(1.0));
    }

    @Test
    void shouldRejectOffFrequency_whenMismatched() {
        double onFrequency = GoertzelAlgorithm.power(sineAt(100, 2.0), 100, SAMPLE_RATE);
        double offFrequency = GoertzelAlgorithm.power(sineAt(100, 2.0), 250, SAMPLE_RATE);
        assertThat(onFrequency / Math.max(offFrequency, 1e-12)).isGreaterThan(10.0);
    }

    @Test
    void shouldMatchDirectDftBin_whenRandomSignal() {
        java.util.Random random = new java.util.Random(11004L);
        double[] samples = new double[N];
        for (int i = 0; i < N; i++) {
            samples[i] = random.nextGaussian();
        }
        int bin = 37;
        double frequency = (double) bin * SAMPLE_RATE / N;
        double goertzel = GoertzelAlgorithm.power(samples, frequency, SAMPLE_RATE);
        double re = 0;
        double im = 0;
        for (int t = 0; t < N; t++) {
            double angle = -2.0 * Math.PI * bin * t / N;
            re += samples[t] * Math.cos(angle);
            im += samples[t] * Math.sin(angle);
        }
        assertThat(goertzel).isCloseTo(re * re + im * im, within(1e-6));
    }

    @Test
    void shouldReproduceIdenticalPower_whenSameInputTwice() {
        double[] samples = sineAt(125, 1.0);
        assertThat(GoertzelAlgorithm.power(samples, 125, SAMPLE_RATE))
                .isEqualTo(GoertzelAlgorithm.power(samples, 125, SAMPLE_RATE));
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        double[] samples = {1, 2, 3};
        assertThatThrownBy(() -> GoertzelAlgorithm.power(null, 100, SAMPLE_RATE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GoertzelAlgorithm.power(samples, 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("采样率");
        assertThatThrownBy(() -> GoertzelAlgorithm.power(samples, 0, SAMPLE_RATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标频域");
        assertThatThrownBy(() -> GoertzelAlgorithm.power(samples, SAMPLE_RATE, SAMPLE_RATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标频域");
    }
}
