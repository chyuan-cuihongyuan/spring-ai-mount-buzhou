package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.TopologicalSorter.SortResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3012 / T5026：拓扑排序合同——经典 DAG 手算（字典序最小序）、
 * 边前驱先于后继性质、环诚实前缀、自环即环、孤立点全含、计数读回、
 * 越界 fail-fast、随机 DAG 性质（每边 from 先于 to + acyclic）。
 */
class TopologicalSorterTest {

    @Test
    void classicDagShouldSortInSmallestFirstOrder() {
        // 经典六点 DAG（CLRS 风格）：5→2/5→0/4→0/4→1/2→3/3→1
        TopologicalSorter sorter = new TopologicalSorter(6);
        sorter.addEdge(5, 2);
        sorter.addEdge(5, 0);
        sorter.addEdge(4, 0);
        sorter.addEdge(4, 1);
        sorter.addEdge(2, 3);
        sorter.addEdge(3, 1);
        SortResult result = sorter.sort();
        assertThat(result.acyclic()).isTrue();
        assertThat(result.order()).isEqualTo(List.of(4, 5, 0, 2, 3, 1));
    }

    @Test
    void everyEdgeShouldHavePredecessorFirst() {
        TopologicalSorter sorter = new TopologicalSorter(6);
        sorter.addEdge(5, 2);
        sorter.addEdge(5, 0);
        sorter.addEdge(4, 0);
        sorter.addEdge(4, 1);
        sorter.addEdge(2, 3);
        sorter.addEdge(3, 1);
        List<Integer> order = sorter.sort().order();
        int[] position = new int[6];
        for (int i = 0; i < order.size(); i++) {
            position[order.get(i)] = i;
        }
        assertThat(position[5]).isLessThan(position[2]);
        assertThat(position[2]).isLessThan(position[3]);
        assertThat(position[3]).isLessThan(position[1]);
        assertThat(position[4]).isLessThan(position[0]);
        assertThat(position[4]).isLessThan(position[1]);
    }

    @Test
    void cycleShouldReportHonestPrefix() {
        // 0→1→2→0 环 + 独立点 3：前缀 [3]、acyclic=false
        TopologicalSorter sorter = new TopologicalSorter(4);
        sorter.addEdge(0, 1);
        sorter.addEdge(1, 2);
        sorter.addEdge(2, 0);
        SortResult result = sorter.sort();
        assertThat(result.acyclic()).isFalse();
        assertThat(result.order()).containsExactly(3);
    }

    @Test
    void selfLoopShouldBeACycle() {
        TopologicalSorter sorter = new TopologicalSorter(2);
        sorter.addEdge(0, 0);
        sorter.addEdge(0, 1);
        SortResult result = sorter.sort();
        assertThat(result.acyclic()).isFalse();
        assertThat(result.order()).isEmpty();
    }

    @Test
    void isolatedVerticesShouldAllAppear() {
        TopologicalSorter sorter = new TopologicalSorter(4);
        SortResult result = sorter.sort();
        assertThat(result.acyclic()).isTrue();
        assertThat(result.order()).isEqualTo(List.of(0, 1, 2, 3));
    }

    @Test
    void countsShouldReadBack() {
        TopologicalSorter sorter = new TopologicalSorter(5);
        sorter.addEdge(0, 1);
        sorter.addEdge(1, 2);
        assertThat(sorter.vertexCount()).isEqualTo(5);
        assertThat(sorter.edgeCount()).isEqualTo(2);
    }

    @Test
    void outOfUniverseShouldFailFast() {
        TopologicalSorter sorter = new TopologicalSorter(3);
        assertThatThrownBy(() -> sorter.addEdge(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> sorter.addEdge(0, 3)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> new TopologicalSorter(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void randomDagShouldRespectEveryEdge() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(77);
        for (int trial = 0; trial < 20; trial++) {
            int n = 20;
            TopologicalSorter sorter = new TopologicalSorter(n);
            boolean[][] edges = new boolean[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (rng.nextDouble() < 0.2) {
                        edges[i][j] = true;
                        sorter.addEdge(i, j);
                    }
                }
            }
            SortResult result = sorter.sort();
            assertThat(result.acyclic()).as("随机 DAG 第 %d 次", trial).isTrue();
            int[] position = new int[n];
            for (int i = 0; i < n; i++) {
                position[result.order().get(i)] = i;
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (edges[i][j]) {
                        assertThat(position[i]).isLessThan(position[j]);
                    }
                }
            }
        }
    }
}
