package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1747 / T2696：IdempotencyCollisions 直测——冲突占比/FIFO 逐出。
 */
class IdempotencyCollisionsTest {

    @Test
    void emptyCarriesSentinel() {
        var collisions = new IdempotencyCollisions();
        assertThat(collisions.census().collisionRatio()).isEqualTo(-1d);
    }

    @Test
    void collisionsTallyWithRatio() {
        var collisions = new IdempotencyCollisions();
        collisions.record("k1", false);
        collisions.record("k1", true);
        collisions.record("k2", false);
        collisions.record(null, true);
        var census = collisions.census();
        assertThat(census.distinctKeys()).isEqualTo(3);
        assertThat(census.totalRecords()).isEqualTo(4);
        assertThat(census.replayedRecords()).isEqualTo(2);
        assertThat(census.collisionRatio()).isCloseTo(0.5d, within(1e-9));
    }

    @Test
    void boundedDistinctEvictsOldest() {
        var collisions = new IdempotencyCollisions(2);
        collisions.record("a", false);
        collisions.record("b", false);
        collisions.record("c", false);
        assertThat(collisions.census().distinctKeys()).isEqualTo(2);
    }
}
