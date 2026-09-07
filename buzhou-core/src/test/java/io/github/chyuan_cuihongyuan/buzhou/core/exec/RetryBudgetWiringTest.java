package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 302 / impl-325：工具重试预算接线回归——预算拒绝中途 fail-fast / 每逻辑调用
 * deposit / holder 未启用时重试照常。
 */
class RetryBudgetWiringTest {

    @AfterEach
    void clearHolder() {
        RetryBudgetHolder.set(null);
    }

    /** 恒失败工具（计数每次真实执行）。 */
    private ToolCallback failingTool(AtomicInteger invocations) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                invocations.incrementAndGet();
                throw new IllegalStateException("boom");
            }
        };
    }

    @Test
    void shouldFailFastWithoutRetrying_whenBudgetDenied() {
        // minBalance=0 + percent 极小 → 零余额：首失败后重试必被拒
        RetryBudget budget = RetryBudget.of(0.1, 0);
        AtomicInteger invocations = new AtomicInteger();
        RetryingToolCallback callback = RetryingToolCallback.wrap(failingTool(invocations),
                new RetryingToolCallback.RetryPolicy(5, Duration.ofMillis(1), Duration.ofMillis(2)),
                budget);

        assertThatThrownBy(() -> callback.call("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        assertThat(invocations.get()).as("预算拒绝即止：只有首次真实执行").isEqualTo(1);
        assertThat(budget.denied()).isEqualTo(1);
    }

    @Test
    void shouldDepositPerLogicalCall_andAllowRetriesWithinBalance() {
        // minBalance=3：预存 3 次重试额度，策略 2 次重试在额度内
        RetryBudget budget = RetryBudget.of(0.1, 3);
        AtomicInteger invocations = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();
        ToolCallback flaky = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return invocations.incrementAndGet() == 1 ? throwBoom() : "ok-" + successes.incrementAndGet();
            }

            private String throwBoom() {
                throw new IllegalStateException("transient");
            }
        };
        RetryingToolCallback callback = RetryingToolCallback.wrap(flaky,
                new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(2)),
                budget);

        assertThat(callback.call("{}")).isEqualTo("ok-1");
        assertThat(budget.withdrawn()).as("一次重试支取一次").isEqualTo(1);
    }

    @Test
    void shouldRetryAsBefore_whenBudgetAbsent() {
        RetryBudgetHolder.set(null); // holder 默认未启用
        AtomicInteger invocations = new AtomicInteger();
        RetryingToolCallback callback = RetryingToolCallback.wrap(failingTool(invocations),
                new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(2)));

        assertThatThrownBy(() -> callback.call("{}")).hasMessage("boom");
        assertThat(invocations.get()).as("未启用预算：3 次尝试全走（既有行为）").isEqualTo(3);
    }

    @Test
    void shouldReadHolderByDefault_inTwoArgWrap() {
        RetryBudget budget = RetryBudget.of(0.1, 0);
        RetryBudgetHolder.set(budget);
        AtomicInteger invocations = new AtomicInteger();
        RetryingToolCallback callback = RetryingToolCallback.wrap(failingTool(invocations),
                new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(2)));

        assertThatThrownBy(() -> callback.call("{}")).hasMessage("boom");
        assertThat(invocations.get()).as("两参 wrap 默认接 holder：预算拒绝即止").isEqualTo(1);
    }
}
