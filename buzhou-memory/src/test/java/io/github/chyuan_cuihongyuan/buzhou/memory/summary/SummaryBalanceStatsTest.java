package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1727 / T2656：SummaryBalanceStats 纯函数直测——失衡度/哨兵/空段。
 */
class SummaryBalanceStatsTest {

    @Test
    void balancedSectionsHaveImbalanceNearOne() {
        var report = SummaryBalanceStats.analyze(List.of(100, 100, 100, 100));
        assertThat(report.sections()).isEqualTo(4);
        assertThat(report.totalChars()).isEqualTo(400);
        assertThat(report.imbalance()).isCloseTo(1d, within(1e-9));
    }

    @Test
    void dominantSectionPushesImbalanceToK() {
        var report = SummaryBalanceStats.analyze(List.of(10, 10, 10, 970));
        assertThat(report.largestIndex()).isEqualTo(3);
        assertThat(report.smallestIndex()).isEqualTo(0);
        assertThat(report.imbalance()).isCloseTo(3.88d, within(1e-9));
    }

    @Test
    void degenerateAndEmptyCases() {
        assertThat(SummaryBalanceStats.analyze(List.of(50)).imbalance()).isEqualTo(-1d);
        assertThat(SummaryBalanceStats.analyze(List.of()).imbalance()).isEqualTo(-1d);
        assertThat(SummaryBalanceStats.analyze(null).sections()).isZero();
        var allZero = SummaryBalanceStats.analyze(List.of(0, 0, 0, 0));
        assertThat(allZero.imbalance()).isEqualTo(1d);
    }
}
