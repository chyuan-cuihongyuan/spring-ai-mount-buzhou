package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BranchAndBoundTest {

    @Test
    void shouldMatchClassicKnapsackAnchors() {
        // 经典锚：w={1,3,4,5} v={1,4,5,7} cap=7 → 9（物品 2+3：4+5≤7、5+7=12? 不——w2+w3=9>7；最优=物品1+3：3+5≤7? 8>7——
        // 枚举：{1,2}(3+4=7,9) {1,3}(3+5>7) {0,1,2}? 8>7 {0,3}=6,7 {2}=4,5 → 最优 9={1,2}
        assertThat(BranchAndBound.knapsack(new int[]{1, 3, 4, 5}, new int[]{1, 4, 5, 7}, 7)).isEqualTo(9);
        // 容量 0 / 全超重 → 0
        assertThat(BranchAndBound.knapsack(new int[]{5, 6}, new int[]{10, 10}, 0)).isZero();
        assertThat(BranchAndBound.knapsack(new int[]{5, 6}, new int[]{10, 10}, 3)).isZero();
        // 单件恰好
        assertThat(BranchAndBound.knapsack(new int[]{10}, new int[]{42}, 10)).isEqualTo(42);
        // 密度序陷阱：低密度重件更值——LP 界不会误导精确解
        assertThat(BranchAndBound.knapsack(new int[]{10, 20, 30}, new int[]{60, 100, 120}, 50)).isEqualTo(220);
    }

    @Test
    void shouldAgreeWithBruteForceOnRandomInstances() {
        // 暴力圣像：随机实例 vs 全子集枚举全等（15 件内）
        Random random = new Random(199);
        for (int t = 0; t < 120; t++) {
            int n = 1 + random.nextInt(15);
            int[] w = new int[n];
            int[] v = new int[n];
            for (int i = 0; i < n; i++) {
                w[i] = 1 + random.nextInt(20);
                v[i] = 1 + random.nextInt(30);
            }
            int capacity = random.nextInt(60);
            int expected = 0;
            for (int mask = 0; mask < (1 << n); mask++) {
                int totalW = 0;
                int totalV = 0;
                for (int i = 0; i < n; i++) {
                    if ((mask & (1 << i)) != 0) {
                        totalW += w[i];
                        totalV += v[i];
                    }
                }
                if (totalW <= capacity) {
                    expected = Math.max(expected, totalV);
                }
            }
            assertThat(BranchAndBound.knapsack(w, v, capacity)).as("实例 %d", t).isEqualTo(expected);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[] w = {2, 3, 4, 5};
        int[] v = {3, 4, 5, 6};
        assertThat(BranchAndBound.knapsack(w, v, 9))
                .isEqualTo(BranchAndBound.knapsack(w, v, 9));
        assertThatThrownBy(() -> BranchAndBound.knapsack(null, v, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BranchAndBound.knapsack(new int[]{1, 2}, new int[]{1}, 5))
                .hasMessageContaining("等长");
        assertThatThrownBy(() -> BranchAndBound.knapsack(new int[]{-1}, new int[]{1}, 5))
                .hasMessageContaining("非负");
        assertThatThrownBy(() -> BranchAndBound.knapsack(new int[]{1}, new int[]{-1}, 5))
                .hasMessageContaining("非负");
        assertThatThrownBy(() -> BranchAndBound.knapsack(w, v, -1))
                .hasMessageContaining("容量非负");
    }
}
