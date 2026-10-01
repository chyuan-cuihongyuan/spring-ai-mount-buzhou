package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10033 / X10067：KShortestPaths 合同验证——菱形三路手锚+k 超量如数返回
 * +随机图首路与 DijkstraShortestPath 交叉互证+路径合法性与权重和自洽圣像
 * +确定性+fail-fast。
 */
class KShortestPathsTest {

    /** 菱形+中跨图（0→3）：路径 0-1-3(w2)/0-1-2-3(w4)/0-2-3(w4)。 */
    private static final int[][] DIAMOND = {
            {0, 1, 1}, {1, 3, 1}, {0, 2, 2}, {2, 3, 2}, {1, 2, 1}};

    @Test
    void shouldOrderPathsByWeightThenLexicographic_whenDiamondGraph() {
        List<KShortestPaths.Path> paths = KShortestPaths.kShortest(4, DIAMOND, 0, 3, 3);
        assertThat(paths).hasSize(3);
        assertThat(paths.get(0).totalWeight()).isEqualTo(2L);
        assertThat(paths.get(0).nodes()).containsExactly(0, 1, 3);
        // 权重并列 4——字典序 [0,1,2,3] 先于 [0,2,3]
        assertThat(paths.get(1).nodes()).containsExactly(0, 1, 2, 3);
        assertThat(paths.get(1).totalWeight()).isEqualTo(4L);
        assertThat(paths.get(2).nodes()).containsExactly(0, 2, 3);
        assertThat(paths.get(2).totalWeight()).isEqualTo(4L);
    }

    @Test
    void shouldReturnAllExisting_whenKExceedsPathCount() {
        List<KShortestPaths.Path> paths = KShortestPaths.kShortest(4, DIAMOND, 0, 3, 10);
        assertThat(paths).hasSize(3);
    }

    @Test
    void shouldMatchDijkstraFirstPath_whenRandomGraphs() {
        Random random = new Random(10033L);
        for (int trial = 0; trial < 30; trial++) {
            int n = 10;
            int[][] edges = new int[20][3];
            for (int[] edge : edges) {
                edge[0] = random.nextInt(n);
                edge[1] = random.nextInt(n);
                edge[2] = 1 + random.nextInt(9);
            }
            DijkstraShortestPath dijkstra = new DijkstraShortestPath(n);
            for (int[] edge : edges) {
                dijkstra.addEdge(edge[0], edge[1], edge[2]);
            }
            long expected = dijkstra.distancesFrom(0)[n - 1];
            List<KShortestPaths.Path> paths =
                    KShortestPaths.kShortest(n, edges, 0, n - 1, 1);
            if (expected == -1L) {
                // DijkstraShortestPath 不可达哨兵 −1（权重非负域无歧义）
                assertThat(paths).isEmpty();
            } else {
                assertThat(paths).hasSize(1);
                assertThat(paths.get(0).totalWeight()).as("随机图 %d 首路值", trial)
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    void shouldBeValidWeightedWalks_whenRandomGraphs() {
        Random random = new Random(10034L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 12;
            int[][] edges = new int[30][3];
            for (int[] edge : edges) {
                edge[0] = random.nextInt(n);
                edge[1] = random.nextInt(n);
                edge[2] = 1 + random.nextInt(9);
            }
            List<KShortestPaths.Path> paths =
                    KShortestPaths.kShortest(n, edges, 0, n - 1, 4);
            for (int i = 1; i < paths.size(); i++) {
                assertThat(paths.get(i).totalWeight())
                        .as("随机图 %d 第 %d 条不轻于前条", trial, i)
                        .isGreaterThanOrEqualTo(paths.get(i - 1).totalWeight());
            }
            for (KShortestPaths.Path path : paths) {
                long sum = 0;
                for (int i = 0; i + 1 < path.nodes().length; i++) {
                    int from = path.nodes()[i];
                    int to = path.nodes()[i + 1];
                    int best = Integer.MAX_VALUE;
                    for (int[] edge : edges) {
                        if (edge[0] == from && edge[1] == to) {
                            best = Math.min(best, edge[2]);
                        }
                    }
                    assertThat(best).as("边 %d→%d 实存", from, to).isLessThan(Integer.MAX_VALUE);
                    sum += best;
                }
                assertThat(sum).isEqualTo(path.totalWeight());
            }
        }
    }

    @Test
    void shouldReproduceIdenticalPaths_whenSameInputTwice() {
        List<KShortestPaths.Path> first = KShortestPaths.kShortest(4, DIAMOND, 0, 3, 3);
        List<KShortestPaths.Path> second = KShortestPaths.kShortest(4, DIAMOND, 0, 3, 3);
        assertThat(second).hasSameSizeAs(first);
        for (int i = 0; i < first.size(); i++) {
            assertThat(second.get(i).totalWeight()).isEqualTo(first.get(i).totalWeight());
            assertThat(second.get(i).nodes()).isEqualTo(first.get(i).nodes());
        }
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        assertThatThrownBy(() -> KShortestPaths.kShortest(4, null, 0, 3, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KShortestPaths.kShortest(4, DIAMOND, 0, 3, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("k 为正");
        assertThatThrownBy(() -> KShortestPaths.kShortest(4, DIAMOND, 2, 2, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("源汇同点");
        assertThatThrownBy(() -> KShortestPaths.kShortest(
                4, new int[][]{{0, 1, -1}, {1, 3, 1}}, 0, 3, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("权重非负");
    }
}
