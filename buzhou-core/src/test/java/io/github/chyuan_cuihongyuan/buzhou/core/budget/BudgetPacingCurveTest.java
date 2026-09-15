package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1819 / T2840：花费匀速——三态判、边界含、运行率。 */
class BudgetPacingCurveTest {

    /** 三态判定：带内/超前/落后，偏离与运行率读数。 */
    @Test
    void shouldClassifyThreePacingStates() {
        BudgetPacingCurve.PacingReport onPace =
                BudgetPacingCurve.evaluate(0.5d, 0.52d, 0.05d);
        assertThat(onPace.pacing()).isEqualTo(BudgetPacingCurve.Pacing.ON_PACE);
        assertThat(onPace.deviation()).isCloseTo(0.02d, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(onPace.runRate()).isEqualTo(1.04d);

        BudgetPacingCurve.PacingReport over =
                BudgetPacingCurve.evaluate(0.3d, 0.6d, 0.05d);
        assertThat(over.pacing()).isEqualTo(BudgetPacingCurve.Pacing.OVER_PACING);
        assertThat(over.runRate()).isEqualTo(2.0d); // 按此速度期末烧两倍

        BudgetPacingCurve.PacingReport under =
                BudgetPacingCurve.evaluate(0.8d, 0.2d, 0.05d);
        assertThat(under.pacing()).isEqualTo(BudgetPacingCurve.Pacing.UNDER_PACING);
        assertThat(under.runRate()).isEqualTo(0.25d);
    }

    /** 边界含：偏离恰好等于容差仍在带内。 */
    @Test
    void toleranceBoundaryIsInclusive() {
        assertThat(BudgetPacingCurve.evaluate(0.5d, 0.55d, 0.05d).pacing())
                .isEqualTo(BudgetPacingCurve.Pacing.ON_PACE);
        assertThat(BudgetPacingCurve.evaluate(0.5d, 0.45d, 0.05d).pacing())
                .isEqualTo(BudgetPacingCurve.Pacing.ON_PACE);
    }

    /** 端点：周期首（elapsed=0 运行率 -1 哨兵）与周期末（满花费）。 */
    @Test
    void endpointsBehave() {
        BudgetPacingCurve.PacingReport start =
                BudgetPacingCurve.evaluate(0d, 0d, 0.01d);
        assertThat(start.runRate()).isEqualTo(-1d);
        assertThat(start.pacing()).isEqualTo(BudgetPacingCurve.Pacing.ON_PACE);

        BudgetPacingCurve.PacingReport end =
                BudgetPacingCurve.evaluate(1d, 1d, 0d);
        assertThat(end.pacing()).isEqualTo(BudgetPacingCurve.Pacing.ON_PACE);
        assertThat(end.runRate()).isEqualTo(1.0d);
    }

    /** 畸形入参 fail-fast：分数越界/NaN、负容差。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> BudgetPacingCurve.evaluate(1.5d, 0.5d, 0.1d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elapsedFraction 须在 [0,1]");
        assertThatThrownBy(() -> BudgetPacingCurve.evaluate(0.5d, -0.1d, 0.1d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spentFraction 须在 [0,1]");
        assertThatThrownBy(() -> BudgetPacingCurve.evaluate(0.5d, 0.5d, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tolerance 须 ≥ 0");
        assertThatThrownBy(() -> BudgetPacingCurve.evaluate(Double.NaN, 0.5d, 0.1d))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
