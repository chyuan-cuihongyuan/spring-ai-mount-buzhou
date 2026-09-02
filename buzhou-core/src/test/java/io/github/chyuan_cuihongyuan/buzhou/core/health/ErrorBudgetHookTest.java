package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 321 / impl-344：错误预算喂数 hook 回归——结构化标记记败/正常记成/
 * beforeTool 恒 CONTINUE（纯观察）/null 防御（DefaultToolCallContext 先例 131）。
 */
class ErrorBudgetHookTest {

    /** 快测：SLO 99% / burn 阈 2 / min-samples 1——单样本即可判。 */
    private static ErrorBudget sensitiveBudget() {
        return new ErrorBudget(new ErrorBudget.Config(99, 2.0, 4, Duration.ofMillis(200), 1),
                Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void structuredMarkerCountsAsFailure() {
        ErrorBudget budget = sensitiveBudget();
        ErrorBudgetHook hook = new ErrorBudgetHook(budget);
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        DefaultToolCallContext ctx =
                new DefaultToolCallContext(env, "tc1", "flaky", Map.of());
        ctx.markExecuted("[工具执行失败]\n工具：flaky\n原因：下游 503", null);
        assertThat(hook.afterTool(ctx)).isEqualTo(HookResult.CONTINUE);

        assertThat(budget.samples("flaky")).isEqualTo(1);
        assertThat(budget.breaching("flaky"))
                .as("1 样本 100% 错误率 → burn 100 ≥ 2").isTrue();
    }

    @Test
    void normalResultCountsAsSuccess() {
        ErrorBudget budget = sensitiveBudget();
        ErrorBudgetHook hook = new ErrorBudgetHook(budget);
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        DefaultToolCallContext ctx =
                new DefaultToolCallContext(env, "tc1", "steady", Map.of());
        ctx.markExecuted("正常结果", null);
        hook.afterTool(ctx);

        assertThat(budget.samples("steady")).isEqualTo(1);
        assertThat(budget.breaching("steady")).isFalse(); // burn 0
    }

    @Test
    void beforeToolAlwaysContinues() {
        ErrorBudgetHook hook = new ErrorBudgetHook(sensitiveBudget());
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx =
                new DefaultToolCallContext(env, "tc1", "any", Map.of());
        assertThat(hook.beforeTool(ctx))
                .as("纯观察——预算只计不拦").isEqualTo(HookResult.CONTINUE);
        assertThat(hook.afterTool(null)).isEqualTo(HookResult.CONTINUE); // null 防御
    }
}
