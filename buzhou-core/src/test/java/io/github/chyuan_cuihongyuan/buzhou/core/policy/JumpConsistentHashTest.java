package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 3020 / T5042：jump hash 合同——值域夹持、均匀性、扩容最小
 * 迁移（≈1/(n+1) 且全迁新桶）、确定性、单桶恒 0、非法桶数拒绝、
 * 负键可用。
 */
class JumpConsistentHashTest {

    @Test
    void bucketsShouldStayInRange() {
        for (long key = 0; key < 10_000; key++) {
            int bucket = JumpConsistentHash.bucketOf(key, 16);
            assertThat(bucket).as("key %d", key).isBetween(0, 15);
        }
    }

    @Test
    void distributionShouldBeNearUniform() {
        int buckets = 10;
        long samples = 100_000;
        long[] counts = new long[buckets];
        for (long key = 0; key < samples; key++) {
            counts[JumpConsistentHash.bucketOf(key, buckets)]++;
        }
        for (int b = 0; b < buckets; b++) {
            assertThat(counts[b] / (double) samples)
                    .as("bucket %d 占比", b)
                    .isCloseTo(0.1, within(0.01));
        }
    }

    @Test
    void growingByOneBucketShouldMoveMinimalKeysToNewBucket() {
        int oldCount = 10;
        int newCount = 11;
        long samples = 100_000;
        long moved = 0;
        long movedToNewBucket = 0;
        for (long key = 0; key < samples; key++) {
            if (JumpConsistentHash.movesOnResize(key, oldCount, newCount)) {
                moved++;
                if (JumpConsistentHash.bucketOf(key, newCount) == oldCount) {
                    movedToNewBucket++;
                }
            }
        }
        // 期望迁移率 1/11 ≈ 0.0909（最小迁移），且迁移键全落新桶 10
        assertThat(moved / (double) samples).isCloseTo(1.0 / 11, within(0.01));
        assertThat(movedToNewBucket).isEqualTo(moved);
    }

    @Test
    void sameKeyShouldBeDeterministic() {
        for (long key : new long[] {0, 42, -7, Long.MAX_VALUE, Long.MIN_VALUE + 1}) {
            assertThat(JumpConsistentHash.bucketOf(key, 8))
                    .isEqualTo(JumpConsistentHash.bucketOf(key, 8));
        }
    }

    @Test
    void singleBucketShouldAlwaysMapToZero() {
        for (long key = 0; key < 1_000; key++) {
            assertThat(JumpConsistentHash.bucketOf(key, 1)).isZero();
        }
    }

    @Test
    void negativeKeysShouldHashIntoRange() {
        for (long key = -10_000; key < 0; key++) {
            assertThat(JumpConsistentHash.bucketOf(key, 5)).isBetween(0, 4);
        }
    }

    @Test
    void invalidBucketCountShouldFailFast() {
        assertThatThrownBy(() -> JumpConsistentHash.bucketOf(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JumpConsistentHash.bucketOf(1, -3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
