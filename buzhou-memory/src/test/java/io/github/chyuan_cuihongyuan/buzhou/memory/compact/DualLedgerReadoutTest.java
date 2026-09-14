package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.episodic.EpisodeLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1088 / impl 840：memory 双台账组合——fact 台账与 episodic 台账交叉
 * 调用后读面互不串账、reset 独立隔离。纯测试轮。
 */
class DualLedgerReadoutTest {

    private SessionStateStore stateStore;

    @BeforeEach
    void setUp() {
        BiTemporalFactLedger.resetForTest();
        EpisodeLedger.resetForTest();
        stateStore = new InMemorySessionStateStore();
    }

    @Test
    void crossCallsKeepBothReadoutsIndependent() {
        BiTemporalFactLedger facts = new BiTemporalFactLedger(stateStore);
        EpisodeLedger episodes = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});

        facts.recordSuperseded("s1", "goals", "旧事实", 1, 2);
        episodes.record("s1", "任务", "", "success");
        facts.historyOf("s1", "goals");
        episodes.recallExamples("s1", "任务", 5);

        BiTemporalFactLedger.FactLedgerStats fs = BiTemporalFactLedger.stats();
        assertThat(fs.supersededWrites()).isEqualTo(1);
        assertThat(fs.historyLookups()).isEqualTo(1);

        EpisodeLedger.EpisodicMemoryStats es = EpisodeLedger.stats();
        assertThat(es.recordCalls()).isEqualTo(1);
        assertThat(es.recallCalls()).isEqualTo(1);
    }

    @Test
    void noCrossContaminationBetweenLedgers() {
        EpisodeLedger episodes = new EpisodeLedger(stateStore, text -> new float[]{1f});
        episodes.record("s1", "任务", "", "success");

        BiTemporalFactLedger.FactLedgerStats fs = BiTemporalFactLedger.stats();
        assertThat(fs.supersededWrites()).isZero();
        assertThat(fs.historyLookups()).isZero();
    }

    @Test
    void resetsAreIndependent() {
        BiTemporalFactLedger facts = new BiTemporalFactLedger(stateStore);
        EpisodeLedger episodes = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        facts.recordSuperseded("s1", "goals", "旧", 1, 2);
        episodes.record("s1", "任务", "", "success");

        BiTemporalFactLedger.resetForTest();
        assertThat(BiTemporalFactLedger.stats().supersededWrites()).isZero();
        assertThat(EpisodeLedger.stats().recordCalls()).isEqualTo(1);

        EpisodeLedger.resetForTest();
        assertThat(EpisodeLedger.stats().recordCalls()).isZero();
    }
}
