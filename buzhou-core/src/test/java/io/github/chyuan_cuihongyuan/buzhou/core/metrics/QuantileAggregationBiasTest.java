package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1891 / T2984：分位聚合偏差——naive、偏差比、容差、畸形。 */
class QuantileAggregationBiasTest {

    /** 经典 {100,500}：naive 300；真值 500 → 偏差 −0.4（低报危险侧）。 */
    @Test
    void classicShardAverageUnderreports() {
        assertThat(QuantileAggregationBias.naiveAverage(new double[]{100, 500}))
                .isCloseTo(300.0, within(1e-12));
        assertThat(QuantileAggregationBias.biasRatio(300, 500))
                .isCloseTo(-0.4, within(1e-12));
        assertThat(QuantileAggregationBias.isMateriallyBiased(300, 500, 0.10)).isTrue();
    }

    /** 高报侧：naive > actual 偏差为正——过悲观但不是违约误判方向。 */
    @Test
    void overReportingIsPositive() {
        assertThat(QuantileAggregationBias.biasRatio(500, 300))
                .isCloseTo(2.0 / 3, within(1e-12));
    }

    /** 无偏判定：naive 恰等真值偏差 0；容差两侧行为。 */
    @Test
    void unbiasedAndToleranceBoundary() {
        assertThat(QuantileAggregationBias.biasRatio(300, 300)).isCloseTo(0.0, within(1e-12));
        assertThat(QuantileAggregationBias.isMateriallyBiased(305, 300, 0.02)).isFalse();
        assertThat(QuantileAggregationBias.isMateriallyBiased(306, 300, 0.02)).isTrue();
    }

    /** 畸形入参 fail-fast：空表、负值、actual=0、容差越界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> QuantileAggregationBias.naiveAverage(new double[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分片分位表不能为空");
        assertThatThrownBy(() -> QuantileAggregationBias.naiveAverage(new double[]{-1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分位值不能为负");
        assertThatThrownBy(() -> QuantileAggregationBias.biasRatio(300, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actual 须 > 0");
        assertThatThrownBy(() -> QuantileAggregationBias.isMateriallyBiased(300, 300, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tolerance 须在 [0,1]");
    }
}
