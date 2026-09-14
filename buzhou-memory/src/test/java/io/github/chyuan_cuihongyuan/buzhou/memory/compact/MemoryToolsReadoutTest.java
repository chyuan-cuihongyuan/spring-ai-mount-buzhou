package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.episodic.EpisodeLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.CompactNowTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1087 / impl 839：memory 域双工具组合——compact_now 与 EpisodeLedger
 * 交叉调用后双读面各自守恒保持、互不串账、reset 独立隔离。纯测试轮。
 */
class MemoryToolsReadoutTest {

    private SessionStateStore stateStore;

    @BeforeEach
    void setUp() {
        CompactNowTool.resetForTest();
        EpisodeLedger.resetForTest();
        stateStore = new InMemorySessionStateStore();
    }

    @Test
    void crossCallsKeepBothReadoutsConsistent() {
        CompactNowTool compact = new CompactNowTool(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore(),
                new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore()),
                null, null, 2);
        compact.call("{}", null); // unboundRejects

        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        ledger.record("s1", "任务", "", "success");
        ledger.recallExamples("s1", "任务", 5);

        CompactNowTool.CompactNowStats cs = CompactNowTool.stats();
        assertThat(cs.calls()).isEqualTo(cs.successes() + cs.skippeds()
                + cs.failures() + cs.unboundRejects());
        assertThat(cs.unboundRejects()).isEqualTo(1);

        EpisodeLedger.EpisodicMemoryStats es = EpisodeLedger.stats();
        assertThat(es.recordCalls()).isEqualTo(es.recorded() + es.recordDropped()
                + es.recordFailures());
        assertThat(es.recallCalls()).isEqualTo(es.recallHits() + es.recallEmpties()
                + es.recallDropped());
    }

    @Test
    void noCrossContaminationBetweenReadouts() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f});
        ledger.record("s1", "任务", "", "success");

        assertThat(EpisodeLedger.stats().recordCalls()).isEqualTo(1);
        assertThat(CompactNowTool.stats().calls()).isZero(); // compact 未动
    }

    @Test
    void resetsAreIndependent() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f});
        ledger.record("s1", "任务", "", "success");

        EpisodeLedger.resetForTest();
        assertThat(EpisodeLedger.stats().recordCalls()).isZero();

        CompactNowTool.resetForTest();
        assertThat(CompactNowTool.stats().calls()).isZero();
        assertThat(CompactNowTool.stats().successes()).isZero();
    }
}
