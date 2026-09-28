package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7012：ClosestPair 合同——分治最近点对。手锚；
 * 随机点集 vs 暴力 O(n²) 圣像逐步全等；重合点 0；
 * fail-fast。
 */
class ClosestPairTest {

    @Test
    void handAnchoredCases() {
        long[][] pts = {{0, 0}, {3, 4}, {1, 1}, {5, 5}};
        assertThat(ClosestPair.closestPairSquared(pts)).isEqualTo(2L);
        long[][] mirrored = {{5, 5}, {3, 4}, {1, 1}, {0, 0}};
        assertThat(ClosestPair.closestPairSquared(mirrored)).isEqualTo(2L);
    }

    @Test
    void duplicatesGiveZero() {
        long[][] pts = {{2, 2}, {10, 10}, {2, 2}, {7, 7}};
        assertThat(ClosestPair.closestPairSquared(pts)).isZero();
    }

    @Test
    void randomSetsMatchBruteForce() {
        Random rng = new Random(7012L);
        for (int round = 0; round < 200; round++) {
            int n = 2 + rng.nextInt(150);
            long[][] pts = new long[n][];
            for (int i = 0; i < n; i++) {
                pts[i] = new long[]{rng.nextInt(2000) - 1000, rng.nextInt(2000) - 1000};
            }
            long fast = ClosestPair.closestPairSquared(pts);
            long brute = Long.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    long dx = pts[i][0] - pts[j][0];
                    long dy = pts[i][1] - pts[j][1];
                    brute = Math.min(brute, dx * dx + dy * dy);
                }
            }
            assertThat(fast).as("round %d n=%d", round, n).isEqualTo(brute);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> ClosestPair.closestPairSquared(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClosestPair.closestPairSquared(new long[][]{{1, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClosestPair.closestPairSquared(
                new long[][]{{2_000_000_000L, 0}, {0, 2_000_000_000L}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClosestPair.closestPairSquared(new long[][]{{1, 1, 1}, {2, 2, 2}}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
