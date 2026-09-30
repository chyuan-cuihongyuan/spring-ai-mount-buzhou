package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BinaryFuseFilter 契约测试（spec 10003 / X10008）：零假阴性圣像 +
 * 8 位指纹假阳性上界 + 规模三档（小/中/大段长）+ fail-fast + 确定性。
 */
class BinaryFuseFilterTest {

    @Test
    void shouldFindAllAddedKeysSmallSet() {
        long[] keys = new long[500];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = i * 7919L;
        }
        BinaryFuseFilter filter = new BinaryFuseFilter(keys);
        for (long key : keys) {
            assertThat(filter.mightContain(key)).isTrue();
        }
        assertThat(filter.size()).isEqualTo(500);
    }

    @Test
    void shouldFindAllAddedKeysAcrossSegmentTiers() {
        for (int n : new int[]{100, 3000, 70_000}) {
            long[] keys = new long[n];
            Set<Long> truth = new HashSet<>();
            Random random = new Random(n);
            while (truth.size() < n) {
                truth.add(random.nextLong());
            }
            int i = 0;
            for (long key : truth) {
                keys[i++] = key;
            }
            BinaryFuseFilter filter = new BinaryFuseFilter(keys);
            for (long key : keys) {
                assertThat(filter.mightContain(key)).as("n=%d 键必命中", n).isTrue();
            }
        }
    }

    @Test
    void shouldBoundFalsePositiveRate() {
        long[] keys = new long[2000];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = -4_000_000_000L + i * 104729L;
        }
        BinaryFuseFilter filter = new BinaryFuseFilter(keys);
        int falsePositives = 0;
        for (long i = 0; i < 50_000; i++) {
            if (filter.mightContain(8_000_000_000L + i * 40503L)) {
                falsePositives++;
            }
        }
        assertThat((double) falsePositives / 50_000).isLessThan(0.01);
    }

    @Test
    void shouldFailFastOnDuplicatesAndEmpty() {
        assertThatThrownBy(() -> new BinaryFuseFilter(new long[]{5L, 5L}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BinaryFuseFilter(new long[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BinaryFuseFilter(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBeDeterministicForSameKeySet() {
        long[] keys = new long[300];
        Random random = new Random(11L);
        for (int i = 0; i < keys.length; i++) {
            keys[i] = random.nextLong();
        }
        BinaryFuseFilter first = new BinaryFuseFilter(keys);
        BinaryFuseFilter second = new BinaryFuseFilter(keys);
        for (long key : keys) {
            assertThat(first.mightContain(key)).isEqualTo(second.mightContain(key));
        }
    }
}
