package io.github.chyuan_cuihongyuan.buzhou.spill;

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
 * spec 1093 / impl 845：offload→readBack 双轴闭环组合——溢出落盘（offloaded）
 * → read_range 回读（reads）闭环计数一致，双读面各自守恒保持。纯测试轮。
 */
class OffloadReadBackComboTest {

    @TempDir
    Path rootDir;

    @BeforeEach
    void reset() {
        SpillOffloadHook.resetForTest();
        ReadRangeTool.resetForTest();
    }

    @Test
    void offloadThenReadBackCountsBothSides() {
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillService service = new SpillService(new DiskSpillStore(rootDir), 64, 3);
        DiskSpillStore store = new DiskSpillStore(rootDir);
        SpillOffloadHook offload = new SpillOffloadHook(service, registry,
                uri -> store.dataPathOf(uri), 100, Map.of());
        ReadRangeTool reader = new ReadRangeTool(service);

        // 溢出（超阈值）→ offloaded
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted("x".repeat(600), null);
        offload.afterTool(ctx);
        assertThat(SpillOffloadHook.stats().offloaded()).isEqualTo(1);

        // 回读该句柄 → reads
        String readOut = reader.call("{\"path\":\"spill://agent/s1/tc1\",\"mode\":\"bytes\"}");
        assertThat(readOut).contains("x");

        SpillOffloadHook.SpillOffloadStats os = SpillOffloadHook.stats();
        assertThat(os.invocations()).isEqualTo(os.durableSkips() + os.errorSkips()
                + os.cleanInline() + os.offloaded() + os.refrains());
        ReadRangeTool.ReadRangeStats rs = ReadRangeTool.stats();
        assertThat(rs.calls()).isEqualTo(rs.reads() + rs.truncatedReads()
                + rs.totalRejects());
        // 闭环对应：offloaded=1 且回读命中=1
        assertThat(os.offloaded()).isEqualTo(1);
        assertThat(rs.reads()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependent() {
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillService service = new SpillService(new DiskSpillStore(rootDir), 64, 3);
        DiskSpillStore store = new DiskSpillStore(rootDir);
        SpillOffloadHook offload = new SpillOffloadHook(service, registry,
                uri -> store.dataPathOf(uri), 100, Map.of());
        ReadRangeTool reader = new ReadRangeTool(service);

        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted("x".repeat(600), null);
        offload.afterTool(ctx);

        SpillOffloadHook.resetForTest();
        assertThat(SpillOffloadHook.stats().offloaded()).isZero();
        assertThat(ReadRangeTool.stats().calls()).isZero();
    }
}
