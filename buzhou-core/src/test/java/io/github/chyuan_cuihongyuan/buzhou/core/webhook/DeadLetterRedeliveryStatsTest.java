package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 846 / T1194：死信重投成功率回归——成败计数/成功率/streak 归零/空真。
 */
class DeadLetterRedeliveryStatsTest {

    @Test
    void rateAndStreak() {
        DeadLetterRedeliveryStats stats = new DeadLetterRedeliveryStats();
        stats.record(true);
        stats.record(false);
        stats.record(false);
        stats.record(true);

        assertThat(stats.attempts()).isEqualTo(4);
        assertThat(stats.successes()).isEqualTo(2);
        assertThat(stats.successRate()).isCloseTo(0.5, within(1e-9));
        assertThat(stats.consecutiveFailures()).isZero(); // 末次成功清零
    }

    @Test
    void streakCountsConsecutiveFailures() {
        DeadLetterRedeliveryStats stats = new DeadLetterRedeliveryStats();
        stats.record(false);
        stats.record(false);
        stats.record(false);
        assertThat(stats.consecutiveFailures()).isEqualTo(3);
        assertThat(stats.successRate()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void emptyTruth() {
        DeadLetterRedeliveryStats stats = new DeadLetterRedeliveryStats();
        assertThat(stats.attempts()).isZero();
        assertThat(stats.successRate()).isZero();
        assertThat(stats.consecutiveFailures()).isZero();
    }
}
