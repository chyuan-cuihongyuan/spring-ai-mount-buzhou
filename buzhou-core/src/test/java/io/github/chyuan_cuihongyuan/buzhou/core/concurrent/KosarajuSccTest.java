package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10030 / X10061：KosarajuScc 合同验证——CLRS 22.5 手锚与
 * TarjanSccFinder 交叉互证（同域不同面的算法互审）+孤立点/自环平凡面
 * +随机图分区相等圣像+确定性+fail-fast。
 */
class KosarajuSccTest {

    /** 分区规范化（分量内升序+分量表字典序）——与算法输出序解耦的比较口径。 */
    private static List<List<Integer>> canonical(List<List<Integer>> components) {
        List<List<Integer>> sorted = new ArrayList<>();
        for (List<Integer> component : components) {
            List<Integer> copy = new ArrayList<>(component);
            copy.sort(Comparator.naturalOrder());
            sorted.add(copy);
        }
        sorted.sort(Comparator.comparing(List::toString));
        return sorted;
    }

    @Test
    void shouldMatchClrsHandAnchorAndTarjanPartition_whenClassicGraph() {
        // CLRS 22.5 图 22.9：a=0 b=1 c=2 d=3 e=4 f=5 g=6 h=7
        List<int[]> edges = List.of(
                new int[]{0, 1}, new int[]{1, 4}, new int[]{4, 0},
                new int[]{1, 5}, new int[]{5, 6}, new int[]{6, 5},
                new int[]{2, 6}, new int[]{2, 3}, new int[]{3, 2},
                new int[]{3, 7}, new int[]{7, 3});
        List<List<Integer>> components = KosarajuScc.components(8, edges);
        assertThat(canonical(components)).isEqualTo(canonical(List.of(
                List.of(0, 1, 4), List.of(2, 3, 7), List.of(5, 6))));
        TarjanSccFinder tarjan = new TarjanSccFinder(8);
        for (int[] edge : edges) {
            tarjan.addEdge(edge[0], edge[1]);
        }
        assertThat(canonical(components)).isEqualTo(canonical(tarjan.components()));
    }

    @Test
    void shouldMakeSingletons_whenNoEdges() {
        List<List<Integer>> components = KosarajuScc.components(5, List.of());
        assertThat(components).containsExactly(
                List.of(0), List.of(1), List.of(2), List.of(3), List.of(4));
    }

    @Test
    void shouldKeepSingletons_whenAllSelfLoops() {
        List<int[]> edges = List.of(
                new int[]{0, 0}, new int[]{1, 1}, new int[]{2, 2});
        assertThat(KosarajuScc.components(3, edges)).containsExactly(
                List.of(0), List.of(1), List.of(2));
    }

    @Test
    void shouldMatchTarjanPartition_whenRandomGraphs() {
        Random random = new Random(10030L);
        for (int trial = 0; trial < 30; trial++) {
            int n = 40;
            List<int[]> edges = new ArrayList<>();
            for (int i = 0; i < 120; i++) {
                edges.add(new int[]{random.nextInt(n), random.nextInt(n)});
            }
            TarjanSccFinder tarjan = new TarjanSccFinder(n);
            for (int[] edge : edges) {
                tarjan.addEdge(edge[0], edge[1]);
            }
            assertThat(canonical(KosarajuScc.components(n, edges)))
                    .as("随机图 %d 分区与 Tarjan 相等", trial)
                    .isEqualTo(canonical(tarjan.components()));
        }
    }

    @Test
    void shouldReproduceIdenticalOutput_whenSameInputTwice() {
        List<int[]> edges = List.of(
                new int[]{0, 1}, new int[]{1, 2}, new int[]{2, 0},
                new int[]{2, 3}, new int[]{3, 4}, new int[]{4, 3});
        assertThat(KosarajuScc.components(5, edges))
                .isEqualTo(KosarajuScc.components(5, edges));
        assertThat(KosarajuScc.components(5, edges)).containsExactly(
                List.of(0, 1, 2), List.of(3, 4));
    }

    @Test
    void shouldFailFast_whenNullEdgesOrOutOfRange() {
        assertThatThrownBy(() -> KosarajuScc.components(3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KosarajuScc.components(3, List.of(new int[]{0, 3})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
        assertThatThrownBy(() -> KosarajuScc.components(3, List.of(new int[]{0})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("int[2]");
        assertThatThrownBy(() -> KosarajuScc.components(-1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
