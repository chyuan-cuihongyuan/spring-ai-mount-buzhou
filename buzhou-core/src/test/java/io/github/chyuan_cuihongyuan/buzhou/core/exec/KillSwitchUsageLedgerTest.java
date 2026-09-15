package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1715 / T2632：KillSwitchUsageLedger 直测——留痕/配对时长/逐出。
 */
class KillSwitchUsageLedgerTest {

    @Test
    void flipsAreLedgered() {
        var ledger = new KillSwitchUsageLedger();
        ledger.recordKill("http", "incident-42", 1000L);
        ledger.recordRestore("http", 4000L);
        assertThat(ledger.entries()).hasSize(2);
        assertThat(ledger.entries().get(0).reason()).isEqualTo("incident-42");
        assertThat(ledger.entries().get(1).reason()).isEqualTo("restore");
        assertThat(ledger.killedDurationMillis()).isEqualTo(3000L);
        assertThat(ledger.counters()).containsExactly(1L, 1L);
    }

    @Test
    void restorePairsLatestMatchingKill() {
        var ledger = new KillSwitchUsageLedger();
        ledger.recordKill("a", "r", 0L);
        ledger.recordKill("b", "r", 100L);
        ledger.recordRestore("a", 600L);
        assertThat(ledger.killedDurationMillis()).isEqualTo(600L);
    }

    @Test
    void unpairedKillNotCountedInDuration() {
        var ledger = new KillSwitchUsageLedger();
        ledger.recordKill("a", "r", 0L);
        assertThat(ledger.killedDurationMillis()).isZero();
        assertThat(ledger.counters()[0]).isEqualTo(1L);
    }

    @Test
    void boundedCapacityEvictsOldest() {
        var ledger = new KillSwitchUsageLedger(2);
        ledger.recordKill("a", "1", 0L);
        ledger.recordKill("b", "2", 1L);
        ledger.recordKill("c", "3", 2L);
        assertThat(ledger.entries()).hasSize(2);
        assertThat(ledger.entries().get(0).tool()).isEqualTo("b");
    }
}
