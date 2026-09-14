package io.github.chyuan_cuihongyuan.buzhou.memory.episodic;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1105 / impl 857：EpisodeLedger 双实例组合——同 stateStore 跨实例
 * record/recall 后静态计数累计正确、双守恒保持、reset 归零。纯测试轮。
 */
class EpisodeLedgerDualInstanceTest {

    private SessionStateStore stateStore;

    @BeforeEach
    void reset() {
        EpisodeLedger.resetForTest();
        stateStore = new InMemorySessionStateStore();
    }

    @Test
    void crossInstanceCallsAccumulateCorrectly() {
        EpisodeLedger first = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        EpisodeLedger second = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});

        first.record("s1", "任务 A", "", "success");
        second.record("s1", "任务 B", "", "success");
        first.recallExamples("s1", "任务 A", 5);

        EpisodeLedger.EpisodicMemoryStats stats = EpisodeLedger.stats();
        assertThat(stats.recordCalls()).isEqualTo(2);
        assertThat(stats.recorded()).isEqualTo(2);
        assertThat(stats.recallCalls()).isEqualTo(1);
        assertThat(stats.recallHits()).isEqualTo(1);
    }

    @Test
    void dualConservationHoldsAcrossInstances() {
        EpisodeLedger a = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        EpisodeLedger b = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        a.record("s1", "A 任务", "", "success");
        b.record("s2", null, "", "success"); // dropped
        a.recallExamples("s1", "A 任务", 5); // hit
        b.recallExamples("s2", " ", 5);      // dropped（空白）

        EpisodeLedger.EpisodicMemoryStats stats = EpisodeLedger.stats();
        assertThat(stats.recordCalls())
                .isEqualTo(stats.recorded() + stats.recordDropped() + stats.recordFailures());
        assertThat(stats.recallCalls())
                .isEqualTo(stats.recallHits() + stats.recallEmpties() + stats.recallDropped());
    }

    @Test
    void resetForTestZeroesCounters() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f});
        ledger.record("s1", "任务", "", "success");
        assertThat(EpisodeLedger.stats().recordCalls()).isEqualTo(1);

        EpisodeLedger.resetForTest();

        assertThat(EpisodeLedger.stats().recordCalls()).isZero();
    }
}
