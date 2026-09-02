package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 323 / impl-346：干跑拦截回归——拦入计划（id/名/args 快照）/全量 vs
 * 清单/停跑放行/运行时开关/有界 100 + dropped/afterTool 不动/非错误标记。
 */
class DryRunHookTest {

    private static final HookEnvironment ENV =
            new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    private static DefaultToolCallContext ctx(String id, String tool, Map<String, Object> args) {
        return new DefaultToolCallContext(ENV, id, tool, args);
    }

    @Test
    void blocksAndRecordsIntoPlan() {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        HookResult result = hook.beforeTool(ctx("tc1", "delete_user", Map.of("id", "u-9")));
        assertThat(result).isInstanceOf(HookResult.Block.class);
        String reason = ((HookResult.Block) result).reason();
        assertThat(reason).startsWith("[干跑拦截]");
        assertThat(ToolFeedbackType.isErrorFeedback(reason))
                .as("非错误标记——干跑不是失败（熔断/预算不污染）").isFalse();
        assertThat(hook.plan()).hasSize(1);
        DryRunHook.PlannedCall call = hook.plan().get(0);
        assertThat(call.toolCallId()).isEqualTo("tc1");
        assertThat(call.toolName()).isEqualTo("delete_user");
        assertThat(call.arguments()).isEqualTo(Map.of("id", "u-9"));
    }

    @Test
    void emptyListBlocksAllTools() {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        assertThat(hook.beforeTool(ctx("a", "read", Map.of())))
                .isInstanceOf(HookResult.Block.class);
        assertThat(hook.beforeTool(ctx("b", "write", Map.of())))
                .isInstanceOf(HookResult.Block.class);
        assertThat(hook.plan()).hasSize(2); // 纯演练——全量拦
    }

    @Test
    void includeListLetsOthersRun() {
        DryRunHook hook = new DryRunHook(Set.of("delete_user"), true);
        assertThat(hook.beforeTool(ctx("a", "search", Map.of())))
                .isEqualTo(HookResult.CONTINUE); // 清单外照常真跑
        assertThat(hook.beforeTool(ctx("b", "delete_user", Map.of())))
                .isInstanceOf(HookResult.Block.class);
        assertThat(hook.plan()).hasSize(1);
    }

    @Test
    void runtimeToggleStopsInterception() {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        assertThat(hook.beforeTool(ctx("a", "write", Map.of())))
                .isInstanceOf(HookResult.Block.class);
        hook.setEnabled(false); // 计划过目——停干跑真执行
        assertThat(hook.beforeTool(ctx("b", "write", Map.of())))
                .isEqualTo(HookResult.CONTINUE);
        assertThat(hook.plan()).hasSize(1); // 停跑后不再记
    }

    @Test
    void planIsBoundedWithDroppedCount() {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        for (int i = 0; i < 120; i++) {
            hook.beforeTool(ctx("tc" + i, "write", Map.of()));
        }
        assertThat(hook.plan()).hasSize(100); // 有界
        assertThat(hook.droppedCount()).isEqualTo(20); // 诚实截断
        hook.clearPlan();
        assertThat(hook.plan()).isEmpty();
        assertThat(hook.droppedCount()).isZero();
    }

    @Test
    void disabledHookPassesEverything() {
        DryRunHook hook = new DryRunHook(Set.of(), false);
        assertThat(hook.beforeTool(ctx("a", "any", Map.of())))
                .isEqualTo(HookResult.CONTINUE);
        assertThat(hook.plan()).isEmpty();
    }

    @Test
    void afterToolNeverTouchesResult() {
        DryRunHook hook = new DryRunHook(Set.of(), true);
        DefaultToolCallContext executed = ctx("a", "search", Map.of());
        executed.markExecuted("真实结果", null);
        assertThat(hook.afterTool(executed)).isEqualTo(HookResult.CONTINUE);
        assertThat(executed.result()).isEqualTo("真实结果");
    }
}
