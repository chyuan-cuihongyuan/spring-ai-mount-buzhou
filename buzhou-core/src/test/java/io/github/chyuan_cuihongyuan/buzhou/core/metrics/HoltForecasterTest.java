package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3021 / T5044：Holt 合同——常量序列水平恒定趋势零（精确）、
 * 线性序列趋势收敛到斜率+一步预测贴真值、首观测初始化、步进随
 * 趋势缩放、空态 NaN、参数开区间、加速序列趋势上行。
 */
class HoltForecasterTest {

    @Test
    void constantSeriesShouldStayLevelWithZeroTrend() {
        HoltForecaster holt = new HoltForecaster(0.5, 0.5);
        for (int i = 0; i < 20; i++) {
            holt.observe(5.0);
        }
        assertThat(holt.level()).isEqualTo(5.0);
        assertThat(holt.trend()).isEqualTo(0.0);
        assertThat(holt.forecast(5)).isEqualTo(5.0);
    }

    @Test
    void linearSeriesShouldConvergeToTrueSlope() {
        // y = 3 + 2i：水平贴当前值、趋势收敛 2、一步预测贴下一真值
        HoltForecaster holt = new HoltForecaster(0.5, 0.5);
        for (int i = 0; i < 60; i++) {
            holt.observe(3 + 2.0 * i);
        }
        assertThat(holt.trend()).isCloseTo(2.0, within(0.05));
        double nextTrue = 3 + 2.0 * 60;
        assertThat(holt.forecast(1)).isCloseTo(nextTrue, within(0.5));
    }

    @Test
    void firstObservationShouldInitialize() {
        HoltForecaster holt = new HoltForecaster(0.3, 0.2);
        holt.observe(42);
        assertThat(holt.level()).isEqualTo(42);
        assertThat(holt.trend()).isZero();
        assertThat(holt.forecast(0)).isEqualTo(42);
        assertThat(holt.observations()).isEqualTo(1);
    }

    @Test
    void forecastStepsShouldScaleWithTrend() {
        HoltForecaster holt = new HoltForecaster(0.6, 0.6);
        for (int i = 0; i < 50; i++) {
            holt.observe(10 + 1.5 * i);
        }
        double one = holt.forecast(1);
        double two = holt.forecast(2);
        assertThat(two - one).isCloseTo(holt.trend(), within(1e-9));
        assertThat(two - one).isCloseTo(1.5, within(0.05));
    }

    @Test
    void noObservationShouldBeNaN() {
        HoltForecaster holt = new HoltForecaster(0.5, 0.5);
        assertThat(holt.forecast(0)).isNaN();
        assertThat(holt.level()).isNaN();
        assertThat(holt.observations()).isZero();
    }

    @Test
    void acceleratingSeriesShouldLiftTrend() {
        // y = i²：加速段后 50 步的趋势应高于前 50 步（趋势分量跟涨）
        HoltForecaster holt = new HoltForecaster(0.7, 0.7);
        for (int i = 0; i < 50; i++) {
            holt.observe((double) (i * i));
        }
        double midTrend = holt.trend();
        for (int i = 50; i < 100; i++) {
            holt.observe((double) (i * i));
        }
        assertThat(holt.trend()).isGreaterThan(midTrend);
    }

    @Test
    void parametersAndStepsShouldBeValidated() {
        assertThatThrownBy(() -> new HoltForecaster(0, 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HoltForecaster(0.5, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HoltForecaster(-0.1, 0.5)).isInstanceOf(IllegalArgumentException.class);
        HoltForecaster holt = new HoltForecaster(0.5, 0.5);
        holt.observe(1);
        assertThatThrownBy(() -> holt.forecast(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
