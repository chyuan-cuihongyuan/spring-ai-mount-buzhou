package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1077 / impl 829：Spill 溢出 hook 判定读面——溢出替换（offloaded）、
 * 阈值内内联（cleanInline）、error 跳过、durable 覆盖、守恒恒等式、归零。
 * 骨架同 SpillOffloadHookTest（DiskSpillStore + SpillOffloadHook 五参构造）。
 */
class SpillOffloadStatsTest {

    @TempDir
    Path rootDir;

    private static final int THRESHOLD = 100;

    @BeforeEach
    void reset() {
        SpillOffloadHook.resetForTest();
    }

    private SpillOffloadHook hook() {
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillService service = new SpillService(new DiskSpillStore(rootDir), 64, 3);
        DiskSpillStore store = new DiskSpillStore(rootDir);
        return new SpillOffloadHook(service, registry,
                uri -> store.dataPathOf(uri), THRESHOLD, Map.of());
    }

    private HookResult invoke(SpillOffloadHook hook, String tool, Object result) {
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", tool, Map.of());
        ctx.markExecuted(result, null);
        return hook.afterTool(ctx);
    }

    @Test
    void oversizedOutputCountsOffloaded() {
        SpillOffloadHook hook = hook();
        HookResult result = invoke(hook, "big_tool", "x".repeat(THRESHOLD + 500));
        assertThat(result).isEqualTo(HookResult.CONTINUE); // 溢出替换文本（CONTINUE 语义）

        SpillOffloadHook.SpillOffloadStats stats = SpillOffloadHook.stats();
        assertThat(stats.invocations()).isEqualTo(1);
        assertThat(stats.offloaded()).isEqualTo(1);
        assertThat(stats.cleanInline()).isZero();
    }

    @Test
    void inlineOutputCountsCleanInline() {
        SpillOffloadHook hook = hook();
        invoke(hook, "small_tool", "短输出");

        SpillOffloadHook.SpillOffloadStats stats = SpillOffloadHook.stats();
        assertThat(stats.cleanInline()).isEqualTo(1);
        assertThat(stats.offloaded()).isZero();
    }

    @Test
    void errorResultCountsErrorSkip() {
        hook();
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted(null, new RuntimeException("boom"));
        hook().afterTool(ctx);

        assertThat(SpillOffloadHook.stats().errorSkips()).isEqualTo(1);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        SpillOffloadHook hook = hook();
        invoke(hook, "big_tool", "y".repeat(THRESHOLD + 100)); // offloaded
        invoke(hook, "small_tool", "ok");                       // cleanInline
        invoke(hook, "small_tool", null);                       // errorSkips
        invoke(hook, "small_tool", "fine");                     // cleanInline

        SpillOffloadHook.SpillOffloadStats stats = SpillOffloadHook.stats();
        assertThat(stats.invocations()).isEqualTo(4);
        assertThat(stats.invocations())
                .isEqualTo(stats.durableSkips() + stats.errorSkips() + stats.cleanInline()
                        + stats.offloaded() + stats.refrains());
        assertThat(stats.offloaded()).isEqualTo(1);
        assertThat(stats.cleanInline()).isEqualTo(2);
        assertThat(stats.errorSkips()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        SpillOffloadHook hook = hook();
        invoke(hook, "small_tool", "ok");
        assertThat(SpillOffloadHook.stats().invocations()).isEqualTo(1);

        SpillOffloadHook.resetForTest();

        SpillOffloadHook.SpillOffloadStats stats = SpillOffloadHook.stats();
        assertThat(stats.invocations()).isZero();
        assertThat(stats.cleanInline()).isZero();
    }
}
