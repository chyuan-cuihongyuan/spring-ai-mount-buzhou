package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 318 / impl-341：会话扰乱预算回归——额度随预留递减/触底拒绝/归还恢复/
 * 0 不限/计数器/构造校验。
 */
class SessionDisruptionBudgetTest {

    @Test
    void allowanceDecreasesWithReservations_andRecoversOnComplete() {
        AtomicLong active = new AtomicLong(10);
        SessionDisruptionBudget budget = new SessionDisruptionBudget(active::get, 5);

        assertThat(budget.tryAcquireDisruption()).isTrue(); // 10-0 > 5
        assertThat(budget.tryAcquireDisruption()).isTrue(); // 10-1 > 5
        assertThat(budget.tryAcquireDisruption()).isTrue(); // 10-2 > 5
        assertThat(budget.tryAcquireDisruption())
                .as("10-3=7 > 5 边界：available-reserved 须严格大于 minAvailable").isTrue();
        assertThat(budget.tryAcquireDisruption())
                .as("10-4=6 > 5").isTrue();
        assertThat(budget.tryAcquireDisruption())
                .as("10-5=5 不再大于 5——触底拒绝").isFalse();

        budget.completeDisruption();
        assertThat(budget.tryAcquireDisruption()).isTrue(); // 归还一格恢复
    }

    @Test
    void availableIsObservable() {
        AtomicLong active = new AtomicLong(8);
        SessionDisruptionBudget budget = new SessionDisruptionBudget(active::get, 3);
        assertThat(budget.disruptionsAvailable()).isEqualTo(5);
        budget.tryAcquireDisruption();
        assertThat(budget.disruptionsAvailable()).isEqualTo(4);
    }

    @Test
    void zeroMinAvailableAllowsDrainingAllExisting() {
        AtomicLong active = new AtomicLong(3);
        SessionDisruptionBudget budget = new SessionDisruptionBudget(active::get, 0);
        for (int i = 0; i < 3; i++) {
            assertThat(budget.tryAcquireDisruption())
                    .as("min=0：存量全可排（K8s PDB 同语义）").isTrue();
        }
        active.set(5); // 新会话进来——额度随存量恢复
        assertThat(budget.tryAcquireDisruption()).isTrue();
    }

    @Test
    void countersRecordAllowedAndRejected() {
        AtomicLong active = new AtomicLong(4);
        SessionDisruptionBudget budget = new SessionDisruptionBudget(active::get, 4);
        assertThat(budget.tryAcquireDisruption()).isFalse();
        active.set(10);
        assertThat(budget.tryAcquireDisruption()).isTrue();
        assertThat(budget.allowedCount()).isEqualTo(1);
        assertThat(budget.rejectedCount()).isEqualTo(1);
    }

    @Test
    void constructorValidates() {
        assertThatThrownBy(() -> new SessionDisruptionBudget(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionDisruptionBudget(() -> 1L, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completeWithoutAcquireFloorsAtZero() {
        SessionDisruptionBudget budget = new SessionDisruptionBudget(() -> 10L, 0);
        budget.completeDisruption();
        budget.completeDisruption();
        assertThat(budget.disruptionsAvailable()).isEqualTo(10);
    }
}
