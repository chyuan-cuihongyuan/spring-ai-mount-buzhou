package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HungarianMatcherTest {

    @Test
    void shouldMatchHandAnchors() {
        long[][] anchor = {{4, 1, 3}, {2, 0, 5}, {3, 2, 2}};
        assertThat(HungarianMatcher.minCost(anchor)).isEqualTo(5);
        assertThat(HungarianMatcher.assignment(anchor)).containsExactly(1, 0, 2);
        assertThat(HungarianMatcher.minCost(new long[][]{{1, 0}, {0, 9}})).isEqualTo(0);
        assertThat(HungarianMatcher.minCost(new long[][]{{5, 5}, {5, 5}})).isEqualTo(10);
        assertThat(HungarianMatcher.minCost(new long[][]{{7}})).isEqualTo(7);
    }

    @Test
    void shouldMatchPermutationOracleOnRandomMatrices() {
        Random random = new Random(8007);
        for (int round = 0; round < 100; round++) {
            int n = 2 + random.nextInt(5);
            long[][] cost = new long[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    cost[i][j] = random.nextInt(21);
                }
            }
            long expected = brutePermutation(cost);
            int[] assignment = HungarianMatcher.assignment(cost);
            assertThat(HungarianMatcher.minCost(cost))
                    .as("round=%d n=%d", round, n)
                    .isEqualTo(expected);
            assertThat(assignment).doesNotHaveDuplicates();
            for (int col : assignment) {
                assertThat(col).isBetween(0, n - 1);
            }
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        long[][] cost = {{4, 1, 3}, {2, 0, 5}, {3, 2, 2}};
        assertThat(HungarianMatcher.assignment(cost)).isEqualTo(HungarianMatcher.assignment(cost));
        assertThatThrownBy(() -> HungarianMatcher.assignment(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HungarianMatcher.assignment(new long[0][]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HungarianMatcher.assignment(new long[][]{{1, 2}, {3}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HungarianMatcher.assignment(new long[][]{null, {1}}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：全排列枚举最小总代价（n ≤ 6）。 */
    private static long brutePermutation(long[][] cost) {
        int n = cost.length;
        int[] cols = new int[n];
        for (int i = 0; i < n; i++) {
            cols[i] = i;
        }
        long best = Long.MAX_VALUE;
        do {
            long total = 0;
            for (int row = 0; row < n; row++) {
                total += cost[row][cols[row]];
            }
            best = Math.min(best, total);
        } while (nextPermutation(cols));
        return best;
    }

    private static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        int j = a.length - 1;
        while (a[j] <= a[i]) {
            j--;
        }
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) {
            int t2 = a[l];
            a[l] = a[r];
            a[r] = t2;
        }
        return true;
    }
}
