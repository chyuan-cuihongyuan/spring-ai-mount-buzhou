package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolFeedbackType;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 327 / impl-350：工具循环断路回归——达窗拦+三选一文案/持续干预直到
 * 换 key/换参解闩/换工具重计/会话隔离/null 防御/构造校验。
 */
class ToolLoopBreakerHookTest {

    private static DefaultToolCallContext ctx(String session, String tool,
            Map<String, Object> args) {
        HookEnvironment env = new HookEnvironment(session, "agent",
                new InMemorySessionStateStore());
        return new DefaultToolCallContext(env, "tc-" + System.nanoTime(), tool, args);
    }

    @Test
    void blocksAtWindowWithThreeWayOutGuidance() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(3);
        Map<String, Object> args = Map.of("q", "同参");
        assertThat(hook.beforeTool(ctx("s1", "search", args)))
                .isEqualTo(HookResult.CONTINUE); // run 1
        assertThat(hook.beforeTool(ctx("s1", "search", args)))
                .isEqualTo(HookResult.CONTINUE); // run 2
        HookResult result = hook.beforeTool(ctx("s1", "search", args)); // run 3 = 窗
        assertThat(result).isInstanceOf(HookResult.Block.class);
        String reason = ((HookResult.Block) result).reason();
        assertThat(reason).startsWith("[工具循环]").contains("三选一");
        assertThat(ToolFeedbackType.isErrorFeedback(reason))
                .as("非 131/321 错误两档——不污染熔断/错误预算").isFalse();
    }

    @Test
    void keepsBlockingSameKeyUntilChanged() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(2);
        Map<String, Object> args = Map.of("id", 7);
        hook.beforeTool(ctx("s1", "fetch", args));
        assertThat(hook.beforeTool(ctx("s1", "fetch", args)))
                .isInstanceOf(HookResult.Block.class);
        assertThat(hook.beforeTool(ctx("s1", "fetch", args)))
                .as("持续干预——不拦就继续烧真配额（与 326 闩一次不同）")
                .isInstanceOf(HookResult.Block.class);
    }

    @Test
    void changedArgsOrToolRelatches() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(2);
        hook.beforeTool(ctx("s1", "search", Map.of("q", "a")));
        assertThat(hook.beforeTool(ctx("s1", "search", Map.of("q", "a"))))
                .isInstanceOf(HookResult.Block.class);
        assertThat(hook.beforeTool(ctx("s1", "search", Map.of("q", "b"))))
                .as("换参 = 新 run").isEqualTo(HookResult.CONTINUE);
        hook.beforeTool(ctx("s1", "search", Map.of("q", "b")));
        assertThat(hook.beforeTool(ctx("s1", "fetch", Map.of("q", "b"))))
                .as("换工具 = 新 run").isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void argsOrderDoesNotMatter() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(2);
        java.util.Map<String, Object> a = new java.util.LinkedHashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        java.util.Map<String, Object> b = new java.util.LinkedHashMap<>();
        b.put("y", 2);
        b.put("x", 1);
        hook.beforeTool(ctx("s1", "search", a));
        assertThat(hook.beforeTool(ctx("s1", "search", b)))
                .as("Map.hashCode 与顺序无关——同参不同序也算循环")
                .isInstanceOf(HookResult.Block.class);
    }

    @Test
    void sessionsAreIndependent() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(2);
        Map<String, Object> args = Map.of("q", "x");
        hook.beforeTool(ctx("s1", "search", args));
        assertThat(hook.beforeTool(ctx("s2", "search", args)))
                .as("s2 首条——run 1").isEqualTo(HookResult.CONTINUE);
        assertThat(hook.beforeTool(ctx("s1", "search", args)))
                .isInstanceOf(HookResult.Block.class);
        assertThat(hook.currentRun("s1", "search", args)).isEqualTo(2);
    }

    @Test
    void nullSafetyAndValidation() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(2);
        assertThat(hook.beforeTool(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.beforeTool(ctx("s1", "search", null)))
                .isEqualTo(HookResult.CONTINUE); // null args 按空表
        assertThatThrownBy(() -> new ToolLoopBreakerHook(1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
