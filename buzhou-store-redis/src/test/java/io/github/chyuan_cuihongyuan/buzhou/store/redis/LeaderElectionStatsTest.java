package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 838 / T1176：选举竞争读数回归——四态计数/竞争烈度精确值/null 忽略/空真。
 */
class LeaderElectionStatsTest {

    @Test
    void outcomeCountsAndContention() {
        LeaderElectionStats stats = new LeaderElectionStats();
        stats.record(LeaderElectionStats.Outcome.ACQUIRED);
        stats.record(LeaderElectionStats.Outcome.RENEWED);
        stats.record(LeaderElectionStats.Outcome.RENEWED);
        stats.record(LeaderElectionStats.Outcome.RENEWED);
        stats.record(LeaderElectionStats.Outcome.OTHER_HOLDER);
        stats.record(LeaderElectionStats.Outcome.LOST);

        assertThat(stats.totalAttempts()).isEqualTo(6);
        assertThat(stats.acquired()).isEqualTo(1);
        assertThat(stats.renewed()).isEqualTo(3);
        assertThat(stats.otherHolder()).isEqualTo(1);
        assertThat(stats.lost()).isEqualTo(1);
        // 烈度 = (1+1)/6
        assertThat(stats.contentionRatio()).isCloseTo(2.0 / 6, within(1e-9));
    }

    @Test
    void steadyStateNearZeroContention() {
        LeaderElectionStats stats = new LeaderElectionStats();
        stats.record(LeaderElectionStats.Outcome.ACQUIRED);
        for (int i = 0; i < 50; i++) {
            stats.record(LeaderElectionStats.Outcome.RENEWED);
        }
        assertThat(stats.contentionRatio()).isZero(); // 全程保位无竞争
    }

    @Test
    void nullIgnoredAndEmptyTruth() {
        LeaderElectionStats stats = new LeaderElectionStats();
        stats.record(null);
        assertThat(stats.totalAttempts()).isZero();
        assertThat(stats.contentionRatio()).isZero();
        assertThat(stats.acquired()).isZero();
    }
}
