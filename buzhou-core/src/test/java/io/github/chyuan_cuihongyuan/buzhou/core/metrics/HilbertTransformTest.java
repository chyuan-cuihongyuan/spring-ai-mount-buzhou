package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 10038 / X10077：HilbertTransform 合同验证——整数周期正弦恒幅包络
 * +双正弦复合+直流信号+确定性+fail-fast。
 */
class HilbertTransformTest {

    @Test
    void shouldKeepConstantAmplitude_whenIntegerCycleSine() {
        int n = 1024;
        double amplitude = 3.0;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = amplitude * Math.sin(2.0 * Math.PI * 16 * i / n);
        }
        double[] envelope = HilbertTransform.envelope(signal);
        for (int i = 0; i < n; i++) {
            assertThat(envelope[i]).as("样点 %d", i)
                    .isCloseTo(amplitude, within(0.03));
        }
    }

    @Test
    void shouldComposeAmplitudes_whenTwoSines() {
        int n = 1024;
        double a1 = 2.0;
        double a2 = 1.0;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            double t = 2.0 * Math.PI * i / n;
            signal[i] = a1 * Math.sin(8 * t) + a2 * Math.cos(24 * t);
        }
        double[] envelope = HilbertTransform.envelope(signal);
        // 窄带复合包络在 [a1−a2, a1+a2] 内波动（拍频）——上界不超过 a1+a2+容差
        for (int i = 0; i < n; i++) {
            assertThat(envelope[i]).as("样点 %d", i).isBetween(a1 - a2 - 0.05, a1 + a2 + 0.05);
        }
    }

    @Test
    void shouldBeAbsoluteValue_whenDcSignal() {
        double[] signal = new double[64];
        java.util.Arrays.fill(signal, 2.5);
        double[] envelope = HilbertTransform.envelope(signal);
        for (int i = 0; i < 64; i++) {
            assertThat(envelope[i]).isCloseTo(2.5, within(1e-9));
        }
    }

    @Test
    void shouldReproduceIdenticalEnvelope_whenSameInputTwice() {
        double[] signal = new double[256];
        for (int i = 0; i < 256; i++) {
            signal[i] = Math.sin(i);
        }
        assertThat(HilbertTransform.envelope(signal))
                .isEqualTo(HilbertTransform.envelope(signal));
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> HilbertTransform.envelope(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HilbertTransform.envelope(new double[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 的幂");
        double[] withNaN = new double[8];
        withNaN[3] = Double.NaN;
        assertThatThrownBy(() -> HilbertTransform.envelope(withNaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
