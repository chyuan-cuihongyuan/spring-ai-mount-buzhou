package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1920 / T3042：复合健康分——加权合成、判级、畸形。 */
class HealthScoreCompositeTest {

    /** 等权合成：{90,80,40} → 70 分 DEGRADED。 */
    @Test
    void equalWeightsComposite() {
        double score = HealthScoreComposite.composite(
                new double[]{90, 80, 40}, new double[]{1, 1, 1});
        assertThat(score).isCloseTo(70.0, within(1e-12));
        assertThat(HealthScoreComposite.band(score))
                .isEqualTo(HealthScoreComposite.Band.DEGRADED);
    }

    /** 带权倾斜：关键维度权重主导——{90,40} 权 {1,3} → 52.5。 */
    @Test
    void weightedTiltDominates() {
        double score = HealthScoreComposite.composite(
                new double[]{90, 40}, new double[]{1, 3});
        assertThat(score).isCloseTo(52.5, within(1e-12));
    }

    /** 判级三档边界含下：80 HEALTHY / 50 DEGRADED / 30 UNHEALTHY。 */
    @Test
    void bandBoundariesInclusive() {
        assertThat(HealthScoreComposite.band(80)).isEqualTo(HealthScoreComposite.Band.HEALTHY);
        assertThat(HealthScoreComposite.band(50)).isEqualTo(HealthScoreComposite.Band.DEGRADED);
        assertThat(HealthScoreComposite.band(30)).isEqualTo(HealthScoreComposite.Band.UNHEALTHY);
    }

    /** 畸形入参 fail-fast：不等长、分数越界、零权重。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> HealthScoreComposite.composite(
                new double[]{90}, new double[]{1, 1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同长且非空");
        assertThatThrownBy(() -> HealthScoreComposite.composite(
                new double[]{101}, new double[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分数须在 [0,100]");
        assertThatThrownBy(() -> HealthScoreComposite.composite(
                new double[]{90}, new double[]{0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("权重须 > 0");
    }
}
