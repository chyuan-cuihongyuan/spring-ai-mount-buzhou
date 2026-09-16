package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2027 / T3156：EWMA 合同——首样本锚定、α 加权递推、α=1 直通、
 * 小 α 惯性、单调混合收敛、reset、畸形 fail-fast。
 */
class EwmaEstimatorTest {

    @Test
    void firstSampleShouldAnchorEstimate() {
        EwmaEstimator ewma = new EwmaEstimator(0.2d);
        assertThat(ewma.hasSamples()).isFalse();
        assertThat(ewma.estimate()).isNaN();
        ewma.observe(100.0d);
        assertThat(ewma.estimate()).isEqualTo(100.0d); // 锚定非 0 爬坡
        assertThat(ewma.hasSamples()).isTrue();
    }

    @Test
    void secondSampleShouldBlendByAlpha() {
        EwmaEstimator ewma = new EwmaEstimator(0.5d);
        ewma.observe(100.0d);
        ewma.observe(200.0d);
        assertThat(ewma.estimate()).isEqualTo(150.0d); // 0.5×200+0.5×100
    }

    @Test
    void alphaOneShouldTrackLatestExactly() {
        EwmaEstimator ewma = new EwmaEstimator(1.0d);
        ewma.observe(10);
        ewma.observe(999);
        ewma.observe(42);
        assertThat(ewma.estimate()).isEqualTo(42.0d); // 直通最新
    }

    @Test
    void smallAlphaShouldSmoothSpikes() {
        EwmaEstimator ewma = new EwmaEstimator(0.1d);
        ewma.observe(100);
        ewma.observe(1000); // 尖峰
        assertThat(ewma.estimate()).isEqualTo(190.0d); // 0.1×1000+0.9×100——尖峰被稀释
    }

    @Test
    void repeatedObservationShouldConverge() {
        EwmaEstimator ewma = new EwmaEstimator(0.2d);
        ewma.observe(0);
        for (int i = 0; i < 50; i++) {
            ewma.observe(100);
        }
        assertThat(ewma.estimate()).isCloseTo(100.0d, org.assertj.core.data.Offset.offset(0.01d)); // 收敛到新稳态（α=0.2×50 期残余 ~0.8%）
    }

    @Test
    void monotoneInputShouldStayMonotone() {
        EwmaEstimator ewma = new EwmaEstimator(0.3d);
        double prev = Double.NEGATIVE_INFINITY;
        for (int v = 0; v <= 100; v += 10) {
            ewma.observe(v);
            assertThat(ewma.estimate()).isGreaterThanOrEqualTo(prev); // 单调输入下估计单调
            prev = ewma.estimate();
        }
    }

    @Test
    void resetShouldReturnToUnanchored() {
        EwmaEstimator ewma = new EwmaEstimator();
        ewma.observe(50);
        ewma.reset();
        assertThat(ewma.hasSamples()).isFalse();
        assertThat(ewma.observations()).isZero();
        assertThat(ewma.estimate()).isNaN();
        ewma.observe(7); // 重新锚定
        assertThat(ewma.estimate()).isEqualTo(7.0d);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new EwmaEstimator(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EwmaEstimator(1.5))
                .isInstanceOf(IllegalArgumentException.class);
        EwmaEstimator ewma = new EwmaEstimator();
        assertThatThrownBy(() -> ewma.observe(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ewma.observe(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
