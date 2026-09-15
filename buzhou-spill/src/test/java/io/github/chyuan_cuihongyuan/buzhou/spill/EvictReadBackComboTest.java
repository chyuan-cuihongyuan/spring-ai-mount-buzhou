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
 * spec 1114 / impl 865：evict×readRange 逐出复活组合——逐出→回读→再逐出
 * 链路双读面（EvictStats/ReadRangeStats）各自守恒保持、互不串账。纯测试轮。
 */
class EvictReadBackComboTest {

    @TempDir
    Path rootDir;

    private HandleLifecycleRegistry lifecycle;
    private SpillService service;
    private EvictHandleTool evict;
    private ReadRangeTool reader;

    @BeforeEach
    void setUp() {
        EvictHandleTool.resetForTest();
        ReadRangeTool.resetForTest();
        lifecycle = new HandleLifecycleRegistry();
        service = new SpillService(new DiskSpillStore(rootDir), 64, 3);
        evict = new EvictHandleTool(lifecycle);
        reader = new ReadRangeTool(service, null);
        reader.setHandleLifecycle(lifecycle);
    }

    private void spillViaService() {
        service.tryOffload("agent", "s1", "tc1", "big_tool", "y".repeat(600), 100);
    }

    @Test
    void evictThenReadBackThenReEvictChain() {
        spillViaService();
        // 逐出
        evict.call("{\"path\":\"spill://agent/s1/tc1\"}");
        // 回读（复活）
        String out = reader.call("{\"path\":\"spill://agent/s1/tc1\",\"mode\":\"bytes\"}");
        assertThat(out).contains("y");
        // 再逐出
        evict.call("{\"path\":\"spill://agent/s1/tc1\"}");

        EvictHandleTool.EvictStats es = EvictHandleTool.stats();
        assertThat(es.attempts()).isEqualTo(2);
        assertThat(es.evictions()).isEqualTo(2);

        ReadRangeTool.ReadRangeStats rs = ReadRangeTool.stats();
        assertThat(rs.reads()).isEqualTo(1);
    }

    @Test
    void noCrossContaminationBetweenStats() {
        evict.call("{\"path\":\"spill://agent/s1/tc1\"}");
        reader.call("{\"path\":\"spill://agent/s1/tc1\",\"mode\":\"bytes\"}");

        assertThat(EvictHandleTool.stats().evictions()).isEqualTo(1);
        assertThat(EvictHandleTool.stats().attempts()).isEqualTo(1);
        assertThat(ReadRangeTool.stats().calls()).isEqualTo(1);
        assertThat(ReadRangeTool.stats().reads()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependent() {
        evict.call("{\"path\":\"spill://agent/s1/tc1\"}");
        reader.call("{\"path\":\"spill://agent/s1/tc1\",\"mode\":\"bytes\"}");

        EvictHandleTool.resetForTest();
        assertThat(EvictHandleTool.stats().evictions()).isZero();
        assertThat(ReadRangeTool.stats().reads()).isEqualTo(1);

        ReadRangeTool.resetForTest();
        assertThat(ReadRangeTool.stats().calls()).isZero();
    }
}
