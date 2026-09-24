package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5030 / T6162：跳跃一致哈希合同——论文圣像值钉住、确定性、
 * 单调稳定（5→6 迁移 ≈ 1/6）、均衡（max/min<2）、指纹稳定、fail-fast。
 */
class JumpHashTest {

    private static final int PROBE_KEYS = 1000;

    private static final int BUCKETS_SMALL = 5;

    private static final int BUCKETS_GROWN = 6;

    private static final int BUCKETS_TEN = 10;

    private static final int BUCKETS_HUNDRED = 100;

    private static final int MIN_STABLE_5_TO_6 = 800;

    private static final double MAX_BALANCE_RATIO = 2.0;

    @Test
    void paperOracleValuesShouldBePinned() {
        // 64 位 LCG 随机游走圣像：与论文算法逐位一致（Python 64 位语义预演钉住）
        assertThat(JumpHash.bucketOf(1L, BUCKETS_TEN)).isEqualTo(6);
        assertThat(JumpHash.bucketOf(1L, BUCKETS_HUNDRED)).isEqualTo(55);
        assertThat(JumpHash.bucketOf(42L, BUCKETS_SMALL)).isEqualTo(2);
        assertThat(JumpHash.bucketOf(42L, BUCKETS_TEN)).isEqualTo(2);
        assertThat(JumpHash.bucketOf(42L, BUCKETS_HUNDRED)).isEqualTo(43);
        assertThat(JumpHash.bucketOf(1000L, BUCKETS_TEN)).isEqualTo(9);
        assertThat(JumpHash.bucketOf(1000L, BUCKETS_HUNDRED)).isEqualTo(93);
        assertThat(JumpHash.bucketOf(987654321L, BUCKETS_SMALL)).isEqualTo(2);
        assertThat(JumpHash.bucketOf(987654321L, BUCKETS_HUNDRED)).isEqualTo(84);
    }

    @Test
    void sameKeyShouldMapToSameBucketDeterministically() {
        for (long key = 0; key < 64; key++) {
            int first = JumpHash.bucketOf(key, BUCKETS_TEN);
            int second = JumpHash.bucketOf(key, BUCKETS_TEN);
            assertThat(first).isEqualTo(second);
            assertThat(first).isBetween(0, BUCKETS_TEN - 1);
        }
    }

    @Test
    void growingBucketsShouldKeepMostKeysInPlace() {
        int stable = 0;
        for (long key = 0; key < PROBE_KEYS; key++) {
            if (JumpHash.bucketOf(key, BUCKETS_SMALL) == JumpHash.bucketOf(key, BUCKETS_GROWN)) {
                stable++;
            }
        }
        // 理论迁移率 1/(m+1)≈16.7%——留桶 ≥80%（实测 ≈83%）
        assertThat(stable).isGreaterThanOrEqualTo(MIN_STABLE_5_TO_6);
    }

    @Test
    void keysShouldSpreadBalancedAcrossBuckets() {
        Map<Integer, Integer> occupancy = new HashMap<>();
        for (long key = 0; key < PROBE_KEYS; key++) {
            occupancy.merge(JumpHash.bucketOf(key, BUCKETS_TEN), 1, Integer::sum);
        }
        int max = occupancy.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = occupancy.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        assertThat((double) max / min).isLessThan(MAX_BALANCE_RATIO);
    }

    @Test
    void stringFingerprintShouldBeStableAndRouteConsistently() {
        assertThat(JumpHash.fingerprint("session-a")).isEqualTo(1623557964181311581L);
        assertThat(JumpHash.fingerprint("session-b")).isEqualTo(1623554665646426948L);
        assertThat(JumpHash.fingerprint("tenant-x")).isEqualTo(-4400183786085845888L);
        assertThat(JumpHash.bucketOf("session-a", BUCKETS_TEN)).isEqualTo(2);
        assertThat(JumpHash.bucketOf("session-b", BUCKETS_TEN)).isEqualTo(3);
        assertThat(JumpHash.bucketOf("tenant-x", BUCKETS_TEN)).isEqualTo(0);
        assertThat(JumpHash.bucketOf("session-a", BUCKETS_TEN))
                .isEqualTo(JumpHash.bucketOf(JumpHash.fingerprint("session-a"), BUCKETS_TEN));
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> JumpHash.bucketOf(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JumpHash.bucketOf(1L, -3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JumpHash.bucketOf((String) null, BUCKETS_TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
