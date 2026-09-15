package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1723 / T2648：RepairOutcomeStats 直测——三动作归账/重放占比/reset。
 */
class RepairOutcomeStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new RepairOutcomeStats();
        assertThat(stats.census().replayRatio()).isEqualTo(-1d);
    }

    @Test
    void actionsTallyWithReplayRatio() {
        var stats = new RepairOutcomeStats();
        stats.record(RepairOutcomeStats.Action.REPLAYED);
        stats.record(RepairOutcomeStats.Action.REPLAYED);
        stats.record(RepairOutcomeStats.Action.MARKED_FAILED);
        stats.record(RepairOutcomeStats.Action.SKIPPED_GONE);
        var census = stats.census();
        assertThat(census.total()).isEqualTo(4);
        assertThat(census.replayed()).isEqualTo(2);
        assertThat(census.markedFailed()).isEqualTo(1);
        assertThat(census.skippedGone()).isEqualTo(1);
        assertThat(census.replayRatio()).isCloseTo(0.5d, within(1e-9));
    }

    @Test
    void resetForTest() {
        var stats = new RepairOutcomeStats();
        stats.record(RepairOutcomeStats.Action.REPLAYED);
        stats.resetForTest();
        assertThat(stats.census().total()).isZero();
    }
}
