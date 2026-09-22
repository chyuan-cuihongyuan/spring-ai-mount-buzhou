package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1915 / T3032：百分位排位——排位、极值、直读、畸形。 */
class PercentileRankTest {

    /** 排位：样本 {1..5} 中值 4 → 4/5 = 0.8。 */
    @Test
    void rankPrecise() {
        assertThat(PercentileRank.rank(new long[]{1, 2, 3, 4, 5}, 4))
                .isCloseTo(0.8, within(1e-12));
    }

    /** 极值：高于最大 1.0、低于最小 0.2（最小值本身 ≤ 计入）。 */
    @Test
    void extremesClamped() {
        long[] samples = {1, 2, 3, 4, 5};
        assertThat(PercentileRank.rank(samples, 100)).isCloseTo(1.0, within(1e-12));
        assertThat(PercentileRank.rank(samples, 0)).isCloseTo(0.0, within(1e-12));
        assertThat(PercentileRank.rank(samples, 1)).isCloseTo(0.2, within(1e-12));
    }

    /** 百分位直读：0.8 → 80。 */
    @Test
    void percentileDirectRead() {
        assertThat(PercentileRank.percentileOf(new long[]{1, 2, 3, 4, 5}, 4))
                .isEqualTo(80);
    }

    /** 畸形入参 fail-fast：空表、负样本。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> PercentileRank.rank(new long[0], 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("样本表不能为空");
        assertThatThrownBy(() -> PercentileRank.rank(new long[]{-1}, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("样本值不能为负");
    }
}
