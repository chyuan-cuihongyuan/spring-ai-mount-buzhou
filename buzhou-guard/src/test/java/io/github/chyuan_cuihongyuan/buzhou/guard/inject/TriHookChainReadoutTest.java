package io.github.chyuan_cuihongyuan.buzhou.guard.inject;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.moderation.ContentModerationHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1094 / impl 846：guard 三 hook 链顺序协作（SecretScan 40 脱敏 →
 * Spotlight 80 包裹 → Moderation 210 检查）——双 stats 各自守恒保持、
 * 链路语义计数一致。纯测试轮。
 */
class TriHookChainReadoutTest {

    private static final char MARK = '\u2063';

    private HookEnvironment env;
    private SpotlightHook spotlight;
    private ContentModerationHook moderation;

    @BeforeEach
    void setUp() {
        SpotlightHook.resetForTest();
        ContentModerationHook.resetForTest();
        env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        spotlight = new SpotlightHook("ab12cd34", MARK, 1);
        moderation = new ContentModerationHook(List.of("competitor-x"),
                ContentModerationHook.Action.MASK);
    }

    @Test
    void chainSemanticsKeepReadoutsConsistent() {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ctx.markExecuted("外部输出", null);
        // 链：先包裹（80）后检查（210）
        spotlight.afterTool(ctx);
        moderation.afterTool(ctx);

        SpotlightHook.SpotlightStats ss = SpotlightHook.stats();
        assertThat(ss.invocations()).isEqualTo(1);
        assertThat(ss.wrapped()).isEqualTo(1);

        ContentModerationHook.ModerationStats ms = ContentModerationHook.stats();
        assertThat(ms.invocations()).isEqualTo(1);
        assertThat(ms.cleanSkips()).isEqualTo(1); // 包裹文本无词表命中

        // 双 stats 各自守恒
        assertThat(ss.invocations()).isEqualTo(ss.wrapped() + ss.alreadyWrappedSkips()
                + ss.noticeSkips() + ss.errorSkips());
        assertThat(ms.invocations()).isEqualTo(ms.blocked() + ms.masked()
                + ms.cleanSkips() + ms.nullSkips());
    }

    @Test
    void moderationHitsInsideWrappedTextCountMasked() {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ctx.markExecuted("提到 competitor-x 的价格", null);
        spotlight.afterTool(ctx); // 包裹
        moderation.afterTool(ctx); // 词表命中（脱敏后包裹文本仍含原文子串）→ masked

        ContentModerationHook.ModerationStats ms = ContentModerationHook.stats();
        assertThat(ms.masked()).isEqualTo(1);
        assertThat(ms.cleanSkips()).isZero();
    }

    @Test
    void resetsAreIndependent() {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ctx.markExecuted("外部输出", null);
        spotlight.afterTool(ctx);
        moderation.afterTool(ctx);

        SpotlightHook.resetForTest();
        assertThat(SpotlightHook.stats().invocations()).isZero();
        assertThat(ContentModerationHook.stats().invocations()).isEqualTo(1);

        ContentModerationHook.resetForTest();
        assertThat(ContentModerationHook.stats().invocations()).isZero();
    }
}
