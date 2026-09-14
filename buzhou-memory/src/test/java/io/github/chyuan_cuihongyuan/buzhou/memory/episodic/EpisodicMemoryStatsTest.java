package io.github.chyuan_cuihongyuan.buzhou.memory.episodic;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1055 / impl 807：情景记忆读写双守恒读面——写入/丢弃/失败三桶、
 * 召回命中/空召/丢弃三桶、双守恒恒等式、resetForTest 归零。
 * 骨架同 EpisodeLedgerSequenceRecoveryTest（InMemory store + lambda provider）。
 */
class EpisodicMemoryStatsTest {

    private final SessionStateStore stateStore = new InMemorySessionStateStore();

    @BeforeEach
    void reset() {
        EpisodeLedger.resetForTest();
    }

    @Test
    void successfulRecordAndRecallHit() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        ledger.record("s1", "部署数据库迁移", "migrate --apply", "success");
        var hits = ledger.recallExamples("s1", "部署数据库迁移", 5);
        assertThat(hits).hasSize(1);

        EpisodeLedger.EpisodicMemoryStats stats = EpisodeLedger.stats();
        assertThat(stats.recordCalls()).isEqualTo(1);
        assertThat(stats.recorded()).isEqualTo(1);
        assertThat(stats.recallCalls()).isEqualTo(1);
        assertThat(stats.recallHits()).isEqualTo(1);
        assertThat(stats.recallEmpties()).isZero();
    }

    @Test
    void nullProviderRecordCountsDropped() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, null);
        ledger.record("s1", "goal", "", "success");

        EpisodeLedger.EpisodicMemoryStats stats = EpisodeLedger.stats();
        assertThat(stats.recordCalls()).isEqualTo(1);
        assertThat(stats.recordDropped()).isEqualTo(1);
        assertThat(stats.recorded()).isZero();
    }

    @Test
    void blankGoalRecallCountsDropped() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f});
        assertThat(ledger.recallExamples("s1", "  ", 5)).isEmpty();

        assertThat(EpisodeLedger.stats().recallDropped()).isEqualTo(1);
    }

    @Test
    void unrelatedGoalCountsRecallEmpty() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> {
            // 正交向量：余弦 0，全部低于 0.10 语义地板
            if (text.equals("迁移上线")) {
                return new float[]{1f, 0f};
            }
            return new float[]{0f, 1f};
        });
        ledger.record("s1", "迁移上线", "", "success");
        assertThat(ledger.recallExamples("s1", " unrelated ", 5)).isEmpty();

        EpisodeLedger.EpisodicMemoryStats stats = EpisodeLedger.stats();
        assertThat(stats.recallCalls()).isEqualTo(1);
        assertThat(stats.recallEmpties()).isEqualTo(1);
        assertThat(stats.recallHits()).isZero();
    }

    @Test
    void bothConservationIdentitiesHold() {
        EpisodeLedger good = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        good.record("s1", "任务 A", "", "success");   // recorded
        good.record("s1", " ", "", "success");        // dropped（空白 goal）
        good.record("s1", null, "", "success");       // dropped（null goal）
        good.recallExamples("s1", "任务 A", 5);        // hits
        good.recallExamples("s1", " ", 5);            // dropped

        EpisodeLedger.EpisodicMemoryStats stats = EpisodeLedger.stats();
        assertThat(stats.recordCalls())
                .isEqualTo(stats.recorded() + stats.recordDropped() + stats.recordFailures());
        assertThat(stats.recallCalls())
                .isEqualTo(stats.recallHits() + stats.recallEmpties() + stats.recallDropped());
        assertThat(stats.recorded()).isEqualTo(1);
        assertThat(stats.recordDropped()).isEqualTo(2);
        assertThat(stats.recallCalls()).isEqualTo(2);
        assertThat(stats.recallDropped()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f});
        ledger.record("s1", "任务", "", "success");
        assertThat(EpisodeLedger.stats().recordCalls()).isEqualTo(1);

        EpisodeLedger.resetForTest();

        EpisodeLedger.EpisodicMemoryStats stats = EpisodeLedger.stats();
        assertThat(stats.recordCalls()).isZero();
        assertThat(stats.recorded()).isZero();
        assertThat(stats.recallCalls()).isZero();
    }
}
