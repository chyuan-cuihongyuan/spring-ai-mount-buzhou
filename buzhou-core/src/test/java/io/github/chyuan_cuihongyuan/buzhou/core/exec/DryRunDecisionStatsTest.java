package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1720 / T2642：DryRunDecisionStats 直测——三态归账/拦截占比/reset。
 */
class DryRunDecisionStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new DryRunDecisionStats();
        assertThat(stats.census().blockRatio()).isEqualTo(-1d);
    }

    @Test
    void decisionsTallyWithBlockRatio() {
        var stats = new DryRunDecisionStats();
        stats.record(DryRunDecisionStats.Decision.WOULD_RUN);
        stats.record(DryRunDecisionStats.Decision.WOULD_RUN);
        stats.record(DryRunDecisionStats.Decision.WOULD_BLOCK);
        stats.record(DryRunDecisionStats.Decision.PLAN_ERROR);
        var census = stats.census();
        assertThat(census.planned()).isEqualTo(4);
        assertThat(census.wouldRun()).isEqualTo(2);
        assertThat(census.wouldBlock()).isEqualTo(1);
        assertThat(census.planErrors()).isEqualTo(1);
        assertThat(census.blockRatio()).isCloseTo(0.25d, within(1e-9));
    }

    @Test
    void resetForTest() {
        var stats = new DryRunDecisionStats();
        stats.record(DryRunDecisionStats.Decision.WOULD_BLOCK);
        stats.resetForTest();
        assertThat(stats.census().planned()).isZero();
    }
}
