package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 206 §B / T570：批量存入红队——n 次一次记账等价循环；0 次 no-op；
 * 负数拒绝。
 */
class RetryBudgetBulkDepositTest {

    @Test
    void bulkDepositMatchesLoopAndValidates() {
        RetryBudget bulk = RetryBudget.of(20, 0);
        RetryBudget loop = RetryBudget.of(20, 0);
        bulk.deposit(10);
        for (int i = 0; i < 10; i++) {
            loop.deposit();
        }
        assertThat(bulk.balance()).isEqualTo(loop.balance()).isEqualTo(2);

        bulk.deposit(0); // no-op
        assertThat(bulk.balance()).isEqualTo(2);
        assertThatThrownBy(() -> bulk.deposit(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
