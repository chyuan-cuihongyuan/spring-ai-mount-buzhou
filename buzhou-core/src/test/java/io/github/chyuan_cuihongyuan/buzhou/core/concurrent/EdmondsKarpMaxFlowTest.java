package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EdmondsKarpMaxFlowTest {

    @Test
    void shouldMatchClassicFlowAnchor() {
        // 经典锚：菱形网 0→1(3), 0→2(2), 1→2(1), 1→3(2), 2→3(3)——最大流 5
        int[][] edges = {{0, 1, 3}, {0, 2, 2}, {1, 2, 1}, {1, 3, 2}, {2, 3, 3}};
        assertThat(EdmondsKarpMaxFlow.maxFlow(4, edges, 0, 3)).isEqualTo(5);
        // 平行边合并语义：两条 0→1 各 2 等价一条 4
        assertThat(EdmondsKarpMaxFlow.maxFlow(2, new int[][]{{0, 1, 2}, {0, 1, 2}}, 0, 1)).isEqualTo(4);
    }

    @Test
    void shouldHandleDegenerateShapes() {
        assertThat(EdmondsKarpMaxFlow.maxFlow(2, new int[][]{}, 0, 1)).isZero();
        // 源被割断：瓶颈边 0→1 容量 1
        assertThat(EdmondsKarpMaxFlow.maxFlow(3, new int[][]{{0, 1, 1}, {1, 2, 5}}, 0, 2)).isEqualTo(1);
        // 汇不可达（方向断路）
        assertThat(EdmondsKarpMaxFlow.maxFlow(3, new int[][]{{1, 2, 5}, {1, 0, 5}}, 0, 2)).isZero();
        // 反向回流量：0→1(1), 0→2(1), 2→1(1), 1→3(2)——需要 1→3 收两口
        assertThat(EdmondsKarpMaxFlow.maxFlow(4,
                new int[][]{{0, 1, 1}, {0, 2, 1}, {2, 1, 1}, {1, 3, 2}}, 0, 3)).isEqualTo(2);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[][] edges = {{0, 1, 3}, {0, 2, 2}, {1, 3, 2}, {2, 3, 3}};
        assertThat(EdmondsKarpMaxFlow.maxFlow(4, edges, 0, 3))
                .isEqualTo(EdmondsKarpMaxFlow.maxFlow(4, edges, 0, 3));
        assertThatThrownBy(() -> EdmondsKarpMaxFlow.maxFlow(1, new int[][]{}, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EdmondsKarpMaxFlow.maxFlow(2, new int[][]{}, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EdmondsKarpMaxFlow.maxFlow(2, new int[][]{{0, 2, 1}}, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EdmondsKarpMaxFlow.maxFlow(2, new int[][]{{0, 1, -1}}, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAgreeWithDinicOnRandomGraphs() {
        // 互证圣像：EK 与 Dinic（spec 8006）对随机图流值全等
        Random random = new Random(7);
        for (int t = 0; t < 60; t++) {
            int n = 2 + random.nextInt(7);
            int[][] es = new int[random.nextInt(18)][];
            for (int i = 0; i < es.length; i++) {
                es[i] = new int[]{random.nextInt(n), random.nextInt(n), 1 + random.nextInt(9)};
            }
            long ek = EdmondsKarpMaxFlow.maxFlow(n, es, 0, n - 1);
            long dinic = DinicMaxFlow.maxFlow(n, es, 0, n - 1);
            assertThat(ek).as("随机图 %d 流值 EK=Dinic", t).isEqualTo(dinic);
        }
    }
}
