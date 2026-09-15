package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1721 / T2644：ErrorNoveltyLedger 直测——首见判定/占比/FIFO 逐出。
 */
class ErrorNoveltyLedgerTest {

    @Test
    void firstSeenThenRepeat() {
        var ledger = new ErrorNoveltyLedger();
        assertThat(ledger.record("sig-a")).isTrue();
        assertThat(ledger.record("sig-a")).isFalse();
        assertThat(ledger.record("sig-b")).isTrue();
        var report = ledger.report();
        assertThat(report.seenDistinct()).isEqualTo(2);
        assertThat(report.newCount()).isEqualTo(2);
        assertThat(report.repeatCount()).isEqualTo(1);
        assertThat(report.noveltyRatio()).isCloseTo(2d / 3d, within(1e-9));
    }

    @Test
    void fifoEvictionAllowsReFirstSeen() {
        var ledger = new ErrorNoveltyLedger(2);
        assertThat(ledger.record("a")).isTrue();
        assertThat(ledger.record("b")).isTrue();
        assertThat(ledger.record("c")).isTrue();
        assertThat(ledger.report().seenDistinct()).isEqualTo(2);
        assertThat(ledger.record("a")).isTrue();
    }

    @Test
    void emptyCarriesSentinelAndBlankNormalizes() {
        var ledger = new ErrorNoveltyLedger();
        assertThat(ledger.report().noveltyRatio()).isEqualTo(-1d);
        assertThat(ledger.record("")).isTrue();
        assertThat(ledger.record(null)).isFalse();
        assertThat(ledger.report().seenDistinct()).isEqualTo(1);
    }
}
