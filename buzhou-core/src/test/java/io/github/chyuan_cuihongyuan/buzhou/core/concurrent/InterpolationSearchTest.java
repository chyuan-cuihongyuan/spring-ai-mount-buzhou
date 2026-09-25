package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6018：InterpolationSearch 合同——分布感知内插探测。
 * 均匀/偏斜/全等分布正确性 vs binarySearch 圣像；缺席
 * -1；fail-fast。
 */
class InterpolationSearchTest {

    @Test
    void uniformDistributionFindsAll() {
        long[] array = new long[1000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i * 10;
        }
        for (int i = 0; i < array.length; i++) {
            int idx = InterpolationSearch.search(array, array[i]);
            assertThat(idx).as("key %d", array[i]).isGreaterThanOrEqualTo(0);
            assertThat(array[idx]).isEqualTo(array[i]);
        }
        assertThat(InterpolationSearch.search(array, 5)).isEqualTo(-1);
        assertThat(InterpolationSearch.search(array, -1)).isEqualTo(-1);
        assertThat(InterpolationSearch.search(array, 10_000)).isEqualTo(-1);
    }

    @Test
    void matchesBinarySearchOracleOnRandomArrays() {
        Random rng = new Random(6018L);
        for (int trial = 0; trial < 50; trial++) {
            long[] array = new long[200];
            long value = rng.nextLong(50);
            for (int i = 0; i < array.length; i++) {
                value += rng.nextInt(3);
                array[i] = value;
            }
            for (int q = 0; q < 50; q++) {
                long key = rng.nextLong(array[array.length - 1] + 20) - 5;
                int expected = Arrays.binarySearch(array, key);
                int actual = InterpolationSearch.search(array, key);
                if (expected >= 0) {
                    assertThat(actual).as("trial %d key %d 在位", trial, key)
                            .isGreaterThanOrEqualTo(0);
                    assertThat(array[actual]).isEqualTo(key);
                } else {
                    assertThat(actual).as("trial %d key %d 缺席", trial, key).isEqualTo(-1);
                }
            }
        }
    }

    @Test
    void allEqualWindowTerminates() {
        long[] array = {7, 7, 7, 7};
        assertThat(InterpolationSearch.search(array, 7)).isGreaterThanOrEqualTo(0);
        assertThat(InterpolationSearch.search(array, 8)).isEqualTo(-1);
        assertThat(InterpolationSearch.search(array, 6)).isEqualTo(-1);
        assertThat(InterpolationSearch.search(new long[0], 1)).isEqualTo(-1);
        assertThat(InterpolationSearch.search(new long[]{42}, 42)).isZero();
    }

    @Test
    void extremeValuesNoOverflow() {
        long[] array = {Long.MIN_VALUE, 0, Long.MAX_VALUE};
        assertThat(InterpolationSearch.search(array, Long.MAX_VALUE)).isEqualTo(2);
        assertThat(InterpolationSearch.search(array, Long.MIN_VALUE)).isZero();
        assertThat(InterpolationSearch.search(array, 0)).isEqualTo(1);
        assertThat(InterpolationSearch.search(array, -1)).isEqualTo(-1);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> InterpolationSearch.search(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
