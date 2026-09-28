package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7045：ActivitySelectionGreedy 合同——最早结束贪心。
 * 经典手锚；随机 vs O(n²) DP 圣像；并列 canonical；fail-fast。
 */
class ActivitySelectionGreedyTest {

    @Test
    void classicHandAnchors() {
        long[][] activities = {{1, 3}, {2, 4}, {3, 5}, {0, 7}, {5, 8}, {8, 9}};
        List<Integer> selected = ActivitySelectionGreedy.select(activities);
        assertThat(selected).containsExactly(0, 2, 4, 5);
        assertThat(ActivitySelectionGreedy.maxCount(activities)).isEqualTo(4);
    }

    @Test
    void touchingBoundaryCompatible() {
        long[][] touching = {{0, 5}, {5, 10}, {10, 15}};
        assertThat(ActivitySelectionGreedy.select(touching)).containsExactly(0, 1, 2);
    }

    @Test
    void randomSetsMatchDpOracle() {
        Random rng = new Random(7045L);
        for (int round = 0; round < 300; round++) {
            int n = 1 + rng.nextInt(30);
            long[][] activities = new long[n][];
            for (int i = 0; i < n; i++) {
                int start = rng.nextInt(50);
                activities[i] = new long[]{start, start + 1 + rng.nextInt(10)};
            }
            int greedy = ActivitySelectionGreedy.maxCount(activities);
            int oracle = dpOracle(activities);
            assertThat(greedy).as("round %d", round).isEqualTo(oracle);
            List<Integer> selected = ActivitySelectionGreedy.select(activities);
            long lastEnd = Long.MIN_VALUE;
            for (int index : selected) {
                assertThat(activities[index][0]).as("兼容性").isGreaterThanOrEqualTo(lastEnd);
                lastEnd = activities[index][1];
            }
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> ActivitySelectionGreedy.select(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActivitySelectionGreedy.select(new long[][]{{3, 3}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActivitySelectionGreedy.select(new long[][]{{4, 2}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActivitySelectionGreedy.select(new long[][]{{1, 2, 3}}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** O(n²) DP 圣像（按结束排序后 DP）。 */
    private int dpOracle(long[][] activities) {
        long[][] sorted = activities.clone();
        java.util.Arrays.sort(sorted, (a, b) -> Long.compare(a[1], b[1]));
        int[] dp = new int[sorted.length];
        java.util.Arrays.fill(dp, 1);
        int best = 0;
        for (int i = 0; i < sorted.length; i++) {
            for (int j = 0; j < i; j++) {
                if (sorted[j][1] <= sorted[i][0]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            best = Math.max(best, dp[i]);
        }
        return best;
    }
}
