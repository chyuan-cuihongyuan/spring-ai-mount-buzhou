package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1709 / T2620：MigrationOutcomeStats 直测——四桶归账/成功率哨兵/reset。
 */
class MigrationOutcomeStatsTest {

    @Test
    void emptyCensusCarriesNegativeRatio() {
        var stats = new MigrationOutcomeStats();
        var census = stats.census();
        assertThat(census.total()).isZero();
        assertThat(census.attemptSuccessRatio()).isEqualTo(-1d);
    }

    @Test
    void bucketsTallyAndRatioExcludesSkips() {
        var stats = new MigrationOutcomeStats();
        stats.record(MigrationOutcomeStats.Outcome.MIGRATED);
        stats.record(MigrationOutcomeStats.Outcome.MIGRATED);
        stats.record(MigrationOutcomeStats.Outcome.FAILED);
        stats.record(MigrationOutcomeStats.Outcome.SKIPPED_CURRENT);
        stats.record(MigrationOutcomeStats.Outcome.SKIPPED_EMPTY);
        var census = stats.census();
        assertThat(census.total()).isEqualTo(5);
        assertThat(census.migrated()).isEqualTo(2);
        assertThat(census.failed()).isEqualTo(1);
        assertThat(census.skippedCurrent()).isEqualTo(1);
        assertThat(census.skippedEmpty()).isEqualTo(1);
        assertThat(census.attemptSuccessRatio()).isEqualTo(2d / 3d);
    }

    @Test
    void resetForTestZeroesEverything() {
        var stats = new MigrationOutcomeStats();
        stats.record(MigrationOutcomeStats.Outcome.MIGRATED);
        stats.resetForTest();
        assertThat(stats.census().total()).isZero();
        assertThat(stats.census().attemptSuccessRatio()).isEqualTo(-1d);
    }
}
