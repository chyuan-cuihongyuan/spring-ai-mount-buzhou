package io.github.chyuan_cuihongyuan.buzhou.guard.moderation;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 515 / T781–T782：内容安全词表过滤——工具缝 BLOCK 告示替换/MASK
 * 打码、大小写不敏感、无命中透传、输入缝 BLOCK、yml terms 装配/缺省不装。
 */
class ContentModerationHookTest {

    private static DefaultToolCallContext toolCtx(HookEnvironment env, String result) {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "tool_x", Map.of());
        ctx.markExecuted(result, null);
        return ctx;
    }

    @Test
    void toolOutputBlockedByNoticeWhenActionBlock() {
        HookEnvironment env = new HookEnvironment("app", "agent", new InMemorySessionStateStore());
        ContentModerationHook hook = new ContentModerationHook(
                List.of("competitor-x", "违禁词"), ContentModerationHook.Action.BLOCK);
        DefaultToolCallContext ctx = toolCtx(env, "这里提到 Competitor-X 的价格");
        hook.afterTool(ctx);
        assertThat(String.valueOf(ctx.result()))
                .isEqualTo(ContentModerationHook.TOOL_BLOCK_NOTICE);
    }

    @Test
    void toolOutputMaskedWhenActionMask() {
        HookEnvironment env = new HookEnvironment("app", "agent", new InMemorySessionStateStore());
        ContentModerationHook hook = new ContentModerationHook(
                List.of("secret-plan"), ContentModerationHook.Action.MASK);
        DefaultToolCallContext ctx = toolCtx(env, "the SECRET-PLAN is leaked here");
        hook.afterTool(ctx);
        assertThat(String.valueOf(ctx.result())).isEqualTo("the [已屏蔽] is leaked here");
    }

    @Test
    void cleanOutputPassesThroughUntouched() {
        HookEnvironment env = new HookEnvironment("app", "agent", new InMemorySessionStateStore());
        ContentModerationHook hook = new ContentModerationHook(
                List.of("competitor-x"), ContentModerationHook.Action.BLOCK);
        DefaultToolCallContext ctx = toolCtx(env, "完全干净的结果");
        hook.afterTool(ctx);
        assertThat(String.valueOf(ctx.result())).isEqualTo("完全干净的结果");
    }

    @Test
    void inputSeamBlocksWhenActionBlock() {
        ContentModerationHook hook = new ContentModerationHook(
                List.of("违禁词"), ContentModerationHook.Action.BLOCK);
        var ctx = new TurnCtxStub("包含违禁词的输入");
        HookResult result = hook.beforeTurn(ctx);
        assertThat(result).isInstanceOf(HookResult.Block.class);
    }

    @Test
    void ymlAssemblyTermsRequired() {
        var stores = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        GuardModule enabled = GuardModule.fromYml(stores, Map.of("moderation", Map.of(
                "terms", List.of("违禁词"), "action", "mask")));
        assertThat(enabled.configure().hooks())
                .anySatisfy(h -> assertThat(h)
                        .isInstanceOf(ContentModerationHook.class));
        GuardModule absent = GuardModule.fromYml(stores, Map.of());
        assertThat(absent.configure().hooks())
                .noneMatch(h -> h instanceof ContentModerationHook);
    }

    /** 最小 TurnContext 桩。 */
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
