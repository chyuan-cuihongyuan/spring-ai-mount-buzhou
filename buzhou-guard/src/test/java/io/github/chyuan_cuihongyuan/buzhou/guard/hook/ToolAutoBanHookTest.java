package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 800 / T1102：工具自动封禁回归——达阈值封禁 / 滑窗滑出 /
 * 成功不重置 / 封禁到期解除 / 会话隔离 / 未监视零行为 / 快照读数 / 参数 fail-fast。
 */
class ToolAutoBanHookTest {

    /** 可拨动的测试时钟。 */
    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-09-13T00:00:00Z");

        void advanceSeconds(long s) {
            now = now.plusSeconds(s);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();

    private ToolAutoBanHook hook(Set<String> watch, int max, long window, long ban) {
        return new ToolAutoBanHook(watch, max, window, ban, clock);
    }

    private DefaultToolCallContext ctx(String session, String tool) {
        return new DefaultToolCallContext(
                new HookEnvironment(session, "agent", new InMemorySessionStateStore()), "tc-1", tool, Map.of());
    }

    private void fail(ToolAutoBanHook hook, String session, String tool) {
        DefaultToolCallContext c = ctx(session, tool);
        c.markExecuted(null, new RuntimeException("boom"));
        hook.afterTool(c);
    }

    private void ok(ToolAutoBanHook hook, String session, String tool) {
        DefaultToolCallContext c = ctx(session, tool);
        c.markExecuted("fine", null);
        hook.afterTool(c);
    }

    @Test
    void reachesThresholdBansThenExpires() {
        ToolAutoBanHook hook = hook(Set.of("deploy"), 3, 60, 120);

        fail(hook, "s1", "deploy");
        fail(hook, "s1", "deploy");
        assertThat(hook.beforeTool(ctx("s1", "deploy"))).isEqualTo(HookResult.CONTINUE);

        fail(hook, "s1", "deploy"); // 第 3 次失败 → 封禁
        HookResult verdict = hook.beforeTool(ctx("s1", "deploy"));
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) verdict).reason())
                .contains("deploy").contains("3 次").contains("s");

        // 封禁期内失败不再累计
        long bansBefore = hook.snapshot().totalBans();
        fail(hook, "s1", "deploy");
        assertThat(hook.snapshot().totalBans()).isEqualTo(bansBefore);

        // 到期自动解除
        clock.advanceSeconds(121);
        assertThat(hook.beforeTool(ctx("s1", "deploy"))).isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void windowSlidesOldFailuresOut() {
        ToolAutoBanHook hook = hook(Set.of("deploy"), 2, 60, 300);

        fail(hook, "s1", "deploy");
        clock.advanceSeconds(61); // 第一次失败滑出窗口
        fail(hook, "s1", "deploy");
        assertThat(hook.beforeTool(ctx("s1", "deploy"))).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.snapshot().totalBans()).isZero();
    }

    @Test
    void successDoesNotResetWindow() {
        ToolAutoBanHook hook = hook(Set.of("deploy"), 2, 60, 300);

        fail(hook, "s1", "deploy");
        ok(hook, "s1", "deploy");
        fail(hook, "s1", "deploy"); // 成功不重置——2 次失败仍达阈值
        HookResult verdict = hook.beforeTool(ctx("s1", "deploy"));
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
    }

    @Test
    void sessionIsolation() {
        ToolAutoBanHook hook = hook(Set.of("deploy"), 1, 60, 300);

        fail(hook, "s1", "deploy");
        assertThat(hook.beforeTool(ctx("s1", "deploy"))).isInstanceOf(HookResult.Block.class);
        assertThat(hook.beforeTool(ctx("s2", "deploy"))).isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void unwatchedToolsAndEmptyWatchAreNoOps() {
        assertThat(hook(Set.of(), 1, 60, 300).beforeTool(ctx("s1", "x"))).isEqualTo(HookResult.CONTINUE);
        ToolAutoBanHook hook = hook(Set.of("watched"), 1, 60, 300);
        assertThat(hook.beforeTool(ctx("s1", "other"))).isEqualTo(HookResult.CONTINUE);
        fail(hook, "s1", "other");
        assertThat(hook.snapshot().totalViolations()).isZero();
        assertThat(hook.beforeTool(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.afterTool(null)).isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void snapshotReadsTotalsAndActiveBans() {
        ToolAutoBanHook hook = hook(Set.of("a", "b"), 1, 60, 100);
        assertThat(hook.snapshot().active()).isEmpty();

        fail(hook, "s1", "a");
        fail(hook, "s2", "b");

        ToolAutoBanHook.BanSnapshot snap = hook.snapshot();
        assertThat(snap.totalViolations()).isEqualTo(2);
        assertThat(snap.totalBans()).isEqualTo(2);
        assertThat(snap.active()).hasSize(2);
        assertThat(snap.active()).allSatisfy(b -> assertThat(b.remainingSeconds()).isPositive());
        assertThat(snap.active().stream().map(ToolAutoBanHook.ActiveBan::toolName))
                .containsExactlyInAnyOrder("a", "b");
        assertThat(snap.truncated()).isFalse();

        clock.advanceSeconds(101);
        assertThat(hook.snapshot().active()).isEmpty(); // 过期封禁不再列为 active
    }

    @Test
    void trackedKeysAreCapped() {
        ToolAutoBanHook hook = hook(Set.of("t"), 1, 60, 100);
        for (int i = 0; i < ToolAutoBanHook.MAX_TRACKED_KEYS + 10; i++) {
            fail(hook, "session-" + i, "t");
        }
        ToolAutoBanHook.BanSnapshot snap = hook.snapshot();
        assertThat(snap.truncated()).isTrue();
        assertThat(snap.active()).hasSize(ToolAutoBanHook.MAX_TRACKED_KEYS);
    }

    @Test
    void invalidArgumentsFailFast() {
        assertThatThrownBy(() -> hook(Set.of("t"), 0, 60, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hook(Set.of("t"), 1, 0, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hook(Set.of("t"), 1, 60, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolAutoBanHook(Set.of("t"), 1, 60, 100, null))
                .isInstanceOf(NullPointerException.class);
    }
}
