package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1736 / T2674：McpReconnectStats 直测——成功率/退避账目/哨兵。
 */
class McpReconnectStatsTest {

    @Test
    void emptyCarriesSentinels() {
        var stats = new McpReconnectStats();
        var census = stats.census();
        assertThat(census.successRatio()).isEqualTo(-1d);
        assertThat(census.avgBackoffMillis()).isEqualTo(-1d);
    }

    @Test
    void attemptsTallyWithSuccessRateAndBackoff() {
        var stats = new McpReconnectStats();
        stats.recordAttempt(0);
        stats.recordAttempt(500);
        stats.recordAttempt(2000);
        stats.recordSuccess();
        stats.recordSuccess();
        stats.recordGiveUp();
        var census = stats.census();
        assertThat(census.attempts()).isEqualTo(3);
        assertThat(census.successes()).isEqualTo(2);
        assertThat(census.giveUps()).isEqualTo(1);
        assertThat(census.successRatio()).isCloseTo(2d / 3d, within(1e-9));
        assertThat(census.maxBackoffMillis()).isEqualTo(2000L);
        assertThat(census.avgBackoffMillis()).isCloseTo(2500d / 3d, within(1e-9));
    }

    @Test
    void negativeBackoffIgnored() {
        var stats = new McpReconnectStats();
        stats.recordAttempt(-1);
        assertThat(stats.census().attempts()).isZero();
    }
}
