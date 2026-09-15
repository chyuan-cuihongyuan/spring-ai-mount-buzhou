package io.github.chyuan_cuihongyuan.buzhou.memory.episodic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1726 / T2654：EpisodeRetentionStats 直测——三桶归账/逐出占比/reset。
 */
class EpisodeRetentionStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new EpisodeRetentionStats();
        assertThat(stats.census().evictRatio()).isEqualTo(-1d);
    }

    @Test
    void bucketsTallyWithEvictRatio() {
        var stats = new EpisodeRetentionStats();
        stats.record(EpisodeRetentionStats.RetentionEvent.STORED, 10);
        stats.record(EpisodeRetentionStats.RetentionEvent.EVICTED_TTL, 3);
        stats.record(EpisodeRetentionStats.RetentionEvent.EVICTED_CAPACITY, 2);
        var census = stats.census();
        assertThat(census.stored()).isEqualTo(10);
        assertThat(census.evictedTtl()).isEqualTo(3);
        assertThat(census.evictedCapacity()).isEqualTo(2);
        assertThat(census.evictRatio()).isCloseTo(5d / 15d, within(1e-9));
    }

    @Test
    void negativeCountsIgnoredAndReset() {
        var stats = new EpisodeRetentionStats();
        stats.record(EpisodeRetentionStats.RetentionEvent.STORED, -1);
        stats.record(EpisodeRetentionStats.RetentionEvent.STORED, 1);
        stats.resetForTest();
        assertThat(stats.census().stored()).isZero();
    }
}
