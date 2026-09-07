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
 * spec 325 / impl-348：紧急停用回归——停用拦（非错误标记）/集外放行/
 * 运行时增删清/视图/null 防御。
 */
class ToolKillSwitchHookTest {

    private static final HookEnvironment ENV =
            new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    private static DefaultToolCallContext ctx(String tool) {
        return new DefaultToolCallContext(ENV, "tc-" + tool, tool, Map.of());
    }

    @Test
    void disabledToolBlockedWithNonErrorMarker() {
        ToolKillSwitchHook hook = new ToolKillSwitchHook();
        hook.disableTools(Set.of("bad_tool"));
        HookResult result = hook.beforeTool(ctx("bad_tool"));
        assertThat(result).isInstanceOf(HookResult.Block.class);
        String reason = ((HookResult.Block) result).reason();
        assertThat(reason).startsWith("[工具已停用]");
        assertThat(ToolFeedbackType.isErrorFeedback(reason))
                .as("人为停用≠失败——熔断/错误预算零污染").isFalse();
        assertThat(hook.beforeTool(ctx("good_tool")))
                .as("集外直通").isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void runtimeButtonsDisableEnableClear() {
        ToolKillSwitchHook hook = new ToolKillSwitchHook();
        assertThat(hook.beforeTool(ctx("any"))).isEqualTo(HookResult.CONTINUE); // 空集直通
        hook.disableTools(Set.of("a", "b"));
        assertThat(hook.disabled()).containsExactlyInAnyOrder("a", "b");
        hook.enableTools(Set.of("a"));
        assertThat(hook.disabled()).containsExactly("b");
        assertThat(hook.beforeTool(ctx("a"))).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.beforeTool(ctx("b"))).isInstanceOf(HookResult.Block.class);
        hook.clearAll();
        assertThat(hook.disabled()).isEmpty();
        assertThat(hook.beforeTool(ctx("b"))).isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void replaceDisabledOverwritesForNewSourceOfTruth() {
        ToolKillSwitchHook hook = new ToolKillSwitchHook();
        hook.disableTools(Set.of("runtime-emergency"));
        hook.replaceDisabled(Set.of("yml-a", "yml-b")); // 刷新事件：yml 整体覆盖
        assertThat(hook.disabled()).containsExactlyInAnyOrder("yml-a", "yml-b");
    }

    @Test
    void nullContextAndNullSetsAreSafe() {
        ToolKillSwitchHook hook = new ToolKillSwitchHook();
        assertThat(hook.beforeTool(null)).isEqualTo(HookResult.CONTINUE);
        hook.disableTools(null);
        hook.enableTools(null);
        hook.replaceDisabled(null);
        assertThat(hook.disabled()).isEmpty();
    }
}
