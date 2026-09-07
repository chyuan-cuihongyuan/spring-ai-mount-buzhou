package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 348 / impl-371：重试预算健康面回归——有预算恒 UP+数值 /
 * 无预算 UNKNOWN / 机制名。
 */
class RetryBudgetHealthTest {

    @AfterEach
    void clearHolder() {
        RetryBudgetHolder.set(null);
    }

    @Test
    void upWithSnapshotDetailsWhenBudgetPresent() {
        RetryBudget budget = RetryBudget.of(0.2, 100);
        budget.deposit(10); // 余额 2
        for (int i = 0; i < 3; i++) {
            budget.tryAcquire(); // 部分取、部分拦
        }
        RetryBudgetHolder.set(budget);

        RetryBudgetHealth health = new RetryBudgetHealth();
        assertThat(health.mechanism()).isEqualTo("retry-budget");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP); // 恒 UP——拦截是保护
        assertThat(health.details())
                .containsKeys("balance", "withdrawn", "denied")
                .containsKey("note");
    }

    @Test
    void unknownWhenBudgetDisabled() {
        RetryBudgetHealth health = new RetryBudgetHealth();
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(health.details()).containsEntry("reason", "retry-budget-disabled");
    }
}
