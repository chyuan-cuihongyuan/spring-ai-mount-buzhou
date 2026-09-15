package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1746 / T2694：BudgetEtaProjection 纯函数直测——三裁决契约。
 */
class BudgetEtaProjectionTest {

    @Test
    void projectionFromAverageSpend() {
        var eta = BudgetEtaProjection.project(100d, List.of(10d, 20d, 30d), 60_000L);
        assertThat(eta.avgSpendPerInterval()).isCloseTo(20d, within(1e-9));
        assertThat(eta.etaMillis()).isEqualTo(300_000L);
        assertThat(eta.verdict()).isEqualTo(BudgetEtaProjection.Verdict.PROJECTED);
    }

    @Test
    void zeroSpendIsStable() {
        var eta = BudgetEtaProjection.project(100d, List.of(0d, 0d), 60_000L);
        assertThat(eta.etaMillis()).isEqualTo(-1L);
        assertThat(eta.verdict()).isEqualTo(BudgetEtaProjection.Verdict.STABLE);
    }

    @Test
    void noDataCarriesSentinel() {
        assertThat(BudgetEtaProjection.project(50d, List.of(), 1000L).verdict())
                .isEqualTo(BudgetEtaProjection.Verdict.NO_DATA);
        assertThat(BudgetEtaProjection.project(50d, null, 1000L).etaMillis()).isEqualTo(-1L);
    }
}
