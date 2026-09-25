package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6016：DictionaryEncoding 合同——低基数列字典+变窄
 * 下标双层编码。往返全等+首次出现序字典+位宽公式+压缩率
 * +fail-fast。
 */
class DictionaryEncodingTest {

    @Test
    void lowCardinalityRoundTrip() {
        Random rng = new Random(6016L);
        long[] column = new long[300];
        for (int i = 0; i < column.length; i++) {
            column[i] = rng.nextInt(5) * 1000L;
        }
        DictionaryEncoding encoded = DictionaryEncoding.encode(column);
        assertThat(encoded.decode()).containsExactly(column);
        assertThat(encoded.size()).isEqualTo(300);
        assertThat(encoded.distinctCount()).isEqualTo(5);
        assertThat(encoded.indexBitWidth()).isEqualTo(3);
        Set<Long> expected = new LinkedHashSet<>();
        for (long v : column) {
            expected.add(v);
        }
        assertThat(encoded.dictionary())
                .containsExactly(expected.stream().mapToLong(Long::longValue).toArray());
    }

    @Test
    void singleValueColumnUsesZeroWidth() {
        DictionaryEncoding encoded = DictionaryEncoding.encode(new long[]{7, 7, 7});
        assertThat(encoded.distinctCount()).isEqualTo(1);
        assertThat(encoded.indexBitWidth()).isZero();
        assertThat(encoded.decode()).containsExactly(7L, 7, 7);
    }

    @Test
    void allDistinctStillRoundTrips() {
        long[] column = {5, 3, 9, 1, 5, 9, 5};
        DictionaryEncoding encoded = DictionaryEncoding.encode(column);
        assertThat(encoded.decode()).containsExactly(column);
        assertThat(encoded.distinctCount()).isEqualTo(4);
        assertThat(encoded.indexBitWidth()).isEqualTo(2);
        assertThat(encoded.dictionary()).containsExactly(5L, 3, 9, 1);
    }

    @Test
    void indicesCompressColumn() {
        long[] column = new long[1000];
        java.util.Arrays.fill(column, 42L);
        column[500] = 43L;
        DictionaryEncoding encoded = DictionaryEncoding.encode(column);
        assertThat(encoded.decode()).containsExactly(column);
        assertThat(encoded.indexBitWidth()).isEqualTo(1);
        assertThat(encoded.indicesWordCount()).isEqualTo(16);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> DictionaryEncoding.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DictionaryEncoding.encode(new long[0]))
                .isInstanceOf(IllegalArgumentException.class);
        DictionaryEncoding encoded = DictionaryEncoding.encode(new long[]{1, 2});
        assertThatThrownBy(() -> encoded.valueAt(2)).isInstanceOf(IllegalArgumentException.class);
    }
}
