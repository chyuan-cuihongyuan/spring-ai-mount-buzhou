package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10039 / X10079：PeakDetector 合同验证——双峰 prominence 手锚+阈值
 * 过滤+单调/平台无峰+噪声脉冲检出+确定性+fail-fast。
 */
class PeakDetectorTest {

    @Test
    void shouldFindBothPeaks_whenProminenceAboveThreshold() {
        // {0,5,1,4,1,0}：峰 1 prominence=5−max(0,0)=5；峰 3 prominence=4−max(1,0)=3
        double[] signal = {0, 5, 1, 4, 1, 0};
        assertThat(PeakDetector.findPeaks(signal, 2.0)).containsExactly(1, 3);
    }

    @Test
    void shouldFilterLowProminencePeak_whenThresholdRaises() {
        double[] signal = {0, 5, 1, 4, 1, 0};
        assertThat(PeakDetector.findPeaks(signal, 3.5)).containsExactly(1);
        assertThat(PeakDetector.findPeaks(signal, 5.5)).isEmpty();
    }

    @Test
    void shouldFindNoPeaks_whenMonotone() {
        assertThat(PeakDetector.findPeaks(new double[]{1, 2, 3, 4, 5}, 0.0)).isEmpty();
    }

    @Test
    void shouldFindNoPeaks_whenPlateau() {
        // 严格峰口径：平台两邻非严格小——无峰
        double[] signal = {0, 1, 1, 1, 0};
        assertThat(PeakDetector.findPeaks(signal, 0.0)).isEmpty();
    }

    @Test
    void shouldDetectImpulse_whenBuriedInSmallNoise() {
        Random random = new Random(10039L);
        double[] signal = new double[128];
        for (int i = 0; i < 128; i++) {
            signal[i] = random.nextGaussian() * 0.1;
        }
        signal[64] = 5.0;
        int[] peaks = PeakDetector.findPeaks(signal, 3.0);
        assertThat(peaks).containsExactly(64);
    }

    @Test
    void shouldReproduceIdenticalPeaks_whenSameInputTwice() {
        double[] signal = {0, 5, 1, 4, 1, 0, 7, 0};
        assertThat(PeakDetector.findPeaks(signal, 1.0))
                .isEqualTo(PeakDetector.findPeaks(signal, 1.0));
    }

    @Test
    void shouldFailFast_whenNullOrNegativeThreshold() {
        assertThatThrownBy(() -> PeakDetector.findPeaks(null, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PeakDetector.findPeaks(new double[]{1, 2, 1}, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("阈值非负");
        double[] withNaN = {1, Double.NaN, 1};
        assertThatThrownBy(() -> PeakDetector.findPeaks(withNaN, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
