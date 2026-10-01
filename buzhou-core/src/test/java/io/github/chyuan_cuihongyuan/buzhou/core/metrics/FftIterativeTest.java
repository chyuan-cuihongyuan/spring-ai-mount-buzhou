package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 10036 / X10073：FftIterative 合同验证——脉冲/直流手锚+随机实信号
 * 与 O(n²) 直接 DFT 交叉互证+Parseval 能量守恒+线性性+确定性+fail-fast。
 */
class FftIterativeTest {

    /** 直接 O(n²) DFT 神像（独立实现口径）。 */
    private static void directDft(double[] re, double[] im) {
        int n = re.length;
        double[] outRe = new double[n];
        double[] outIm = new double[n];
        for (int k = 0; k < n; k++) {
            for (int t = 0; t < n; t++) {
                double angle = -2.0 * Math.PI * k * t / n;
                outRe[k] += re[t] * Math.cos(angle) - im[t] * Math.sin(angle);
                outIm[k] += re[t] * Math.sin(angle) + im[t] * Math.cos(angle);
            }
        }
        System.arraycopy(outRe, 0, re, 0, n);
        System.arraycopy(outIm, 0, im, 0, n);
    }

    @Test
    void shouldGiveFlatSpectrum_whenUnitImpulse() {
        double[] re = {1, 0, 0, 0, 0, 0, 0, 0};
        double[] im = new double[8];
        FftIterative.transform(re, im);
        for (int k = 0; k < 8; k++) {
            assertThat(Math.hypot(re[k], im[k])).as("频点 %d", k)
                    .isCloseTo(1.0, within(1e-12));
        }
    }

    @Test
    void shouldHaveDcOnly_whenConstantSignal() {
        double[] re = {1, 1, 1, 1};
        double[] im = new double[4];
        FftIterative.transform(re, im);
        assertThat(Math.hypot(re[0], im[0])).isCloseTo(4.0, within(1e-12));
        for (int k = 1; k < 4; k++) {
            assertThat(Math.hypot(re[k], im[k])).isCloseTo(0.0, within(1e-12));
        }
    }

    @Test
    void shouldMatchDirectDft_whenRandomRealSignal() {
        Random random = new Random(10036L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 32;
            double[] re = new double[n];
            for (int i = 0; i < n; i++) {
                re[i] = random.nextGaussian();
            }
            double[] im = new double[n];
            double[] refRe = re.clone();
            double[] refIm = im.clone();
            FftIterative.transform(re, im);
            directDft(refRe, refIm);
            for (int k = 0; k < n; k++) {
                assertThat(re[k]).as("试验 %d 频点 %d 实部", trial, k)
                        .isCloseTo(refRe[k], within(1e-9));
                assertThat(im[k]).as("试验 %d 频点 %d 虚部", trial, k)
                        .isCloseTo(refIm[k], within(1e-9));
            }
        }
    }

    @Test
    void shouldConserveEnergy_whenParseval() {
        double[] re = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] im = new double[8];
        double timeEnergy = 0;
        for (double v : re) {
            timeEnergy += v * v;
        }
        FftIterative.transform(re, im);
        double freqEnergy = 0;
        for (int k = 0; k < 8; k++) {
            freqEnergy += re[k] * re[k] + im[k] * im[k];
        }
        assertThat(freqEnergy).isCloseTo(timeEnergy * 8, within(1e-9));
    }

    @Test
    void shouldBeLinear_whenSumOfSignals() {
        double[] a = {1, 2, 3, 4};
        double[] b = {5, 6, 7, 8};
        double[] sum = {6, 8, 10, 12};
        double[] faIm = new double[4];
        double[] fbIm = new double[4];
        double[] fsIm = new double[4];
        FftIterative.transform(a, faIm);
        FftIterative.transform(b, fbIm);
        FftIterative.transform(sum, fsIm);
        for (int k = 0; k < 4; k++) {
            assertThat(a[k] + b[k]).isCloseTo(sum[k], within(1e-12));
            assertThat(faIm[k] + fbIm[k]).isCloseTo(fsIm[k], within(1e-12));
        }
    }

    @Test
    void shouldReproduceIdenticalSpectrum_whenSameInputTwice() {
        double[] re1 = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] im1 = new double[8];
        double[] re2 = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] im2 = new double[8];
        FftIterative.transform(re1, im1);
        FftIterative.transform(re2, im2);
        assertThat(re1).isEqualTo(re2);
        assertThat(im1).isEqualTo(im2);
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        assertThatThrownBy(() -> FftIterative.transform(null, new double[4]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FftIterative.transform(new double[4], new double[8]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("长度不配");
        assertThatThrownBy(() -> FftIterative.magnitudes(new double[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 的幂");
        assertThatThrownBy(() -> FftIterative.magnitudes(new double[]{1, Double.NaN}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
