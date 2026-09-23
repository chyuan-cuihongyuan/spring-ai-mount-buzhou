package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4020 / T6042：协作预算合同——扣减至尽、让出重置、
 * 独立任务账户、畸形 fail-fast。
 */
class CoopBudgetTest {

    @Test
    void budgetShouldDrainByChargeAndResetByYield() {
        CoopBudget budget = new CoopBudget(3);
        assertThat(budget.remaining()).isEqualTo(3);
        budget.charge();
        budget.charge();
        assertThat(budget.hasBudget()).isTrue();
        budget.charge();
        assertThat(budget.hasBudget()).isFalse();   // 恰尽——让出时机
    }

    @Test
    void yieldShouldRefillToInitial() {
        CoopBudget budget = new CoopBudget(8);
        for (int i = 0; i < 8; i++) {
            budget.charge();
        }
        assertThat(budget.remaining()).isZero();
        budget.yield();   // 重新排队换取新预算
        assertThat(budget.remaining()).isEqualTo(8);
        assertThat(budget.hasBudget()).isTrue();
        assertThat(budget.initial()).isEqualTo(8);
    }

    @Test
    void overchargeShouldFailFast() {
        CoopBudget budget = new CoopBudget(2);
        budget.charge();
        budget.charge();
        assertThatThrownBy(budget::charge)
                .isInstanceOf(IllegalStateException.class);   // 尽后再扣——让出检查缺失
        budget.yield();
        budget.charge();   // 重置后恢复可扣
        assertThat(budget.remaining()).isEqualTo(1);
    }

    @Test
    void independentTasksShouldCarryOwnBudgets() {
        CoopBudget taskA = new CoopBudget(4);
        CoopBudget taskB = new CoopBudget(4);
        taskA.charge();
        taskA.charge();
        taskA.charge();
        taskB.charge();
        assertThat(taskA.remaining()).isEqualTo(1);
        assertThat(taskB.remaining()).isEqualTo(3);   // 账户互不串门
        taskA.yield();
        assertThat(taskB.remaining()).isEqualTo(3);
    }

    @Test
    void invalidBudgetShouldFailFast() {
        assertThatThrownBy(() -> new CoopBudget(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CoopBudget(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
