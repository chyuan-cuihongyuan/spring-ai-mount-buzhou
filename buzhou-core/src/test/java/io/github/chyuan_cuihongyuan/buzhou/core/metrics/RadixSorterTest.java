package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RadixSorterTest {

    @Test
    void shouldMatchHandAnchors() {
        assertThat(RadixSorter.sort(new long[]{5, 3, 8, 1, 9, 2}))
                .containsExactly(1L, 2L, 3L, 5L, 8L, 9L);
        assertThat(RadixSorter.sort(new long[]{7, 7, 7})).containsExactly(7L, 7L, 7L);
        assertThat(RadixSorter.sort(new long[]{1, 2, 3})).containsExactly(1L, 2L, 3L);
        assertThat(RadixSorter.sort(new long[]{3, 2, 1})).containsExactly(1L, 2L, 3L);
        assertThat(RadixSorter.sort(new long[0])).isEmpty();
        assertThat(RadixSorter.sort(new long[]{42})).containsExactly(42L);
        assertThat(RadixSorter.sort(new long[]{Long.MAX_VALUE, 0})).containsExactly(0L, Long.MAX_VALUE);
    }

    @Test
    void shouldMatchArraysSortOracleOnRandomInputs() {
        Random random = new Random(8022);
        for (int round = 0; round < 300; round++) {
            int length = random.nextInt(200);
            long[] values = new long[length];
            for (int i = 0; i < length; i++) {
                values[i] = random.nextLong() & Long.MAX_VALUE;
            }
            long[] copy = Arrays.copyOf(values, values.length);
            long[] expected = Arrays.copyOf(values, values.length);
            Arrays.sort(expected);
            long[] sorted = RadixSorter.sort(values);
            assertThat(sorted).as("round=%d", round).isEqualTo(expected);
            assertThat(values).isEqualTo(copy);
        }
    }

    @Test
    void shouldFailFastOnNegativeAndNull() {
        assertThatThrownBy(() -> RadixSorter.sort(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RadixSorter.sort(new long[]{1, -2, 3}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
