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
 * spec 1092 / impl 844：Spotlight（ORDER 80 包裹）× Moderation（ORDER 210 检查）
 * 顺序协作——同 ctx 连续两 hook 调用后双读面计数一致、各自守恒保持。纯测试轮。
 */
class SpotlightModerationComboTest {

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

    private DefaultToolCallContext ctx(String result) {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ctx.markExecuted(result, null);
        return ctx;
    }

    @Test
    void sequentialHooksCountBothReadouts() {
        DefaultToolCallContext ctx = ctx("普通外部输出");
        spotlight.afterTool(ctx);   // ORDER 80：包裹
        moderation.afterTool(ctx);  // ORDER 210：词表检查（包裹文本无命中 → cleanSkips）

        SpotlightHook.SpotlightStats ss = SpotlightHook.stats();
        assertThat(ss.invocations()).isEqualTo(1);
        assertThat(ss.wrapped()).isEqualTo(1);

        ContentModerationHook.ModerationStats ms = ContentModerationHook.stats();
        assertThat(ms.invocations()).isEqualTo(1);
        assertThat(ms.cleanSkips()).isEqualTo(1);
    }

    @Test
    void bothReadoutsHoldTheirConservation() {
        DefaultToolCallContext ctx = ctx("外部数据");
        spotlight.afterTool(ctx);
        moderation.afterTool(ctx);

        SpotlightHook.SpotlightStats ss = SpotlightHook.stats();
        assertThat(ss.invocations())
                .isEqualTo(ss.wrapped() + ss.alreadyWrappedSkips()
                        + ss.noticeSkips() + ss.errorSkips());
        ContentModerationHook.ModerationStats ms = ContentModerationHook.stats();
        assertThat(ms.invocations())
                .isEqualTo(ms.blocked() + ms.masked() + ms.cleanSkips() + ms.nullSkips());
    }

    @Test
    void resetsAreIndependent() {
        DefaultToolCallContext ctx = ctx("数据");
        spotlight.afterTool(ctx);
        moderation.afterTool(ctx);

        SpotlightHook.resetForTest();
        assertThat(SpotlightHook.stats().invocations()).isZero();
        assertThat(ContentModerationHook.stats().invocations()).isEqualTo(1);

        ContentModerationHook.resetForTest();
        assertThat(ContentModerationHook.stats().invocations()).isZero();
    }
}
