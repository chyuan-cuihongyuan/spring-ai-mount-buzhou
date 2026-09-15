package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1744 / T2690：HedgeStats 直测——赢率/节省/哨兵。
 */
class HedgeStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new HedgeStats();
        assertThat(stats.census().hedgeWinRatio()).isEqualTo(-1d);
    }

    @Test
    void winsTallyWithRatioAndSavings() {
        var stats = new HedgeStats();
        stats.recordHedge();
        stats.recordHedge();
        stats.recordHedge();
        stats.recordPrimaryWin();
        stats.recordHedgeWin();
        stats.recordHedgeWin();
        stats.recordLatencySaved(300);
        stats.recordLatencySaved(-1);
        var census = stats.census();
        assertThat(census.hedges()).isEqualTo(3);
        assertThat(census.primaryWins()).isEqualTo(1);
        assertThat(census.hedgeWins()).isEqualTo(2);
        assertThat(census.latencySavedMillis()).isEqualTo(300);
        assertThat(census.hedgeWinRatio()).isCloseTo(2d / 3d, within(1e-9));
    }
}
