package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 4036 / T6074：标量卡尔曼合同——常值收敛、方差收缩、
 * 增益闭环（update 降 predict 升）、平滑降噪、确定性回放、
 * 畸形 fail-fast。
 */
class ScalarKalmanFilterTest {

    private static final double PROCESS_NOISE = 0.01;
    private static final double MEASUREMENT_NOISE = 1.0;

    @Test
    void constantSignalShouldConvergeWithShrinkingVariance() {
        ScalarKalmanFilter filter = new ScalarKalmanFilter(0.0, 100.0,
                PROCESS_NOISE, MEASUREMENT_NOISE);
        double previousVariance = filter.variance();
        for (int i = 0; i < 50; i++) {
            filter.observe(42.0);
            assertThat(filter.variance()).isLessThanOrEqualTo(previousVariance);
            previousVariance = filter.variance();
        }
        assertThat(filter.state()).isCloseTo(42.0, within(0.1));
    }

    @Test
    void gainShouldDecreaseOnPureUpdatesAndReboundOnPredict() {
        ScalarKalmanFilter filter = new ScalarKalmanFilter(0.0, 10.0, PROCESS_NOISE, MEASUREMENT_NOISE);
        double previousGain = Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            filter.update(1.0);
            assertThat(filter.lastGain()).isLessThan(previousGain);   // 越信模型越不惊弓
            previousGain = filter.lastGain();
        }
        double beforePredictGain = filter.lastGain();
        double varianceBefore = filter.variance();
        for (int i = 0; i < 10; i++) {
            filter.predict();   // 长期无观测——不确定性持续累积
        }
        assertThat(filter.variance()).isGreaterThan(varianceBefore);
        double varianceAfterPredicts = filter.variance();
        filter.update(1.0);
        assertThat(filter.lastGain()).isGreaterThan(beforePredictGain);   // 方差回升重信读数
        assertThat(filter.variance()).isLessThan(varianceAfterPredicts);   // update 收缩
    }

    @Test
    void smoothingShouldBeatRawNoise() {
        java.util.Random random = new java.util.Random(42L);
        ScalarKalmanFilter filter = new ScalarKalmanFilter(0.0, 10.0, PROCESS_NOISE, MEASUREMENT_NOISE);
        double trueValue = 100.0;
        double rawSquaredError = 0.0;
        double filteredSquaredError = 0.0;
        for (int i = 0; i < 200; i++) {
            double noisy = trueValue + random.nextGaussian(0, 5.0);
            filter.observe(noisy);
            rawSquaredError += (noisy - trueValue) * (noisy - trueValue);
            filteredSquaredError += (filter.state() - trueValue) * (filter.state() - trueValue);
        }
        assertThat(filteredSquaredError).isLessThan(rawSquaredError);
    }

    @Test
    void sameObservationsShouldReplaySameTrajectory() {
        double[] readings = {10, 12, 11, 40, 41, 39, 40, 40};
        ScalarKalmanFilter first = new ScalarKalmanFilter(0.0, 25.0, PROCESS_NOISE, MEASUREMENT_NOISE);
        ScalarKalmanFilter second = new ScalarKalmanFilter(0.0, 25.0, PROCESS_NOISE, MEASUREMENT_NOISE);
        for (double reading : readings) {
            first.observe(reading);
            second.observe(reading);
            assertThat(first.state()).isEqualTo(second.state());
            assertThat(first.variance()).isEqualTo(second.variance());
        }
    }

    @Test
    void invalidConstructionAndReadingsShouldFailFast() {
        assertThatThrownBy(() -> new ScalarKalmanFilter(0, 10, -1, MEASUREMENT_NOISE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScalarKalmanFilter(0, 10, PROCESS_NOISE, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScalarKalmanFilter(0, 0, PROCESS_NOISE, MEASUREMENT_NOISE))
                .isInstanceOf(IllegalArgumentException.class);
        ScalarKalmanFilter filter = new ScalarKalmanFilter(0, 10, PROCESS_NOISE, MEASUREMENT_NOISE);
        assertThatThrownBy(() -> filter.update(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> filter.update(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
