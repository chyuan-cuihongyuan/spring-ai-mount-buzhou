package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1745 / T2692：RetrySpreadStats 纯函数直测——相对散布契约。
 */
class RetrySpreadStatsTest {

    @Test
    void identicalDelaysHaveZeroSpread() {
        var report = RetrySpreadStats.analyze(List.of(100L, 100L, 100L));
        assertThat(report.count()).isEqualTo(3);
        assertThat(report.meanMillis()).isCloseTo(100d, within(1e-9));
        assertThat(report.relativeSpread()).isCloseTo(0d, within(1e-9));
    }

    @Test
    void spreadOutDelaysHaveHighSpread() {
        var report = RetrySpreadStats.analyze(List.of(0L, 2000L, 1000L, 1000L));
        assertThat(report.meanMillis()).isCloseTo(1000d, within(1e-9));
        assertThat(report.relativeSpread()).isCloseTo(2d, within(1e-9));
        assertThat(report.minMillis()).isZero();
        assertThat(report.maxMillis()).isEqualTo(2000L);
    }

    @Test
    void degenerateAndNegative() {
        assertThat(RetrySpreadStats.analyze(List.of()).relativeSpread()).isEqualTo(-1d);
        assertThat(RetrySpreadStats.analyze(List.of(5L)).relativeSpread()).isEqualTo(-1d);
        assertThat(RetrySpreadStats.analyze(List.of(-1L, 5L)).count()).isEqualTo(1);
        assertThat(RetrySpreadStats.analyze(null).count()).isZero();
    }

    @Test
    void allZeroDelaysRecordZeroSpread() {
        var report = RetrySpreadStats.analyze(List.of(0L, 0L));
        assertThat(report.relativeSpread()).isZero();
    }
}
