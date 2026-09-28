package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7048：KadaneMaxSubarray 合同——单遍最大子段和。
 * 手锚；随机 vs 暴力圣像（双语义）；全负/空；fail-fast。
 */
class KadaneMaxSubarrayTest {

    @Test
    void classicHandAnchors() {
        assertThat(KadaneMaxSubarray.maxSubarray(new long[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}))
                .isEqualTo(6L);
        assertThat(KadaneMaxSubarray.maxSubarrayNonEmpty(new long[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}))
                .isEqualTo(6L);
        assertThat(KadaneMaxSubarray.maxSubarray(new long[]{-5, -3, -8})).isZero();
        assertThat(KadaneMaxSubarray.maxSubarrayNonEmpty(new long[]{-5, -3, -8})).isEqualTo(-3L);
        assertThat(KadaneMaxSubarray.maxSubarray(new long[]{})).isZero();
    }

    @Test
    void randomSequencesMatchBruteForceBothSemantics() {
        Random rng = new Random(7048L);
        for (int round = 0; round < 300; round++) {
            int n = rng.nextInt(40);
            long[] values = new long[n];
            for (int i = 0; i < n; i++) {
                values[i] = rng.nextInt(41) - 20;
            }
            long bestEmpty = 0;
            long bestNonEmpty = n == 0 ? 0 : Long.MIN_VALUE;
            for (int i = 0; i < n; i++) {
                long sum = 0;
                for (int j = i; j < n; j++) {
                    sum += values[j];
                    bestEmpty = Math.max(bestEmpty, sum);
                    bestNonEmpty = Math.max(bestNonEmpty, sum);
                }
            }
            if (n > 0) {
                bestNonEmpty = Math.max(bestNonEmpty, maxSingle(values));
            }
            assertThat(KadaneMaxSubarray.maxSubarray(values))
                    .as("round %d 空段语义", round).isEqualTo(bestEmpty);
            if (n > 0) {
                assertThat(KadaneMaxSubarray.maxSubarrayNonEmpty(values))
                        .as("round %d 非空语义", round).isEqualTo(bestNonEmpty);
            }
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> KadaneMaxSubarray.maxSubarray(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KadaneMaxSubarray.maxSubarrayNonEmpty(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KadaneMaxSubarray.maxSubarrayNonEmpty(new long[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private long maxSingle(long[] values) {
        long max = Long.MIN_VALUE;
        for (long value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}
