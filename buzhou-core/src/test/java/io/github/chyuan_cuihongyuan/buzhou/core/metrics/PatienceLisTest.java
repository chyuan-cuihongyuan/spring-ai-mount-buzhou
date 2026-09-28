package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7025：PatienceLis 合同——耐心排序 LIS。手锚；随机
 * 序列 vs O(n²) DP 圣像；LIS 实例合法（递增+等长）；fail-fast。
 */
class PatienceLisTest {

    @Test
    void handAnchoredCases() {
        assertThat(PatienceLis.length(new long[]{10, 9, 2, 5, 3, 7, 101, 18})).isEqualTo(4);
        assertThat(PatienceLis.length(new long[]{0, 1, 0, 3, 2, 3})).isEqualTo(4);
        assertThat(PatienceLis.length(new long[]{7, 7, 7, 7})).isEqualTo(1);
        assertThat(PatienceLis.length(new long[]{})).isZero();
        PatienceLis.Result result = PatienceLis.longestIncreasingSubsequence(
                new long[]{10, 9, 2, 5, 3, 7, 101, 18});
        assertThat(result.length()).isEqualTo(4);
        assertThat(result.sequence()).hasSize(4);
    }

    @Test
    void randomSequencesMatchDpOracle() {
        Random rng = new Random(7025L);
        for (int round = 0; round < 300; round++) {
            int n = rng.nextInt(40);
            long[] values = new long[n];
            for (int i = 0; i < n; i++) {
                values[i] = rng.nextInt(30);
            }
            int fast = PatienceLis.length(values);
            int oracle = dpOracle(values);
            assertThat(fast).as("round %d", round).isEqualTo(oracle);
            PatienceLis.Result result = PatienceLis.longestIncreasingSubsequence(values);
            assertThat(result.sequence().length).isEqualTo(oracle);
            for (int i = 1; i < result.sequence().length; i++) {
                assertThat(result.sequence()[i])
                        .as("LIS 实例严格递增 %s", java.util.Arrays.toString(result.sequence()))
                        .isGreaterThan(result.sequence()[i - 1]);
            }
            assertThat(isSubsequence(values, result.sequence())).isTrue();
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> PatienceLis.longestIncreasingSubsequence(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** O(n²) DP 圣像。 */
    private int dpOracle(long[] values) {
        if (values.length == 0) {
            return 0;
        }
        int[] dp = new int[values.length];
        java.util.Arrays.fill(dp, 1);
        int best = 1;
        for (int i = 1; i < values.length; i++) {
            for (int j = 0; j < i; j++) {
                if (values[j] < values[i]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    private boolean isSubsequence(long[] values, long[] candidate) {
        int cursor = 0;
        for (long value : values) {
            if (cursor < candidate.length && value == candidate[cursor]) {
                cursor++;
            }
        }
        return cursor == candidate.length;
    }
}
