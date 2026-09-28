package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7009：ArticulationPoints 合同——Tarjan 低链接割点桥。
 * 路径/环/星形/双三角桥接手锚；随机图 vs 逐点删除暴力圣像；
 * 桥升序确定性；fail-fast。
 */
class ArticulationPointsTest {

    @Test
    void handAnchoredShapes() {
        ArticulationPoints path = new ArticulationPoints(4);
        for (int i = 0; i < 3; i++) {
            path.addEdge(i, i + 1);
        }
        assertThat(path.articulationPoints()).containsExactly(1, 2);

        ArticulationPoints cycle = new ArticulationPoints(4);
        for (int i = 0; i < 4; i++) {
            cycle.addEdge(i, (i + 1) % 4);
        }
        assertThat(cycle.articulationPoints()).isEmpty();

        ArticulationPoints star = new ArticulationPoints(4);
        for (int i = 1; i < 4; i++) {
            star.addEdge(0, i);
        }
        assertThat(star.articulationPoints()).containsExactly(0);
    }

    @Test
    void bridgeBetweenTwoTriangles() {
        ArticulationPoints g = new ArticulationPoints(6);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0);
        g.addEdge(3, 4);
        g.addEdge(4, 5);
        g.addEdge(5, 3);
        g.addEdge(2, 3);
        assertThat(g.articulationPoints()).containsExactly(2, 3);
        assertThat(g.bridges()).containsExactly(new long[]{2L, 3L});
    }

    @Test
    void randomGraphsMatchBruteForceOracle() {
        Random rng = new Random(7009L);
        for (int round = 0; round < 150; round++) {
            int n = 2 + rng.nextInt(12);
            ArticulationPoints g = new ArticulationPoints(n);
            Set<Long> oracleEdges = new HashSet<>();
            int edges = rng.nextInt(2 * n);
            for (int e = 0; e < edges; e++) {
                int a = rng.nextInt(n);
                int b = rng.nextInt(n);
                if (a == b) {
                    continue;
                }
                g.addEdge(a, b);
                oracleEdges.add((long) Math.min(a, b) * 100 + Math.max(a, b));
            }
            TreeSet<Integer> oracle = bruteForceCutVertices(n, oracleEdges);
            assertThat(g.articulationPoints())
                    .as("round %d n=%d", round, n)
                    .containsExactlyElementsOf(oracle);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new ArticulationPoints(0)).isInstanceOf(IllegalArgumentException.class);
        ArticulationPoints g = new ArticulationPoints(3);
        assertThatThrownBy(() -> g.addEdge(1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> g.addEdge(0, 3)).isInstanceOf(IllegalArgumentException.class);
        g.addEdge(0, 1);
        g.addEdge(0, 1);
        assertThat(g.edgeCount()).isEqualTo(1);
    }

    /** 暴力圣像：逐点删除后查连通分量数是否增加。 */
    private TreeSet<Integer> bruteForceCutVertices(int n, Set<Long> edges) {
        TreeSet<Integer> cuts = new TreeSet<>();
        int baseComponents = componentCount(n, edges, -1);
        for (int victim = 0; victim < n; victim++) {
            if (componentCount(n, edges, victim) > baseComponents) {
                cuts.add(victim);
            }
        }
        return cuts;
    }

    private int componentCount(int n, Set<Long> edges, int removed) {
        DisjointSet dsu = new DisjointSet(n);
        for (long e : edges) {
            int a = (int) (e / 100);
            int b = (int) (e % 100);
            if (a != removed && b != removed) {
                dsu.union(a, b);
            }
        }
        Set<Integer> roots = new HashSet<>();
        for (int i = 0; i < n; i++) {
            if (i != removed) {
                roots.add(dsu.find(i));
            }
        }
        return roots.size();
    }
}
