package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1728 / T2658：CompactionTriggerStats 直测——四因归账/闲时占比/reset。
 */
class CompactionTriggerStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new CompactionTriggerStats();
        assertThat(stats.census().idleShare()).isEqualTo(-1d);
    }

    @Test
    void triggersTallyWithIdleShare() {
        var stats = new CompactionTriggerStats();
        stats.record(CompactionTriggerStats.Trigger.IDLE);
        stats.record(CompactionTriggerStats.Trigger.IDLE);
        stats.record(CompactionTriggerStats.Trigger.RATIO);
        stats.record(CompactionTriggerStats.Trigger.MANUAL);
        stats.record(CompactionTriggerStats.Trigger.CHECKPOINT);
        var census = stats.census();
        assertThat(census.total()).isEqualTo(5);
        assertThat(census.idleShare()).isCloseTo(0.4d, within(1e-9));
        assertThat(census.ratio()).isEqualTo(1);
    }

    @Test
    void resetForTest() {
        var stats = new CompactionTriggerStats();
        stats.record(CompactionTriggerStats.Trigger.RATIO);
        stats.resetForTest();
        assertThat(stats.census().total()).isZero();
    }
}
