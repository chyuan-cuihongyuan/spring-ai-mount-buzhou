package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoruvkaMstTest {

    @Test
    void shouldMatchClassicMstAnchor() {
        // 经典锚：K4 权 {0-1:1, 0-2:2, 0-3:3, 1-2:4, 1-3:5, 2-3:6}——MST = 1+2+3 = 6
        int[][] k4 = {{0, 1, 1}, {0, 2, 2}, {0, 3, 3}, {1, 2, 4}, {1, 3, 5}, {2, 3, 6}};
        assertThat(BoruvkaMst.totalWeight(4, k4)).isEqualTo(6);
        List<long[]> tree = BoruvkaMst.minimumSpanningTree(4, k4);
        assertThat(tree).hasSize(3);
        // 单节点零边
        assertThat(BoruvkaMst.minimumSpanningTree(1, new int[][]{})).isEmpty();
    }

    @Test
    void shouldHandleEqualWeightsDeterministically() {
        // 等权三角：任两边皆 MST——确定性承诺（双跑同输出）
        int[][] triangle = {{0, 1, 5}, {1, 2, 5}, {0, 2, 5}};
        List<long[]> first = BoruvkaMst.minimumSpanningTree(3, triangle);
        List<long[]> second = BoruvkaMst.minimumSpanningTree(3, triangle);
        assertThat(first).hasSize(2);
        assertThat(java.util.Arrays.deepToString(first.toArray()))
                .isEqualTo(java.util.Arrays.deepToString(second.toArray()));
        assertThat(BoruvkaMst.totalWeight(3, triangle)).isEqualTo(10);
    }

    @Test
    void shouldBeFailFast() {
        assertThatThrownBy(() -> BoruvkaMst.minimumSpanningTree(0, new int[][]{}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoruvkaMst.minimumSpanningTree(2, new int[][]{{0, 0, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoruvkaMst.minimumSpanningTree(2, new int[][]{{0, 2, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoruvkaMst.minimumSpanningTree(3, new int[][]{{0, 1, 1}}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不连通");
    }

    @Test
    void shouldAgreeWithKruskalOnRandomGraphs() {
        // 互证圣像：与 KruskalMst（spec 7008）同图总权全等；异权图边集全等（MST 唯一）
        Random random = new Random(31);
        for (int t = 0; t < 50; t++) {
            int n = 2 + random.nextInt(8);
            List<int[]> es = new java.util.ArrayList<>();
            // 先串成连通骨架，再撒异权额外边
            int distinct = 1;
            for (int v = 1; v < n; v++) {
                es.add(new int[]{random.nextInt(v), v, distinct});
                distinct += 1 + random.nextInt(5);
            }
            int extra = random.nextInt(12);
            for (int i = 0; i < extra; i++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n);
                if (u != v) {
                    es.add(new int[]{u, v, distinct});
                    distinct += 1 + random.nextInt(5);
                }
            }
            int[][] edges = es.toArray(new int[0][]);
            KruskalMst kruskal = new KruskalMst(n);
            for (int[] e : edges) {
                kruskal.addEdge(e[0], e[1], e[2]);
            }
            assertThat(BoruvkaMst.totalWeight(n, edges))
                    .as("随机图 %d 总权 Boruvka=Kruskal", t)
                    .isEqualTo(kruskal.totalWeight());
            // 异权 MST 唯一：边集（无向键）全等
            TreeSet<String> boruvkaKeys = new TreeSet<>();
            for (long[] e : BoruvkaMst.minimumSpanningTree(n, edges)) {
                boruvkaKeys.add(Math.min((int) e[0], (int) e[1]) + "-" + Math.max((int) e[0], (int) e[1]));
            }
            TreeSet<String> kruskalKeys = new TreeSet<>();
            for (long[] e : kruskal.minimumSpanningTree()) {
                // KruskalMst 边格式 [weight, 小端, 大端]
                kruskalKeys.add(e[1] + "-" + e[2]);
            }
            assertThat(boruvkaKeys).isEqualTo(kruskalKeys);
        }
    }
}
