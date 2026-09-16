package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2012 / T3126：抢占重算账本合同——双面账（浪费/节省）、净收益
 * 正负两态、浪费率、重算幂等、空账不除零、畸形 fail-fast。
 */
class PreemptionLedgerTest {

    @Test
    void singlePreemptionShouldBookBothSides() {
        PreemptionLedger ledger = new PreemptionLedger();
        ledger.recordPreemption("victim-1", 40, 100);
        PreemptionLedger.PreemptionStats stats = ledger.stats();
        assertThat(stats.preemptions()).isEqualTo(1L);
        assertThat(stats.wastedWorkTicks()).isEqualTo(40L);
        assertThat(stats.savedTicks()).isEqualTo(100L);
        assertThat(stats.netBenefitTicks()).isEqualTo(60L); // 净赚
        assertThat(ledger.wasteRatio()).isCloseTo(40.0 / 140.0,
                org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void multiplePreemptionsShouldAccumulate() {
        PreemptionLedger ledger = new PreemptionLedger();
        ledger.recordPreemption("a", 10, 30);
        ledger.recordPreemption("b", 50, 20);
        PreemptionLedger.PreemptionStats stats = ledger.stats();
        assertThat(stats.preemptions()).isEqualTo(2L);
        assertThat(stats.wastedWorkTicks()).isEqualTo(60L);
        assertThat(stats.savedTicks()).isEqualTo(50L);
        assertThat(stats.netBenefitTicks()).isEqualTo(-10L); // 净赔——降阈值信号
        assertThat(ledger.wasteRatio()).isCloseTo(60.0 / 110.0,
                org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void recomputationShouldCountOncePerVictim() {
        PreemptionLedger ledger = new PreemptionLedger();
        ledger.recordPreemption("a", 10, 30);
        ledger.recordPreemption("b", 5, 5);
        ledger.recordRecomputation("a");
        ledger.recordRecomputation("a"); // 幂等
        assertThat(ledger.stats().recomputations()).isEqualTo(1L);
        assertThat(ledger.recomputeRate()).isCloseTo(0.5d,
                org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void emptyLedgerShouldNotDivideByZero() {
        PreemptionLedger ledger = new PreemptionLedger();
        assertThat(ledger.wasteRatio()).isZero();
        assertThat(ledger.recomputeRate()).isZero();
        assertThat(ledger.netBenefitTicks()).isZero();
        assertThat(ledger.stats().preemptions()).isZero();
    }

    @Test
    void netBenefitSignShouldDistinguishWinAndLoss() {
        PreemptionLedger win = new PreemptionLedger();
        win.recordPreemption("v", 10, 100);
        assertThat(win.netBenefitTicks()).isPositive();
        PreemptionLedger loss = new PreemptionLedger();
        loss.recordPreemption("v", 100, 10);
        assertThat(loss.netBenefitTicks()).isNegative();
        PreemptionLedger even = new PreemptionLedger();
        even.recordPreemption("v", 50, 50);
        assertThat(even.netBenefitTicks()).isZero();
    }

    @Test
    void malformedInputsShouldFailFast() {
        PreemptionLedger ledger = new PreemptionLedger();
        assertThatThrownBy(() -> ledger.recordPreemption(null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ledger.recordPreemption("v", -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ledger.recordPreemption("v", 1, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ledger.recordRecomputation(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
