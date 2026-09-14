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
 * spec 1089 / impl 841：spill 域 offload+evict 生命周期组合——溢出→逐出链路
 * 双读面（SpillOffloadStats/EvictHandleStats）各自守恒保持、互不串账。
 * 纯测试轮第七弹。
 */
class SpillLifecycleReadoutTest {

    @TempDir
    Path rootDir;

    @BeforeEach
    void reset() {
        SpillOffloadHook.resetForTest();
        EvictHandleTool.resetForTest();
    }

    @Test
    void offloadThenEvictKeepsBothReadoutsConsistent() {
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillService service = new SpillService(new DiskSpillStore(rootDir), 64, 3);
        DiskSpillStore store = new DiskSpillStore(rootDir);
        SpillOffloadHook offload = new SpillOffloadHook(service, registry,
                uri -> store.dataPathOf(uri), 100, Map.of());
        EvictHandleTool evict = new EvictHandleTool(new HandleLifecycleRegistry());

        // 溢出（超阈值）→ 溢出桶
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "big_tool", Map.of());
        ctx.markExecuted("x".repeat(600), null);
        offload.afterTool(ctx);
        // 逐出该句柄
        evict.call("{\"path\":\"spill://agent/s1/tc1\"}");

        SpillOffloadHook.SpillOffloadStats os = SpillOffloadHook.stats();
        assertThat(os.invocations()).isEqualTo(os.durableSkips() + os.errorSkips()
                + os.cleanInline() + os.offloaded() + os.refrains());
        assertThat(os.offloaded()).isEqualTo(1);

        EvictHandleTool.EvictStats es = EvictHandleTool.stats();
        assertThat(es.invocations()).isEqualTo(es.evictions()
                + es.badPathRejects() + es.parseRejects());
        assertThat(es.evictions()).isEqualTo(1);
    }

    @Test
    void noCrossContaminationBetweenHooks() {
        EvictHandleTool evict = new EvictHandleTool(new HandleLifecycleRegistry());
        evict.call("{\"path\":\"spill://agent/s1/tc9\"}");

        assertThat(EvictHandleTool.stats().evictions()).isEqualTo(1);
        assertThat(SpillOffloadHook.stats().invocations()).isZero();
    }

    @Test
    void resetsAreIndependent() {
        SpillOffloadHook.resetForTest();
        assertThat(SpillOffloadHook.stats().invocations()).isZero();

        EvictHandleTool.resetForTest();
        assertThat(EvictHandleTool.stats().evictions()).isZero();
    }
}
