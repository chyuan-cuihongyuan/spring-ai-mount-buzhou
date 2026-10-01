package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11008 / Y11017：ZeroCrossingRate 合同验证——交变/常量手锚+正弦
 * 理论值+零值沿用面+确定性+fail-fast。
 */
class ZeroCrossingRateTest {

    @Test
    void shouldBeOne_whenStrictlyAlternating() {
        double[] samples = {1, -1, 1, -1, 1};
        assertThat(ZeroCrossingRate.rate(samples)).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void shouldBeZero_whenConstant() {
        assertThat(ZeroCrossingRate.rate(new double[]{2, 2, 2})).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void shouldMatchTheory_whenSine() {
        // fs=1000、f=50、N=1000：整 50 周期，每周期 2 次过零 → 100/999
        double[] samples = new double[1000];
        for (int i = 0; i < 1000; i++) {
            samples[i] = Math.sin(2.0 * Math.PI * 50 * i / 1000);
        }
        assertThat(ZeroCrossingRate.rate(samples)).isCloseTo(100.0 / 999, within(0.01));
    }

    @Test
    void shouldNotCountZeroAsCrossing_whenZeroInserted() {
        // 零值沿用前符号：{1, 0, -1} 只计 1 次翻转 / 2
        assertThat(ZeroCrossingRate.rate(new double[]{1, 0, -1}))
                .isCloseTo(0.5, within(1e-12));
    }

    @Test
    void shouldReproduceIdenticalRate_whenSameInputTwice() {
        double[] samples = {1, -2, 3, -4};
        assertThat(ZeroCrossingRate.rate(samples)).isEqualTo(ZeroCrossingRate.rate(samples));
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> ZeroCrossingRate.rate(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZeroCrossingRate.rate(new double[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("≥2");
        double[] withNaN = {1, Double.NaN};
        assertThatThrownBy(() -> ZeroCrossingRate.rate(withNaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
