package io.github.chyuan_cuihongyuan.buzhou.guard.inject;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1064 / impl 816：读侧 Spotlighting 包裹判定读面——包裹（wrapped）、
 * 幂等/告示/error 三跳过桶、守恒恒等式、resetForTest 归零。
 * 骨架同 InjectionDefenseUnitTest（HookEnvironment + DefaultToolCallContext）。
 */
class SpotlightStatsTest {

    private static final char MARK = '\u2063';

    private HookEnvironment env;
    private SpotlightHook hook;

    @BeforeEach
    void setUp() {
        SpotlightHook.resetForTest();
        env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        hook = new SpotlightHook("ab12cd34", MARK, 1);
    }

    @Test
    void wrappedAndIdempotentSkipCountTheirBuckets() {
        DefaultToolCallContext first = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        first.markExecuted("正常外部输出", null);
        hook.afterTool(first);
        assertThat(String.valueOf(first.result())).contains("<<<BUZHOU-DATA-ab12cd34-BEGIN>>>");

        DefaultToolCallContext again = new DefaultToolCallContext(env, "tc2", "fetch", Map.of());
        again.markExecuted(String.valueOf(first.result()), null); // 已包裹内容再入
        hook.afterTool(again);

        SpotlightHook.SpotlightStats stats = SpotlightHook.stats();
        assertThat(stats.invocations()).isEqualTo(2);
        assertThat(stats.wrapped()).isEqualTo(1);
        assertThat(stats.alreadyWrappedSkips()).isEqualTo(1);
    }

    @Test
    void noticeSkipCountsItsBucket() {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ctx.markExecuted(CanaryGuardHook.INTERCEPT_NOTICE + "正文", null);
        hook.afterTool(ctx);

        SpotlightHook.SpotlightStats stats = SpotlightHook.stats();
        assertThat(stats.noticeSkips()).isEqualTo(1);
        assertThat(stats.wrapped()).isZero();
    }

    @Test
    void errorAndNullResultCountErrorBucket() {
        DefaultToolCallContext errored = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        errored.markExecuted(null, new RuntimeException("boom"));
        hook.afterTool(errored);

        DefaultToolCallContext nullResult = new DefaultToolCallContext(env, "tc2", "fetch", Map.of());
        hook.afterTool(nullResult);

        assertThat(SpotlightHook.stats().errorSkips()).isEqualTo(2);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        DefaultToolCallContext ok = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ok.markExecuted("外部数据", null);
        hook.afterTool(ok);
        DefaultToolCallContext idem = new DefaultToolCallContext(env, "tc2", "fetch", Map.of());
        idem.markExecuted(String.valueOf(ok.result()), null);
        hook.afterTool(idem);
        DefaultToolCallContext notice = new DefaultToolCallContext(env, "tc3", "fetch", Map.of());
        notice.markExecuted(CanaryGuardHook.INTERCEPT_NOTICE + "正文", null);
        hook.afterTool(notice);
        DefaultToolCallContext err = new DefaultToolCallContext(env, "tc4", "fetch", Map.of());
        err.markExecuted(null, new RuntimeException("x"));
        hook.afterTool(err);

        SpotlightHook.SpotlightStats stats = SpotlightHook.stats();
        assertThat(stats.invocations()).isEqualTo(4);
        assertThat(stats.invocations())
                .isEqualTo(stats.wrapped() + stats.alreadyWrappedSkips()
                        + stats.noticeSkips() + stats.errorSkips());
        assertThat(stats.wrapped()).isEqualTo(1);
        assertThat(stats.alreadyWrappedSkips()).isEqualTo(1);
        assertThat(stats.noticeSkips()).isEqualTo(1);
        assertThat(stats.errorSkips()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ctx.markExecuted("数据", null);
        hook.afterTool(ctx);
        assertThat(SpotlightHook.stats().invocations()).isEqualTo(1);

        SpotlightHook.resetForTest();

        SpotlightHook.SpotlightStats stats = SpotlightHook.stats();
        assertThat(stats.invocations()).isZero();
        assertThat(stats.wrapped()).isZero();
    }
}
