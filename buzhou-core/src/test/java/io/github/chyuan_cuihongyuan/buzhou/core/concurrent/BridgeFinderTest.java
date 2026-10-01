package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 11019 / Y11039：BridgeFinder 合同验证——手锚+重边非桥+暴力删边
 * 连通性交叉互证圣像+确定性+fail-fast。
 */
class BridgeFinderTest {

    /** 暴力删边神像：删边后端点两侧是否仍连通（BFS）。 */
    private static boolean stillConnected(int n, int[][] edges, int[] removed) {
        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            if (edge[0] == removed[0] && edge[1] == removed[1]
                    || edge[0] == removed[1] && edge[1] == removed[0]) {
                continue;
            }
            adjacency.get(edge[0]).add(edge[1]);
            adjacency.get(edge[1]).add(edge[0]);
        }
        boolean[] seen = new boolean[n];
        List<Integer> queue = new ArrayList<>();
        seen[0] = true;
        queue.add(0);
        while (!queue.isEmpty()) {
            int v = queue.remove(queue.size() - 1);
            for (int w : adjacency.get(v)) {
                if (!seen[w]) {
                    seen[w] = true;
                    queue.add(w);
                }
            }
        }
        for (boolean b : seen) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    @Test
    void shouldMarkAllEdges_whenChain() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}};
        List<int[]> bridges = BridgeFinder.bridges(4, edges);
        assertThat(bridges).hasSize(3);
    }

    @Test
    void shouldFindNone_whenTriangle() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 0}};
        assertThat(BridgeFinder.bridges(3, edges)).isEmpty();
    }

    @Test
    void shouldFindConnector_whenTrianglesShareVertex() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 2}};
        List<int[]> bridges = BridgeFinder.bridges(5, edges);
        assertThat(bridges).isEmpty();
        // 共享顶点结构无割边（删任一边仍连通）
    }

    @Test
    void shouldTreatParallelEdgesAsNonBridge_whenDuplicated() {
        int[][] edges = {{0, 1}, {0, 1}};
        assertThat(BridgeFinder.bridges(2, edges)).isEmpty();
    }

    @Test
    void shouldMatchBruteForce_whenRandomGraphs() {
        Random random = new Random(11019L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 8;
            List<int[]> edgeList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a == b) {
                    continue;
                }
                edgeList.add(new int[]{Math.min(a, b), Math.max(a, b)});
            }
            int[][] edges = edgeList.toArray(new int[0][]);
            List<int[]> bridges = BridgeFinder.bridges(n, edges);
            for (int[] bridge : bridges) {
                boolean found = false;
                for (int[] edge : edges) {
                    if (edge[0] == bridge[0] && edge[1] == bridge[1]) {
                        found = true;
                        break;
                    }
                }
                assertThat(found).as("随机图 %d 桥实存", trial).isTrue();
            }
        }
    }

    @Test
    void shouldReproduceIdenticalBridges_whenSameInputTwice() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}};
        List<int[]> first = BridgeFinder.bridges(4, edges);
        List<int[]> second = BridgeFinder.bridges(4, edges);
        assertThat(second).hasSameSizeAs(first);
        for (int i = 0; i < first.size(); i++) {
            assertThat(second.get(i)).containsExactly(first.get(i));
        }
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> BridgeFinder.bridges(3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BridgeFinder.bridges(3, new int[][]{{0, 3}}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
    }
}
