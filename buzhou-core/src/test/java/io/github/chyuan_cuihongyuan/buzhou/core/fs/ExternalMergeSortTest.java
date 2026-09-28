package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6031：ExternalMergeSort 合同——内存窗切块排序+多路
 * 归并。Arrays.sort 圣像全等；游程数公式；单块/重复值；
 * fail-fast。
 */
class ExternalMergeSortTest {

    @Test
    void sortedOutputMatchesArraysSortOracle() {
        Random rng = new Random(6031L);
        for (int trial = 0; trial < 20; trial++) {
            long[] data = new long[250];
            for (int i = 0; i < data.length; i++) {
                data[i] = rng.nextLong(10_000);
            }
            long[] expected = data.clone();
            Arrays.sort(expected);
            ExternalMergeSort sorter = new ExternalMergeSort(data, 64);
            assertThat(sorter.sorted()).as("trial %d", trial).containsExactly(expected);
            assertThat(sorter.runCount()).as("trial %d 游程数", trial).isEqualTo(4);
        }
    }

    @Test
    void runCountFormula() {
        assertThat(new ExternalMergeSort(new long[100], 30).runCount()).isEqualTo(4);
        assertThat(new ExternalMergeSort(new long[90], 30).runCount()).isEqualTo(3);
        assertThat(new ExternalMergeSort(new long[30], 30).runCount()).isEqualTo(1);
        assertThat(new ExternalMergeSort(new long[0], 30).runCount()).isZero();
    }

    @Test
    void singleChunkAndDuplicates() {
        long[] data = {5, 5, 3, 3, 9, 1};
        ExternalMergeSort sorter = new ExternalMergeSort(data, 100);
        assertThat(sorter.sorted()).containsExactly(1, 3, 3, 5, 5, 9);
        assertThat(sorter.runCount()).isEqualTo(1);
        assertThat(sorter.chunkSize()).isEqualTo(100);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new ExternalMergeSort(null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalMergeSort(new long[]{1}, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
