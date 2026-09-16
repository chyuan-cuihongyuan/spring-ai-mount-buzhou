package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.RequiredChecksRollup.CheckState;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.RequiredChecksRollup.Rollup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2031 / T3164：必选检查聚合合同——一票否决（FAILURE 优先于
 * PENDING）、未报即 PENDING、全绿放行、可选不阻断但显形、畸形
 * fail-fast。
 */
class RequiredChecksRollupTest {

    @Test
    void allRequiredSuccessShouldRollupSuccess() {
        RequiredChecksRollup rollup = new RequiredChecksRollup();
        rollup.register("lint", true);
        rollup.register("test", true);
        rollup.report("lint", CheckState.SUCCESS);
        rollup.report("test", CheckState.SUCCESS);
        assertThat(rollup.rollup()).isEqualTo(Rollup.SUCCESS);
    }

    @Test
    void unreportedRequiredShouldStayPending() {
        RequiredChecksRollup rollup = new RequiredChecksRollup();
        rollup.register("lint", true);
        rollup.register("test", true);
        rollup.report("lint", CheckState.SUCCESS);
        assertThat(rollup.rollup()).isEqualTo(Rollup.PENDING); // test 未报
        assertThat(rollup.stats().unreportedRequired()).isEqualTo(1);
    }

    @Test
    void explicitPendingShouldAlsoHold() {
        RequiredChecksRollup rollup = new RequiredChecksRollup();
        rollup.register("lint", true);
        rollup.report("lint", CheckState.PENDING); // 显式挂起
        assertThat(rollup.rollup()).isEqualTo(Rollup.PENDING);
    }

    @Test
    void anyRequiredFailureShouldVeto() {
        RequiredChecksRollup rollup = new RequiredChecksRollup();
        rollup.register("lint", true);
        rollup.register("test", true);
        rollup.register("eval", true);
        rollup.report("lint", CheckState.SUCCESS);
        rollup.report("eval", CheckState.PENDING);
        rollup.report("test", CheckState.FAILURE);
        // FAILURE 一票否决——优先于 PENDING（已失败不必等挂起者）
        assertThat(rollup.rollup()).isEqualTo(Rollup.FAILURE);
    }

    @Test
    void optionalFailureShouldNotBlockButSurface() {
        RequiredChecksRollup rollup = new RequiredChecksRollup();
        rollup.register("lint", true);
        rollup.register("advisory", false); // 可选
        rollup.report("lint", CheckState.SUCCESS);
        rollup.report("advisory", CheckState.FAILURE);
        assertThat(rollup.rollup()).isEqualTo(Rollup.SUCCESS); // 不阻断
        assertThat(rollup.stats().optionalFailures()).isEqualTo(1); // 但显形
    }

    @Test
    void emptyRollupShouldBeSuccess() {
        // 无必选检查——门恒开（空集语义：无阻断项即放行）
        assertThat(new RequiredChecksRollup().rollup()).isEqualTo(Rollup.SUCCESS);
    }

    @Test
    void statesSnapshotShouldFollowRegistrationOrder() {
        RequiredChecksRollup rollup = new RequiredChecksRollup();
        rollup.register("b", true);
        rollup.register("a", true);
        rollup.report("a", CheckState.SUCCESS);
        assertThat(rollup.states().keySet()).containsExactly("b", "a"); // 注册序
        assertThat(rollup.states()).containsEntry("a", CheckState.SUCCESS)
                .containsEntry("b", CheckState.PENDING);
    }

    @Test
    void malformedInputsShouldFailFast() {
        RequiredChecksRollup rollup = new RequiredChecksRollup();
        assertThatThrownBy(() -> rollup.register(null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rollup.register(" ", true))
                .isInstanceOf(IllegalArgumentException.class);
        rollup.register("x", true);
        assertThatThrownBy(() -> rollup.register("x", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已注册");
        assertThatThrownBy(() -> rollup.report("ghost", CheckState.SUCCESS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rollup.report("x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
