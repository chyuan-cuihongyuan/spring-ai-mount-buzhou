package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1809 / T2820：扇出 pacing——头部免节流、尾部匀速、跨度与占比读数。 */
class FanoutPacingPlanTest {

    /** 头部 K 立即发（IW 语义），尾部按间隔匀速放行。 */
    @Test
    void headStartImmediateAndTailPaced() {
        FanoutPacingPlan.Plan plan = FanoutPacingPlan.plan(5, 100L, 2);
        assertThat(plan.starts()).hasSize(5);
        assertThat(plan.starts().get(0).delayMillis()).isZero();
        assertThat(plan.starts().get(1).delayMillis()).isZero();
        assertThat(plan.starts().get(2).delayMillis()).isEqualTo(100L);
        assertThat(plan.starts().get(3).delayMillis()).isEqualTo(200L);
        assertThat(plan.starts().get(4).delayMillis()).isEqualTo(300L);
        assertThat(plan.totalSpanMillis()).isEqualTo(300L);
        assertThat(plan.pacedRatio()).isEqualTo(0.6d);
    }

    /** headStart=0 全量 pacing（首任务也吃一个间隔）；headStart=fanout 全立即。 */
    @Test
    void extremesOfHeadStart() {
        FanoutPacingPlan.Plan allPaced = FanoutPacingPlan.plan(3, 50L, 0);
        assertThat(allPaced.starts())
                .extracting(FanoutPacingPlan.TaskStart::delayMillis)
                .containsExactly(50L, 100L, 150L);
        assertThat(allPaced.pacedRatio()).isEqualTo(1.0d);

        FanoutPacingPlan.Plan allImmediate = FanoutPacingPlan.plan(3, 50L, 3);
        assertThat(allImmediate.starts())
                .extracting(FanoutPacingPlan.TaskStart::delayMillis)
                .containsOnly(0L);
        assertThat(allImmediate.pacedRatio()).isZero();
        assertThat(allImmediate.totalSpanMillis()).isZero();
    }

    /** 零扇出：空计划、跨度 0、占比 -1 哨兵。 */
    @Test
    void zeroFanoutYieldsEmptyPlan() {
        FanoutPacingPlan.Plan plan = FanoutPacingPlan.plan(0, 100L, 0);
        assertThat(plan.starts()).isEmpty();
        assertThat(plan.totalSpanMillis()).isZero();
        assertThat(plan.pacedRatio()).isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：负扇出、零间隔、名额越界（负或超扇出）。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> FanoutPacingPlan.plan(-1, 100L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fanout 不能为负");
        assertThatThrownBy(() -> FanoutPacingPlan.plan(3, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intervalMillis 不能小于 1");
        assertThatThrownBy(() -> FanoutPacingPlan.plan(3, 100L, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("headStart 越界");
        assertThatThrownBy(() -> FanoutPacingPlan.plan(3, 100L, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 任务序完整连续（0..n-1，无缺号错号）。 */
    @Test
    void taskIndexesAreContiguous() {
        FanoutPacingPlan.Plan plan = FanoutPacingPlan.plan(4, 10L, 1);
        assertThat(plan.starts())
                .extracting(FanoutPacingPlan.TaskStart::taskIndex)
                .containsExactly(0, 1, 2, 3);
        assertThat(plan.starts().get(3).delayMillis()).isEqualTo(30L);
    }
}
