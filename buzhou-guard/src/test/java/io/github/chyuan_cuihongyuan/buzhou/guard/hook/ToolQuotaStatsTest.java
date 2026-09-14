package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1068 / impl 820：工具配额消耗读面——放行（allowed）、超限拒绝（quotaBlocks）、
 * 未管辖豁免（unmanagedSkips）、三桶守恒恒等式、resetForTest 归零。
 * 骨架同 ToolQuotaHookTest（HookEnvironment + DefaultToolCallContext）。
 */
class ToolQuotaStatsTest {

    private HookEnvironment env;

    @BeforeEach
    void setUp() {
        ToolQuotaHook.resetForTest();
        env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    private HookResult invoke(ToolQuotaHook hook, String tool) {
        return hook.beforeTool(new DefaultToolCallContext(env, "tc", tool, Map.of()));
    }

    @Test
    void allowedCallsCountTheirBucket() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("expensive", 5));
        assertThat(invoke(hook, "expensive")).isEqualTo(HookResult.CONTINUE);
        assertThat(invoke(hook, "expensive")).isEqualTo(HookResult.CONTINUE);

        ToolQuotaHook.ToolQuotaStats stats = ToolQuotaHook.stats();
        assertThat(stats.calls()).isEqualTo(2);
        assertThat(stats.allowed()).isEqualTo(2);
        assertThat(stats.quotaBlocks()).isZero();
    }

    @Test
    void overQuotaCountsItsBucket() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("expensive", 1));
        assertThat(invoke(hook, "expensive")).isEqualTo(HookResult.CONTINUE);
        assertThat(invoke(hook, "expensive")).isInstanceOf(HookResult.Block.class);
        // 被拒不计消耗（既有语义）——重复拒只入 quotaBlocks 桶
        assertThat(invoke(hook, "expensive")).isInstanceOf(HookResult.Block.class);

        ToolQuotaHook.ToolQuotaStats stats = ToolQuotaHook.stats();
        assertThat(stats.calls()).isEqualTo(3);
        assertThat(stats.allowed()).isEqualTo(1);
        assertThat(stats.quotaBlocks()).isEqualTo(2);
    }

    @Test
    void unconfiguredToolCountsUnmanagedBucket() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("expensive", 5));
        assertThat(invoke(hook, "free-tool")).isEqualTo(HookResult.CONTINUE);

        assertThat(ToolQuotaHook.stats().unmanagedSkips()).isEqualTo(1);
        assertThat(ToolQuotaHook.stats().allowed()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("expensive", 1));
        invoke(hook, "expensive");           // allowed
        invoke(hook, "expensive");           // quotaBlocks
        invoke(hook, "free-tool");           // unmanaged

        ToolQuotaHook.ToolQuotaStats stats = ToolQuotaHook.stats();
        assertThat(stats.calls()).isEqualTo(3);
        assertThat(stats.calls())
                .isEqualTo(stats.allowed() + stats.quotaBlocks() + stats.unmanagedSkips());
        assertThat(stats.allowed()).isEqualTo(1);
        assertThat(stats.quotaBlocks()).isEqualTo(1);
        assertThat(stats.unmanagedSkips()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("t", 1));
        invoke(hook, "t");
        assertThat(ToolQuotaHook.stats().calls()).isEqualTo(1);

        ToolQuotaHook.resetForTest();

        ToolQuotaHook.ToolQuotaStats stats = ToolQuotaHook.stats();
        assertThat(stats.calls()).isZero();
        assertThat(stats.allowed()).isZero();
        assertThat(stats.quotaBlocks()).isZero();
    }
}
