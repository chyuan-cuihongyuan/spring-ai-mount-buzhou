package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 6040：MedianFinder 合同——双堆夹逼流式中位数。
 * 流式插入中位数与排序圣像全等；奇偶两态；确定性；空安全。
 */
class MedianFinderTest {

    @Test
    void streamingMedianMatchesSortedOracle() {
        MedianFinder finder = new MedianFinder();
        Random rng = new Random(6040L);
        List<Long> seen = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            long v = rng.nextLong(10_000);
            finder.insert(v);
            seen.add(v);
            List<Long> sorted = new ArrayList<>(seen);
            Collections.sort(sorted);
            int n = sorted.size();
            long expected = (n % 2 == 1)
                    ? sorted.get(n / 2)
                    : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) >>> 1;
            assertThat(finder.median()).as("step %d", i).isEqualTo(expected);
        }
    }

    @Test
    void parityBothModes() {
        MedianFinder odd = new MedianFinder();
        odd.insert(5);
        odd.insert(1);
        odd.insert(9);
        assertThat(odd.median()).isEqualTo(5);
        MedianFinder even = new MedianFinder();
        even.insert(5);
        even.insert(1);
        even.insert(9);
        even.insert(9);
        assertThat(even.median()).isEqualTo((long) ((5 + 9) >>> 1));
    }

    @Test
    void deterministicMedianSequence() {
        long[] stream = {3, 1, 4, 1, 5, 9, 2, 6};
        MedianFinder a = new MedianFinder();
        MedianFinder b = new MedianFinder();
        List<Long> mediansA = new ArrayList<>();
        List<Long> mediansB = new ArrayList<>();
        for (long v : stream) {
            a.insert(v);
            b.insert(v);
            mediansA.add(a.median());
            mediansB.add(b.median());
        }
        assertThat(mediansB).containsExactlyElementsOf(mediansA);
        assertThat(mediansA).containsExactly(3L, 2L, 3L, 2L, 3L, 3L, 3L, 3L);
    }

    @Test
    void negativeAndExtremeValues() {
        MedianFinder finder = new MedianFinder();
        Arrays.stream(new long[]{Long.MIN_VALUE, Long.MAX_VALUE, 0}).forEach(finder::insert);
        assertThat(finder.median()).isZero();
        assertThat(finder.size()).isEqualTo(3);
    }

    @Test
    void emptyReturnsNull() {
        MedianFinder finder = new MedianFinder();
        assertThat(finder.median()).isNull();
        assertThat(finder.isEmpty()).isTrue();
        finder.insert(1);
        assertThat(finder.isEmpty()).isFalse();
    }
}
