package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.episodic.EpisodeLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.CompactNowTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1107 / impl 859：memory 三读面大组合——compact_now、EpisodeLedger、
 * BiTemporalFactLedger 全交叉后各自守恒保持、互不串账、reset 独立。纯测试轮。
 */
class MemoryTripleReadoutTest {

    private SessionStateStore stateStore;

    @BeforeEach
    void setUp() {
        CompactNowTool.resetForTest();
        EpisodeLedger.resetForTest();
        BiTemporalFactLedger.resetForTest();
        stateStore = new InMemorySessionStateStore();
    }

    private CompactNowTool compact() {
        return new CompactNowTool(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore(),
                new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore()),
                null, null, 2);
    }

    @Test
    void tripleCrossCallsKeepAllReadoutsIndependent() {
        CompactNowTool compact = compact();
        EpisodeLedger episodes = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        BiTemporalFactLedger facts = new BiTemporalFactLedger(stateStore);

        compact.call("{}", null); // unbound
        episodes.record("s1", "任务", "", "success");
        facts.recordSuperseded("s1", "goals", "旧", 1, 2);

        CompactNowTool.CompactNowStats cs = CompactNowTool.stats();
        EpisodeLedger.EpisodicMemoryStats es = EpisodeLedger.stats();
        BiTemporalFactLedger.FactLedgerStats fs = BiTemporalFactLedger.stats();

        assertThat(cs.unboundRejects()).isEqualTo(1);
        assertThat(es.recorded()).isEqualTo(1);
        assertThat(fs.supersededWrites()).isEqualTo(1);
    }

    @Test
    void noCrossContaminationAmongThree() {
        EpisodeLedger episodes = new EpisodeLedger(stateStore, text -> new float[]{1f});
        episodes.record("s1", "任务", "", "success");

        assertThat(CompactNowTool.stats().calls()).isZero();
        assertThat(BiTemporalFactLedger.stats().supersededWrites()).isZero();
        assertThat(EpisodeLedger.stats().recordCalls()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependentAcrossThree() {
        EpisodeLedger episodes = new EpisodeLedger(stateStore, text -> new float[]{1f});
        episodes.record("s1", "任务", "", "success");
        BiTemporalFactLedger facts = new BiTemporalFactLedger(stateStore);
        facts.recordSuperseded("s1", "goals", "旧", 1, 2);

        EpisodeLedger.resetForTest();
        assertThat(EpisodeLedger.stats().recordCalls()).isZero();
        assertThat(BiTemporalFactLedger.stats().supersededWrites()).isEqualTo(1);

        BiTemporalFactLedger.resetForTest();
        assertThat(BiTemporalFactLedger.stats().supersededWrites()).isZero();
        assertThat(CompactNowTool.stats().calls()).isZero();
    }
}
