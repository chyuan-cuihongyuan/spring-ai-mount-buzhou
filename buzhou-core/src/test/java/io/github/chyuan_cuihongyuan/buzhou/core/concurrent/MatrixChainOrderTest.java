package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 11021 / Y11043：MatrixChainOrder 合同验证——CLRS 手锚+双矩阵直乘
 * +随机小链暴力枚举全切分圣像+确定性+fail-fast。
 */
class MatrixChainOrderTest {

    /** 暴力枚举全切分最小代价神像。 */
    private static long bruteForce(int[] dims, int i, int j) {
        if (j == i) {
            return 0;
        }
        long best = Long.MAX_VALUE;
        for (int k = i; k < j; k++) {
            long candidate = bruteForce(dims, i, k) + bruteForce(dims, k + 1, j)
                    + (long) dims[i - 1] * dims[k] * dims[j];
            best = Math.min(best, candidate);
        }
        return best;
    }

    @Test
    void shouldMatchClrsHandAnchor_whenSevenMatrices() {
        // CLRS 经典例：p=[30,35,15,5,10,20,25] → 15125
        assertThat(MatrixChainOrder.minMultiplications(new int[]{30, 35, 15, 5, 10, 20, 25}))
                .isEqualTo(15125L);
    }

    @Test
    void shouldBeDirectProduct_whenTwoMatrices() {
        assertThat(MatrixChainOrder.minMultiplications(new int[]{10, 20, 30}))
                .isEqualTo(6000L);
    }

    @Test
    void shouldMatchBruteForce_whenRandomSmallChains() {
        Random random = new Random(11021L);
        for (int trial = 0; trial < 30; trial++) {
            int n = 3 + random.nextInt(4);
            int[] dims = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                dims[i] = 1 + random.nextInt(20);
            }
            assertThat(MatrixChainOrder.minMultiplications(dims))
                    .as("随机链 %d 与暴力枚举全等", trial)
                    .isEqualTo(bruteForce(dims, 1, n));
        }
    }

    @Test
    void shouldReproduceIdenticalCost_whenSameInputTwice() {
        int[] dims = {5, 10, 3, 12, 5, 50, 6};
        assertThat(MatrixChainOrder.minMultiplications(dims))
                .isEqualTo(MatrixChainOrder.minMultiplications(dims));
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> MatrixChainOrder.minMultiplications(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MatrixChainOrder.minMultiplications(new int[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("≥3");
        assertThatThrownBy(() -> MatrixChainOrder.minMultiplications(new int[]{0, 5, 1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("维度为正");
    }
}
