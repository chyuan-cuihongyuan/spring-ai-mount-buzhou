package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 2048 / T3198：卡方均匀性合同——完全均匀统计量 0 不拒、显著偏斜
 * 拒绝、临界值表、统计量计算、畸形 fail-fast。
 */
class ChiSquareUniformityTest {

    @Test
    void perfectlyUniformObservationShouldNotReject() {
        ChiSquareUniformity.ChiSquareResult result = ChiSquareUniformity.test(
                new long[]{50, 50, 50, 50});
        assertThat(result.statistic()).isZero(); // O=E 全桶——χ²=0
        assertThat(result.degreesOfFreedom()).isEqualTo(3);
        assertThat(result.rejectsUniform()).isFalse();
    }

    @Test
    void heavilySkewedObservationShouldReject() {
        // 97% 落一桶——显著偏斜
        ChiSquareUniformity.ChiSquareResult result = ChiSquareUniformity.test(
                new long[]{97, 1, 1, 1});
        assertThat(result.statistic()).isGreaterThan(result.criticalValue());
        assertThat(result.rejectsUniform()).isTrue();
    }

    @Test
    void mildVariationWithinNoiseShouldNotReject() {
        // 轻微波动的均匀
        ChiSquareUniformity.ChiSquareResult result = ChiSquareUniformity.test(
                new long[]{52, 49, 51, 48});
        assertThat(result.statistic())
                .as("轻波动应远低于临界").isLessThan(result.criticalValue());
        assertThat(result.rejectsUniform()).isFalse();
    }

    @Test
    void statisticFormulaShouldMatchHandComputation() {
        // 两桶 [60,40]：E=50，χ² = (10²/50)×2 = 4.0 —— 恰超 1 自由度临界 3.841
        ChiSquareUniformity.ChiSquareResult result = ChiSquareUniformity.test(
                new long[]{60, 40});
        assertThat(result.statistic()).isCloseTo(4.0d, within(1e-12));
        assertThat(result.criticalValue()).isCloseTo(3.841d, within(1e-3));
        assertThat(result.rejectsUniform()).isTrue(); // 60:40 已显著
    }

    @Test
    void criticalValueShouldGrowWithDegreesOfFreedom() {
        double critical4 = ChiSquareUniformity.test(new long[]{1, 1, 1, 1, 1}).criticalValue();
        double critical10 = ChiSquareUniformity.test(new long[]{1,1,1,1,1,1,1,1,1,1,1}).criticalValue();
        assertThat(critical10).isGreaterThan(critical4);
    }

    @Test
    void emptyCountsAreAllowedAsLongAsTotalPositive() {
        // 一桶 0 其余正——零观测桶合法（期望>0 总量保证）
        ChiSquareUniformity.ChiSquareResult result = ChiSquareUniformity.test(
                new long[]{0, 10, 10});
        assertThat(result.statistic()).isPositive();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> ChiSquareUniformity.test(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChiSquareUniformity.test(new long[]{5}))
                .isInstanceOf(IllegalArgumentException.class); // 单桶无自由度
        assertThatThrownBy(() -> ChiSquareUniformity.test(new long[22]))
                .isInstanceOf(IllegalArgumentException.class); // 超表界
        assertThatThrownBy(() -> ChiSquareUniformity.test(new long[]{0, 0}))
                .isInstanceOf(IllegalArgumentException.class); // 零总量
        assertThatThrownBy(() -> ChiSquareUniformity.test(new long[]{5, -1}))
                .isInstanceOf(IllegalArgumentException.class); // 负频数
    }
}
