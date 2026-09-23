package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1921 / T3044：时钟抖动——恒定、已知集、滚动、畸形。 */
class ClockJitterMeterTest {

    /** 恒定偏斜：偏差恒 50 → 抖动 0、均值 50（纯偏斜可校）。 */
    @Test
    void constantOffsetZeroJitter() {
        ClockJitterMeter meter = new ClockJitterMeter(8);
        for (int i = 0; i < 5; i++) {
            meter.record(50);
        }
        assertThat(meter.jitterMillis()).isCloseTo(0.0, within(1e-12));
        assertThat(meter.meanOffsetMillis()).isCloseTo(50.0, within(1e-12));
    }

    /** 已知样本集 {0,10} 抖动 = 5（总体标准差）。 */
    @Test
    void knownPopulationStddev() {
        ClockJitterMeter meter = new ClockJitterMeter(8);
        meter.record(0);
        meter.record(10);
        assertThat(meter.jitterMillis()).isCloseTo(5.0, within(1e-12));
        assertThat(meter.meanOffsetMillis()).isCloseTo(5.0, within(1e-12));
    }

    /** 不足样本哨兵：0/1 样本 → -1.0 诚实「样本不足」。 */
    @Test
    void insufficientSamplesSentinel() {
        ClockJitterMeter meter = new ClockJitterMeter(4);
        assertThat(meter.jitterMillis()).isEqualTo(-1.0);
        meter.record(5);
        assertThat(meter.jitterMillis()).isEqualTo(-1.0);
        meter.record(5);
        assertThat(meter.jitterMillis()).isCloseTo(0.0, within(1e-12));
    }

    /** 窗口滚动：满窗覆盖最旧（容量 3 记 4 次 → 只剩后三次）。 */
    @Test
    void windowRollsOver() {
        ClockJitterMeter meter = new ClockJitterMeter(3);
        meter.record(100);
        meter.record(100);
        meter.record(100);
        meter.record(0);
        // 窗口现为 {100,100,0}——旧 100 被覆盖
        assertThat(meter.snapshot()).containsExactly(100L, 100L, 0L);
        assertThat(meter.capacity()).isEqualTo(3);
        assertThatThrownBy(() -> new ClockJitterMeter(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("windowSize 不能小于 2");
    }
}
