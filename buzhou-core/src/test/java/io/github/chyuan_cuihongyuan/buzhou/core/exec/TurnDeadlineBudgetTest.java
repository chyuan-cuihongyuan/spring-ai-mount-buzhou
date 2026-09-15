package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1802 / T2806：deadline 传播——截断、耗尽即拒、前缀性。 */
class TurnDeadlineBudgetTest {

    /** 前缀传播：预算内获准、末尾截断（min 规则）、耗尽后拒绝。 */
    @Test
    void shouldTruncateTailAndRejectAfterExhaustion() {
        TurnDeadlineBudget.BudgetPlan plan = TurnDeadlineBudget.plan(100L, List.of(30L, 50L, 40L, 10L));
        assertThat(plan.allocations()).hasSize(4);
        assertThat(plan.allocations().get(0).admitted()).isTrue();
        assertThat(plan.allocations().get(0).effectiveTimeoutNanos()).isEqualTo(30L);
        assertThat(plan.allocations().get(1).effectiveTimeoutNanos()).isEqualTo(50L);
        // 剩 20：min(40, 20)=20 —— 截断而非拒绝
        assertThat(plan.allocations().get(2).admitted()).isTrue();
        assertThat(plan.allocations().get(2).effectiveTimeoutNanos()).isEqualTo(20L);
        // 预算耗尽：不再起工
        assertThat(plan.allocations().get(3).admitted()).isFalse();
        assertThat(plan.committedNanos()).isEqualTo(100L);
        assertThat(plan.admissionRatio()).isEqualTo(0.75d);
    }

    /** 早期调用花光预算 → 后续全拒（顺序传播，不各自重新计时）。 */
    @Test
    void exhaustionIsPrefixWide() {
        TurnDeadlineBudget.BudgetPlan plan = TurnDeadlineBudget.plan(10L,
                List.of(10L, 1L, 0L, 5L));
        assertThat(plan.allocations().get(0).effectiveTimeoutNanos()).isEqualTo(10L);
        for (int i = 1; i < 4; i++) {
            assertThat(plan.allocations().get(i).admitted()).isFalse();
        }
        assertThat(plan.admissionRatio()).isEqualTo(0.25d);
    }

    /** 零耗预估在预算耗尽后同样拒绝——deadline 先于派发检查。 */
    @Test
    void zeroBudgetRejectsEverythingIncludingZeroCost() {
        TurnDeadlineBudget.BudgetPlan plan = TurnDeadlineBudget.plan(0L, List.of(0L, 0L));
        assertThat(plan.allocations()).allSatisfy(a -> assertThat(a.admitted()).isFalse());
        assertThat(plan.committedNanos()).isZero();
    }

    /** 空表与 null 同口径：零分配、committed 0、获准率 -1 哨兵。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (TurnDeadlineBudget.BudgetPlan plan : List.of(
                TurnDeadlineBudget.plan(100L, List.of()),
                TurnDeadlineBudget.plan(100L, null))) {
            assertThat(plan.allocations()).isEmpty();
            assertThat(plan.committedNanos()).isZero();
            assertThat(plan.admissionRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形入参 fail-fast：负预算与负预估。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> TurnDeadlineBudget.plan(-1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurnDeadlineBudget.plan(10L, List.of(5L, -5L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("第 1 项预估不能为负");
    }
}
