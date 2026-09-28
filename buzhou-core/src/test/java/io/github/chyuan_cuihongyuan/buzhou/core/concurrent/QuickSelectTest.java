package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7033：QuickSelect 合同——BFPRT 确定性第 k 小。
 * 随机序列 vs 排序圣像全 k 逐位全等；原数组不动（副本
 * 语义）；确定性双跑全等；fail-fast。
 */
class QuickSelectTest {

    @Test
    void randomSequencesMatchSortedOracle() {
        Random rng = new Random(7033L);
        for (int round = 0; round < 200; round++) {
            int n = 1 + rng.nextInt(150);
            long[] values = new long[n];
            for (int i = 0; i < n; i++) {
                values[i] = rng.nextInt(500);
            }
            long[] sorted = values.clone();
            java.util.Arrays.sort(sorted);
            long[] untouched = values.clone();
            for (int k = 0; k < n; k++) {
                assertThat(QuickSelect.select(values, k))
                        .as("round %d k=%d", round, k).isEqualTo(sorted[k]);
                assertThat(values).as("原数组不动").containsExactly(untouched);
            }
        }
    }

    @Test
    void duplicatesAndDeterminism() {
        long[] values = {5, 1, 5, 5, 2, 5};
        assertThat(QuickSelect.select(values, 0)).isEqualTo(1L);
        assertThat(QuickSelect.select(values, 1)).isEqualTo(2L);
        for (int k = 2; k <= 5; k++) {
            assertThat(QuickSelect.select(values, k)).isEqualTo(5L);
        }
        for (int run = 0; run < 5; run++) {
            assertThat(QuickSelect.select(values, 3)).isEqualTo(5L);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> QuickSelect.select(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuickSelect.select(new long[]{1, 2}, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuickSelect.select(new long[]{1, 2}, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
