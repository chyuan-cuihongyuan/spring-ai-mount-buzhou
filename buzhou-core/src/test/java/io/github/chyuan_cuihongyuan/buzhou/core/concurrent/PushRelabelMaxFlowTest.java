package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10032 / X10065：PushRelabelMaxFlow 合同验证——CLRS 手锚与 Dinic 交叉互证
 * +随机图（反平行/平行边）流值相等圣像+零容量断网+确定性+fail-fast。
 */
class PushRelabelMaxFlowTest {

    @Test
    void shouldMatchDinic_whenClrsHandAnchorGraph() {
        // CLRS 26.1 流网络：s=0 a=1 b=2 c=3 d=4 t=5，最大流 23
        int[][] edges = {
                {0, 1, 16}, {0, 2, 13}, {1, 3, 12}, {2, 1, 4}, {2, 4, 14},
                {3, 2, 9}, {3, 5, 20}, {4, 3, 7}, {4, 5, 4}};
        assertThat(PushRelabelMaxFlow.maxFlow(6, edges, 0, 5)).isEqualTo(23L);
        assertThat(DinicMaxFlow.maxFlow(6, edges, 0, 5)).isEqualTo(23L);
    }

    @Test
    void shouldMatchDinic_whenRandomGraphsWithParallelAndAntiparallelEdges() {
        Random random = new Random(10032L);
        for (int trial = 0; trial < 30; trial++) {
            int n = 12;
            List<int[]> edgeList = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                int from = random.nextInt(n);
                int to = random.nextInt(n);
                if (from == to) {
                    continue;
                }
                edgeList.add(new int[]{from, to, 1 + random.nextInt(20)});
            }
            // 反平行边：to→to+1 的逆向再补一条
            for (int[] edge : new ArrayList<>(edgeList)) {
                edgeList.add(new int[]{edge[1], edge[0], 1 + random.nextInt(10)});
            }
            int[][] edges = edgeList.toArray(new int[0][]);
            long pushRelabel = PushRelabelMaxFlow.maxFlow(n, edges, 0, n - 1);
            long dinic = DinicMaxFlow.maxFlow(n, edges, 0, n - 1);
            assertThat(pushRelabel).as("随机图 %d 流值与 Dinic 相等", trial).isEqualTo(dinic);
        }
    }

    @Test
    void shouldReturnZero_whenSinkDisconnectedByZeroCapacity() {
        int[][] edges = {
                {0, 1, 5}, {1, 2, 0}, {2, 3, 5}};
        assertThat(PushRelabelMaxFlow.maxFlow(4, edges, 0, 3)).isEqualTo(0L);
    }

    @Test
    void shouldReturnZero_whenSinkUnreachable() {
        int[][] edges = {
                {0, 1, 5}, {2, 3, 7}};
        assertThat(PushRelabelMaxFlow.maxFlow(4, edges, 0, 3)).isEqualTo(0L);
    }

    @Test
    void shouldReproduceIdenticalValue_whenSameInputTwice() {
        int[][] edges = {
                {0, 1, 3}, {1, 2, 2}, {0, 2, 2}, {2, 3, 4}};
        assertThat(PushRelabelMaxFlow.maxFlow(4, edges, 0, 3))
                .isEqualTo(PushRelabelMaxFlow.maxFlow(4, edges, 0, 3));
    }

    @Test
    void shouldFailFast_whenNegativeCapacityOrSameSourceSink() {
        assertThatThrownBy(() -> PushRelabelMaxFlow.maxFlow(3, new int[][]{{0, 1, -1}, {1, 2, 1}}, 0, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("容量非负");
        assertThatThrownBy(() -> PushRelabelMaxFlow.maxFlow(3, new int[][]{{0, 1, 1}}, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("源汇同点");
        assertThatThrownBy(() -> PushRelabelMaxFlow.maxFlow(2, new int[][]{{0, 5, 1}}, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越域");
        assertThatThrownBy(() -> PushRelabelMaxFlow.maxFlow(0, new int[][]{}, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
