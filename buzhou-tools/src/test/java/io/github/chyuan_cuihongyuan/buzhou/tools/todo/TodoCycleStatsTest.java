package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1729 / T2660：TodoCycleStats 直测——返工计数/最惨项/基数并桶。
 */
class TodoCycleStatsTest {

    @Test
    void reopensTallyPerItem() {
        var stats = new TodoCycleStats();
        stats.recordReopen("t1");
        stats.recordReopen("t1");
        stats.recordReopen("t2");
        var census = stats.census();
        assertThat(census.touchedItems()).isEqualTo(2);
        assertThat(census.totalReopens()).isEqualTo(3);
        assertThat(census.worstReopens()).isEqualTo(2);
    }

    @Test
    void overflowBucketsBeyondCapacity() {
        var stats = new TodoCycleStats(2);
        stats.recordReopen("a");
        stats.recordReopen("b");
        stats.recordReopen("c");
        assertThat(stats.census().touchedItems()).isEqualTo(3);
    }

    @Test
    void emptyAndAnonymous() {
        var stats = new TodoCycleStats();
        assertThat(stats.census().touchedItems()).isZero();
        stats.recordReopen(null);
        assertThat(stats.census().touchedItems()).isEqualTo(1);
    }
}
