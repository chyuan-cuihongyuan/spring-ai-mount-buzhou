package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BipartiteCheckerTest {

    @Test
    void shouldMatchHandAnchors() {
        int[][] evenCycle = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
        assertThat(BipartiteChecker.isBipartite(4, evenCycle)).isTrue();
        int[][] oddCycle = {{0, 1}, {1, 2}, {2, 0}};
        assertThat(BipartiteChecker.isBipartite(3, oddCycle)).isFalse();
        assertThat(BipartiteChecker.isBipartite(4, new int[][]{{0, 1}, {1, 2}, {2, 3}})).isTrue();
        assertThat(BipartiteChecker.isBipartite(5, new int[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}})).isTrue();
        assertThat(BipartiteChecker.isBipartite(3, new int[0][])).isTrue();
        List<List<Integer>> sides = BipartiteChecker.sides(4, evenCycle);
        assertThat(sides.get(0)).containsExactly(0, 2);
        assertThat(sides.get(1)).containsExactly(1, 3);
        assertThat(BipartiteChecker.sides(3, oddCycle)).isEmpty();
    }

    @Test
    void shouldMatchOddCycleOracleOnRandomGraphs() {
        Random random = new Random(8008);
        for (int round = 0; round < 200; round++) {
            int n = 3 + random.nextInt(6);
            int m = random.nextInt(n + 3);
            int[][] edges = new int[m][2];
            for (int i = 0; i < m; i++) {
                edges[i][0] = random.nextInt(n);
                edges[i][1] = random.nextInt(n);
                if (edges[i][0] == edges[i][1]) {
                    edges[i][1] = (edges[i][1] + 1) % n;
                }
            }
            boolean expected = bruteBipartite(n, edges);
            assertThat(BipartiteChecker.isBipartite(n, edges))
                    .as("round=%d n=%d", round, n)
                    .isEqualTo(expected);
            List<List<Integer>> sides = BipartiteChecker.sides(n, edges);
            if (expected) {
                assertThat(sides).hasSize(2);
                long covered = sides.get(0).size() + sides.get(1).size();
                assertThat(covered).isEqualTo(n);
            }
        }
    }

    @Test
    void shouldFailFastOnSelfLoopAndOutOfRange() {
        assertThatThrownBy(() -> BipartiteChecker.isBipartite(2, new int[][]{{1, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BipartiteChecker.isBipartite(2, new int[][]{{0, 5}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BipartiteChecker.isBipartite(0, new int[0][]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：Kőnig——存在合法 2 染色 ⇔ 二分（全 2^n 染色枚举）。 */
    private static boolean bruteBipartite(int n, int[][] edges) {
        for (int mask = 0; mask < (1 << n); mask++) {
            boolean ok = true;
            for (int[] edge : edges) {
                if (((mask >> edge[0]) & 1) == ((mask >> edge[1]) & 1)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return true;
            }
        }
        return false;
    }
}
