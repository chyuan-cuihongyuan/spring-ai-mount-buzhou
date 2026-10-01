package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11009 / Y11019：AutocorrelationPitch 合同验证——正弦基音复原
 * +白噪峰面+确定性+fail-fast。
 */
class AutocorrelationPitchTest {

    private static final double SAMPLE_RATE = 1000.0;

    private static double[] sine(double frequency, int count) {
        double[] samples = new double[count];
        for (int i = 0; i < count; i++) {
            samples[i] = Math.sin(2.0 * Math.PI * frequency * i / SAMPLE_RATE);
        }
        return samples;
    }

    @Test
    void shouldRecoverPitch_whenIntegerCycleSine() {
        // fs=1000、f=100：lag=10——800 样点 ≥2·maxLag+1（maxLag=100 → 201）
        double pitch = AutocorrelationPitch.detectPitch(sine(100, 800),
                SAMPLE_RATE, 50.0, 200.0);
        assertThat(pitch).isCloseTo(100.0, within(2.0));
    }

    @Test
    void shouldRecoverLowPitch_whenSlowSine() {
        // 50 Hz 整周期 lag=20（60Hz 周期 16.67 非整——整数滞后网格分辨率口径注记）
        double pitch = AutocorrelationPitch.detectPitch(sine(50, 800),
                SAMPLE_RATE, 40.0, 200.0);
        assertThat(pitch).isCloseTo(50.0, within(2.0));
    }

    @Test
    void shouldSuppressNoiseDominance_whenWhiteNoise() {
        Random random = new Random(11009L);
        double[] samples = new double[800];
        for (int i = 0; i < 800; i++) {
            samples[i] = random.nextGaussian();
        }
        double pitch = AutocorrelationPitch.detectPitch(samples,
                SAMPLE_RATE, 50.0, 200.0);
        assertThat(pitch).isBetween(50.0, 200.0);
    }

    @Test
    void shouldReproduceIdenticalPitch_whenSameInputTwice() {
        double[] samples = sine(100, 800);
        assertThat(AutocorrelationPitch.detectPitch(samples, SAMPLE_RATE, 50, 200))
                .isEqualTo(AutocorrelationPitch.detectPitch(samples, SAMPLE_RATE, 50, 200));
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        assertThatThrownBy(() -> AutocorrelationPitch.detectPitch(null, SAMPLE_RATE, 50, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AutocorrelationPitch.detectPitch(sine(100, 800),
                SAMPLE_RATE, 200, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("频域");
        assertThatThrownBy(() -> AutocorrelationPitch.detectPitch(sine(100, 40),
                SAMPLE_RATE, 50, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时长不足");
        assertThatThrownBy(() -> AutocorrelationPitch.detectPitch(sine(100, 800),
                0, 50, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("采样率");
    }
}
