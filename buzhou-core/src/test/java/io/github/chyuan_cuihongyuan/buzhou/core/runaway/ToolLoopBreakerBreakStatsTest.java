package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1433 / T2170：工具循环打断分布——per-tool 打断计数、总次数与最长
 * run 水位、换参重置新 run、放行路径零记账、reset 清榜不清会话状态。
 */
class ToolLoopBreakerBreakStatsTest {

    private static io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext call(
            String sessionId, String tool, Map<String, Object> args) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext() {
            @Override
            public String toolCallId() {
                return "tc";
            }

            @Override
            public String toolName() {
                return tool;
            }

            @Override
            public Map<String, Object> arguments() {
                return args;
            }

            @Override
            public Object result() {
                return null;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public void replaceArguments(Map<String, Object> newArguments) {
            }

            @Override
            public void replaceResult(Object newResult) {
            }

            @Override
            public String sessionId() {
                return sessionId;
            }

            @Override
            public String agentName() {
                return "ag";
            }

            @Override
            public int turn() {
                return 1;
            }

            @Override
            public SessionStateHandle state() {
                return null;
            }

            @Override
            public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) {
            }
        };
    }

    @Test
    void breaksCountedPerToolWithTotalAndWatermark() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(3);
        Map<String, Object> args = Map.of("q", 1);
        // 同工具同参 5 连发：run=3 触发打断（block），其后均打断
        for (int i = 0; i < 5; i++) {
            hook.beforeTool(call("s", "search", args));
        }
        assertThat(hook.brokenTotal()).isEqualTo(3); // run 3/4/5 三次打断
        assertThat(hook.maxRunObserved()).isEqualTo(5);
        assertThat(hook.brokenByToolSnapshot().get("search")).isEqualTo(3);
    }

    @Test
    void argChangeResetsRunAndCountsBothTools() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(3);
        Map<String, Object> q1 = Map.of("q", 1);
        Map<String, Object> q2 = Map.of("q", 2);
        hook.beforeTool(call("s", "search", q1));
        hook.beforeTool(call("s", "search", q1));
        hook.beforeTool(call("s", "search", q1)); // run=3 → 打断
        hook.beforeTool(call("s", "search", q2)); // 换参——新 run
        hook.beforeTool(call("s", "other", q2));  // 换工具——新 run
        hook.beforeTool(call("s", "other", q2));
        hook.beforeTool(call("s", "other", q2)); // run=3 → 打断
        assertThat(hook.brokenTotal()).isEqualTo(2);
        var board = hook.brokenByToolSnapshot();
        assertThat(board.get("search")).isEqualTo(1);
        assertThat(board.get("other")).isEqualTo(1);
        assertThat(hook.maxRunObserved()).isEqualTo(3);
    }

    @Test
    void resetClearsBoardNotSessionState() {
        ToolLoopBreakerHook hook = new ToolLoopBreakerHook(3);
        Map<String, Object> args = Map.of("q", 1);
        for (int i = 0; i < 4; i++) {
            hook.beforeTool(call("s", "search", args));
        }
        assertThat(hook.brokenTotal()).isEqualTo(2);
        hook.resetBrokenForTest();
        assertThat(hook.brokenTotal()).isZero();
        // 会话 run 状态不动：仍在打断裂缝上（run 已 4，再发一次 run=5 仍打断）
        HookResult result = hook.beforeTool(call("s", "search", args));
        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(hook.brokenTotal()).isEqualTo(1);
        assertThat(hook.maxRunObserved()).isEqualTo(5);
    }
}
