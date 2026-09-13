package io.github.chyuan_cuihongyuan.buzhou.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 844 / T1190：降级原因分布回归——五态计数占比降序/null 忽略/空真。
 */
class SummaryDegradeReasonsTest {

    @Test
    void countsSharesSorted() {
        SummaryDegradeReasons stats = new SummaryDegradeReasons();
        stats.record(SummaryDegradeReasons.Reason.OVER_LIMIT);
        stats.record(SummaryDegradeReasons.Reason.OVER_LIMIT);
        stats.record(SummaryDegradeReasons.Reason.OVER_LIMIT);
        stats.record(SummaryDegradeReasons.Reason.GENERATION_FAILED);
        stats.record(SummaryDegradeReasons.Reason.UNKNOWN);

        var report = stats.snapshot();
        assertThat(report.total()).isEqualTo(5);
        assertThat(report.reasons().get(0).reason()).isEqualTo(SummaryDegradeReasons.Reason.OVER_LIMIT);
        assertThat(report.reasons().get(0).share()).isCloseTo(0.6, within(1e-9));
        assertThat(report.reasons()).hasSize(3);
    }

    @Test
    void nullIgnoredEmptyTruth() {
        SummaryDegradeReasons stats = new SummaryDegradeReasons();
        stats.record(null);
        var report = stats.snapshot();
        assertThat(report.total()).isZero();
        assertThat(report.reasons()).isEmpty();
    }
}
