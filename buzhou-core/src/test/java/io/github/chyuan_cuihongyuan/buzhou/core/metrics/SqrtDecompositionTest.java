package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7002：SqrtDecomposition 合同——分块摘要区间和。
 * 随机操作 vs 朴素扫列圣像逐步全等；块界三档（n=1/恰整块/
 * 整块+1）；单点/全列边界；fail-fast。
 */
class SqrtDecompositionTest {

    @Test
    void randomOpsMatchNaiveScan() {
        Random rng = new Random(7002L);
        int n = 137;
        long[] values = new long[n];
        for (int i = 0; i < n; i++) {
            values[i] = rng.nextInt(1000);
        }
        SqrtDecomposition sqrt = new SqrtDecomposition(values);
        for (int op = 0; op < 2000; op++) {
            int i = rng.nextInt(n);
            int j = rng.nextInt(n);
            int from = Math.min(i, j);
            int to = Math.max(i, j);
            if (rng.nextBoolean()) {
                long nv = rng.nextInt(1000);
                sqrt.update(i, nv);
                values[i] = nv;
            }
            long oracle = 0;
            for (int k = from; k <= to; k++) {
                oracle += values[k];
            }
            assertThat(sqrt.rangeSum(from, to))
                    .as("op %d [%d,%d]", op, from, to).isEqualTo(oracle);
        }
    }

    @Test
    void blockBoundarySizes() {
        for (int n : new int[]{1, 100, 101}) {
            long[] values = new long[n];
            SqrtDecomposition sqrt = new SqrtDecomposition(values);
            assertThat(sqrt.size()).isEqualTo(n);
            assertThat(sqrt.blockCount()).isEqualTo((n + sqrt.blockSize() - 1) / sqrt.blockSize());
            assertThat(sqrt.rangeSum(0, n - 1)).isZero();
        }
        SqrtDecomposition one = new SqrtDecomposition(new long[]{42});
        assertThat(one.blockSize()).isEqualTo(1);
        assertThat(one.rangeSum(0, 0)).isEqualTo(42);
    }

    @Test
    void readbackAndShapeReadouts() {
        SqrtDecomposition sqrt = new SqrtDecomposition(new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        assertThat(sqrt.get(8)).isEqualTo(9);
        assertThat(sqrt.rangeSum(0, 8)).isEqualTo(45);
        sqrt.update(0, 91);
        assertThat(sqrt.get(0)).isEqualTo(91);
        assertThat(sqrt.rangeSum(0, 1)).isEqualTo(93);
        assertThat(sqrt.rangeSum(8, 8)).isEqualTo(9);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new SqrtDecomposition(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SqrtDecomposition(new long[0])).isInstanceOf(IllegalArgumentException.class);
        SqrtDecomposition sqrt = new SqrtDecomposition(new long[]{1, 2, 3});
        assertThatThrownBy(() -> sqrt.update(3, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sqrt.get(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sqrt.rangeSum(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sqrt.rangeSum(0, 3)).isInstanceOf(IllegalArgumentException.class);
    }
}
