package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1725 / T2652：RetrievalRankStats 直测——MRR/Hit@k/miss 口径。
 */
class RetrievalRankStatsTest {

    @Test
    void mrrHitAtKAndMisses() {
        var stats = new RetrievalRankStats();
        stats.record(1);
        stats.record(3);
        stats.record(2);
        stats.record(0);
        var report = stats.report();
        assertThat(report.hits()).isEqualTo(3);
        assertThat(report.misses()).isEqualTo(1);
        assertThat(report.hitAt1()).isEqualTo(1);
        assertThat(report.hitAt3()).isEqualTo(3);
        assertThat(report.mrr()).isCloseTo((1d + 1d / 3d + 1d / 2d) / 3d, within(1e-6));
    }

    @Test
    void noHitsCarriesSentinel() {
        var stats = new RetrievalRankStats();
        stats.record(0);
        stats.record(-2);
        var report = stats.report();
        assertThat(report.hits()).isZero();
        assertThat(report.misses()).isEqualTo(2);
        assertThat(report.mrr()).isEqualTo(-1d);
    }

    @Test
    void emptyReportIsAllZeroWithSentinel() {
        var stats = new RetrievalRankStats();
        var report = stats.report();
        assertThat(report.hits()).isZero();
        assertThat(report.mrr()).isEqualTo(-1d);
        assertThat(report.hitAt1()).isZero();
    }
}
