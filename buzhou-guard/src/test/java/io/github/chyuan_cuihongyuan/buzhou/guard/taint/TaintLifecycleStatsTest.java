package io.github.chyuan_cuihongyuan.buzhou.guard.taint;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaintLifecycleStatsTest {

    private final SessionStateStore stateStore = new InMemorySessionStateStore();

    private DefaultToolCallContext ctx(String toolName) {
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        return new DefaultToolCallContext(env, "tc-1", toolName, Map.of());
    }

    private static DangerousToolConfig gateConfig() {
        return new DangerousToolConfig(true, null,
                List.of(new io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolEntry(
                        "delete_records", null, "需要人工确认", null)));
    }

    @Test
    void firstMarkThenReassertCountsDistinguished() {
        TaintTrackingHook tracking = new TaintTrackingHook(stateStore);

        DefaultToolCallContext first = ctx("fetch_page");
        first.markExecuted("输出A", null);
        tracking.afterTool(first);
        DefaultToolCallContext second = ctx("fetch_page");
        second.markExecuted("输出B", null);
        tracking.afterTool(second);

        TaintTrackingHook.TaintMarkStats stats = tracking.stats();
        assertThat(stats.marksApplied()).isEqualTo(2);
        assertThat(stats.firstMarks()).isEqualTo(1);
    }

    @Test
    void nullResultDoesNotMark() {
        TaintTrackingHook tracking = new TaintTrackingHook(stateStore);

        // 未执行 markExecuted 的上下文 result() 为 null——不参与打标
        tracking.afterTool(ctx("fetch_page"));

        assertThat(tracking.stats()).isEqualTo(new TaintTrackingHook.TaintMarkStats(0, 0));
    }

    @Test
    void gateFourBucketsAreCounted() {
        TaintTrackingHook tracking = new TaintTrackingHook(stateStore);
        TaintWriteGateHook gate = new TaintWriteGateHook(gateConfig(), stateStore);

        // ① 可信上下文放行
        HookResult trusted = gate.beforeTool(ctx("delete_records"));
        assertThat(trusted).isSameAs(HookResult.CONTINUE);
        // ② 打标后未批准 → 拦截
        DefaultToolCallContext read = ctx("fetch_page");
        read.markExecuted("网页内容", null);
        tracking.afterTool(read);
        HookResult gated = gate.beforeTool(ctx("delete_records"));
        assertThat(gated).isNotSameAs(HookResult.CONTINUE);
        // ③ 人工批准同 (tool, args) → 放行
        String authKey = io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint.ArgumentFingerprint
                .authKey("delete_records",
                        io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint.ArgumentFingerprint
                                .fingerprint(Map.of()));
        stateStore.put("s1", new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                authKey, "approved", "test", 1, null, java.time.Instant.now()));
        HookResult approved = gate.beforeTool(ctx("delete_records"));
        assertThat(approved).isSameAs(HookResult.CONTINUE);

        TaintWriteGateHook.GateStats stats = gate.stats();
        assertThat(stats.checkedWriteCalls()).isEqualTo(3);
        assertThat(stats.allowedTrusted()).isEqualTo(1);
        assertThat(stats.allowedApproved()).isEqualTo(1);
        assertThat(stats.blocked()).isEqualTo(1);
        assertThat(stats.allowedTrusted() + stats.allowedApproved() + stats.blocked())
                .isEqualTo(stats.checkedWriteCalls());
    }

    @Test
    void nonWriteToolIsNotCounted() {
        TaintWriteGateHook gate = new TaintWriteGateHook(gateConfig(), stateStore);

        assertThat(gate.beforeTool(ctx("read_only"))).isSameAs(HookResult.CONTINUE);
        assertThat(gate.stats().checkedWriteCalls()).isZero();
    }
}
