package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 178 §B / T534：重试预算红队——流量百分比累积（10 请求 @20% = 2 次
 * 重试额度）；初始底数冷启动可用；耗尽即拒（denied 计数——风暴压制证据）；
 * 故障恢复后随流量回填；refill 运维逃逸；参数 fail-fast。
 * 借鉴：Twitter Finagle retry budget。
 */
class RetryBudgetTest {

    @Test
    void depositsAccumulateAsPercentOfTraffic() {
        RetryBudget budget = RetryBudget.of(20, 0);
        for (int i = 0; i < 10; i++) {
            budget.deposit(); // 10 × 200 毫单位 = 2 次额度
        }
        assertThat(budget.tryAcquire()).isTrue();
        assertThat(budget.tryAcquire()).isTrue();
        assertThat(budget.tryAcquire()).isFalse(); // 耗尽
        assertThat(budget.withdrawn()).isEqualTo(2);
        assertThat(budget.denied()).isEqualTo(1);
    }

    @Test
    void minBalanceCoversColdStart() {
        RetryBudget budget = RetryBudget.of(20, 3);
        assertThat(budget.tryAcquire()).isTrue(); // 底数可用（无流量冷启动）
        assertThat(budget.tryAcquire()).isTrue();
        assertThat(budget.tryAcquire()).isTrue();
        assertThat(budget.tryAcquire()).isFalse();
    }

    @Test
    void recoversWithTrafficAfterExhaustion() {
        RetryBudget budget = RetryBudget.of(50, 0);
        budget.deposit(); // 500 毫
        assertThat(budget.tryAcquire()).isFalse(); // 不足 1 次
        budget.deposit(); // 1000 毫 = 1 次（含截断回填——恢复即有额）
        assertThat(budget.tryAcquire()).isTrue();

        budget.refill(); // 运维逃逸：回填底数 0（本例底数 0——验证不抛）
        assertThat(budget.balance()).isZero();
    }

    @Test
    void parametersValidatedFailFast() {
        assertThatThrownBy(() -> RetryBudget.of(0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RetryBudget.of(1001, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RetryBudget.of(20, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
