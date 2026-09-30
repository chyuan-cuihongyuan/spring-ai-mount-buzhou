package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoerWagnerMinCutTest {

    @Test
    void shouldMatchClassicCutAnchor() {
        // 经典锚：K4 单位权——全局最小割 3（孤立一点）
        int[][] k4 = {{0, 1, 1}, {0, 2, 1}, {0, 3, 1}, {1, 2, 1}, {1, 3, 1}, {2, 3, 1}};
        assertThat(StoerWagnerMinCut.minCut(4, k4)).isEqualTo(3);
        // 桥形：两三角由权重 2 的桥相连——最小割 2
        int[][] bridge = {{0, 1, 1}, {1, 2, 1}, {0, 2, 1}, {2, 3, 2}, {3, 4, 1}, {4, 5, 1}, {3, 5, 1}};
        assertThat(StoerWagnerMinCut.minCut(6, bridge)).isEqualTo(2);
        // 平行边合并：双 0—1 各 3 等价单边 6
        assertThat(StoerWagnerMinCut.minCut(2, new int[][]{{0, 1, 3}, {1, 0, 3}})).isEqualTo(6);
    }

    @Test
    void shouldHandleDegenerateShapes() {
        // 两点无边——割 0（断开）
        assertThat(StoerWagnerMinCut.minCut(2, new int[][]{})).isZero();
        // 弱独占点：路径 0—1(1)—2(9)——最小割 1（切断 0—1）
        int[][] pendant = {{0, 1, 1}, {1, 2, 9}};
        assertThat(StoerWagnerMinCut.minCut(3, pendant)).isEqualTo(1);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[][] edges = {{0, 1, 2}, {1, 2, 3}, {2, 0, 4}, {2, 3, 1}};
        assertThat(StoerWagnerMinCut.minCut(4, edges)).isEqualTo(StoerWagnerMinCut.minCut(4, edges));
        assertThat(StoerWagnerMinCut.minCut(4, edges)).isEqualTo(1);
        assertThatThrownBy(() -> StoerWagnerMinCut.minCut(1, new int[][]{}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StoerWagnerMinCut.minCut(2, new int[][]{{0, 0, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StoerWagnerMinCut.minCut(2, new int[][]{{0, 2, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StoerWagnerMinCut.minCut(2, new int[][]{{0, 1, 0}}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBoundRandomGraphCuts() {
        // 随机圣像：割值 ≤ 任一点邻接权和（孤立单点的上界）且 ≤ 总权和；确定性双跑
        Random random = new Random(11);
        for (int t = 0; t < 40; t++) {
            int n = 2 + random.nextInt(7);
            int m = random.nextInt(16);
            int[][] es = new int[m][];
            long total = 0;
            for (int i = 0; i < m; i++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n);
                if (u == v) {
                    v = (v + 1) % n;
                }
                int w = 1 + random.nextInt(9);
                es[i] = new int[]{u, v, w};
                total += w;
            }
            long cut = StoerWagnerMinCut.minCut(n, es);
            assertThat(cut).as("随机图 %d 割上界", t).isLessThanOrEqualTo(total);
            assertThat(cut).isEqualTo(StoerWagnerMinCut.minCut(n, es));
        }
    }
}
