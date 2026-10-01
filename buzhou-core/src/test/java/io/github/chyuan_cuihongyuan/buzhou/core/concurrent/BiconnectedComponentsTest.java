package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 11018 / Y11037：BiconnectedComponents 合同验证——手锚块结构
 * +边集划分圣像+确定性+fail-fast。
 */
class BiconnectedComponentsTest {

    @Test
    void shouldFormSingleBlock_whenTriangle() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 0}};
        List<List<int[]>> components = BiconnectedComponents.components(3, edges);
        assertThat(components).hasSize(1);
        assertThat(components.get(0)).hasSize(3);
    }

    @Test
    void shouldFormTwoBlocks_whenTrianglesShareVertex() {
        // 三角 (0,1,2) 与三角 (2,3,4) 共享顶点 2
        int[][] edges = {{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 2}};
        List<List<int[]>> components = BiconnectedComponents.components(5, edges);
        assertThat(components).hasSize(2);
        assertThat(components.get(0)).hasSize(3);
        assertThat(components.get(1)).hasSize(3);
    }

    @Test
    void shouldMakeEdgeBlocks_whenChain() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}};
        List<List<int[]>> components = BiconnectedComponents.components(4, edges);
        assertThat(components).hasSize(3);
        for (List<int[]> component : components) {
            assertThat(component).hasSize(1);
        }
    }

    @Test
    void shouldPartitionEdges_whenRandomGraphs() {
        Random random = new Random(11018L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 10;
            Set<String> allEdges = new HashSet<>();
            List<int[]> edgeList = new ArrayList<>();
            for (int i = 0; i < 18; i++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a == b) {
                    continue;
                }
                int from = Math.min(a, b);
                int to = Math.max(a, b);
                String key = from + "-" + to;
                if (allEdges.add(key)) {
                    edgeList.add(new int[]{from, to});
                }
            }
            int[][] edges = edgeList.toArray(new int[0][]);
            List<List<int[]>> components = BiconnectedComponents.components(n, edges);
            int totalEdges = 0;
            Set<String> seen = new HashSet<>();
            for (List<int[]> component : components) {
                totalEdges += component.size();
                for (int[] edge : component) {
                    String key = Math.min(edge[0], edge[1]) + "-"
                            + Math.max(edge[0], edge[1]);
                    assertThat(seen.add(key)).as("随机图 %d 边不重复", trial).isTrue();
                }
            }
            assertThat(totalEdges).as("随机图 %d 边集划分 Σ=m", trial)
                    .isEqualTo(edges.length);
        }
    }

    @Test
    void shouldReproduceIdenticalBlocks_whenSameInputTwice() {
        int[][] edges = {{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 2}};
        List<List<int[]>> first = BiconnectedComponents.components(5, edges);
        List<List<int[]>> second = BiconnectedComponents.components(5, edges);
        assertThat(second).hasSameSizeAs(first);
        for (int i = 0; i < first.size(); i++) {
            assertThat(second.get(i)).hasSameSizeAs(first.get(i));
        }
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> BiconnectedComponents.components(3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BiconnectedComponents.components(
                3, new int[][]{{0, 3}}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
        assertThatThrownBy(() -> BiconnectedComponents.components(
                3, new int[][]{{0}}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("int[2]");
    }
}
