package io.github.chyuan_cuihongyuan.buzhou.guard.moderation;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1067 / impl 819：内容安全词表双缝判定读面——BLOCK/MASK 动作桶、
 * 无命中/null 两跳过桶、守恒恒等式、resetForTest 归零。
 * 骨架同 ContentModerationHookTest（toolCtx helper + TurnCtxStub）。
 */
class ModerationStatsTest {

    private HookEnvironment env;

    @BeforeEach
    void reset() {
        ContentModerationHook.resetForTest();
        env = new HookEnvironment("app", "agent", new InMemorySessionStateStore());
    }

    private static DefaultToolCallContext toolCtx(HookEnvironment env, String result) {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "tool_x", Map.of());
        ctx.markExecuted(result, null);
        return ctx;
    }

    @Test
    void blockActionCountsBlocked() {
        ContentModerationHook hook = new ContentModerationHook(
                List.of("competitor-x"), ContentModerationHook.Action.BLOCK);
        // 工具缝 BLOCK 是替换语义（CONTINUE + 告示文本），非 Block 返回
        hook.afterTool(toolCtx(env, "提到 Competitor-X 价格"));

        ContentModerationHook.ModerationStats stats = ContentModerationHook.stats();
        assertThat(stats.invocations()).isEqualTo(1);
        assertThat(stats.blocked()).isEqualTo(1);
        assertThat(stats.masked()).isZero();
    }

    @Test
    void maskActionCountsMasked() {
        ContentModerationHook hook = new ContentModerationHook(
                List.of("secret-plan"), ContentModerationHook.Action.MASK);
        hook.afterTool(toolCtx(env, "the SECRET-PLAN leaked"));

        assertThat(ContentModerationHook.stats().masked()).isEqualTo(1);
    }

    @Test
    void cleanOutputCountsCleanSkip() {
        ContentModerationHook hook = new ContentModerationHook(
                List.of("competitor-x"), ContentModerationHook.Action.BLOCK);
        hook.afterTool(toolCtx(env, "完全干净的结果"));

        assertThat(ContentModerationHook.stats().cleanSkips()).isEqualTo(1);
    }

    @Test
    void nullResultCountsNullSkip() {
        ContentModerationHook hook = new ContentModerationHook(
                List.of("competitor-x"), ContentModerationHook.Action.BLOCK);
        hook.afterTool(toolCtx(env, null));

        assertThat(ContentModerationHook.stats().nullSkips()).isEqualTo(1);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        ContentModerationHook blockHook = new ContentModerationHook(
                List.of("违禁词"), ContentModerationHook.Action.BLOCK);
        // 输入缝 BLOCK
        HookResult blockResult = blockHook.beforeTurn(new TurnCtxStub("含违禁词输入"));
        assertThat(blockResult).isInstanceOf(HookResult.Block.class);
        // 输入缝干净
        blockHook.beforeTurn(new TurnCtxStub("干净输入"));
        // 输入缝 null
        blockHook.beforeTurn(new TurnCtxStub(null));
        // 工具缝 MASK
        ContentModerationHook maskHook = new ContentModerationHook(
                List.of("违禁词"), ContentModerationHook.Action.MASK);
        maskHook.afterTool(toolCtx(env, "含违禁词结果"));

        ContentModerationHook.ModerationStats stats = ContentModerationHook.stats();
        assertThat(stats.invocations()).isEqualTo(4);
        assertThat(stats.invocations())
                .isEqualTo(stats.blocked() + stats.masked() + stats.cleanSkips()
                        + stats.nullSkips());
        assertThat(stats.blocked()).isEqualTo(1);
        assertThat(stats.masked()).isEqualTo(1);
        assertThat(stats.cleanSkips()).isEqualTo(1);
        assertThat(stats.nullSkips()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        ContentModerationHook hook = new ContentModerationHook(
                List.of("违禁词"), ContentModerationHook.Action.MASK);
        hook.afterTool(toolCtx(env, "含违禁词"));
        assertThat(ContentModerationHook.stats().invocations()).isEqualTo(1);

        ContentModerationHook.resetForTest();

        ContentModerationHook.ModerationStats stats = ContentModerationHook.stats();
        assertThat(stats.invocations()).isZero();
        assertThat(stats.masked()).isZero();
    }

    /** 最小 TurnContext 桩（同 ContentModerationHookTest）。 */
    private static final class TurnCtxStub implements io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext {
        private final String input;

        TurnCtxStub(String input) {
            this.input = input;
        }

        @Override public String sessionId() { return "s"; }
        @Override public String agentName() { return "a"; }
        @Override public int turn() { return 0; }
        @Override public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
            throw new UnsupportedOperationException();
        }
        @Override public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) { }
        @Override public String input() { return input; }
        @Override public String response() { return ""; }
        @Override public void replaceInput(String newInput) { }
        @Override public void replaceResponse(String newResponse) { }
    }
}
