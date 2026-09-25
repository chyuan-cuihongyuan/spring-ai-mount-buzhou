package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6004 / T6210：MonotonicDeque 合同——固定窗最大值
 * 摊还 O(1)。全程暴力扫圣像；升/降/全等边界；越窗弹出；
 * 双实例确定性；fail-fast。
 */
class MonotonicDequeTest {

    private static long bruteMax(long[] history, int from, int to) {
        long max = history[from];
        for (int i = from + 1; i <= to; i++) {
            max = Math.max(max, history[i]);
        }
        return max;
    }

    @Test
    void streamingMaxMatchesBruteForceOracle() {
        MonotonicDeque window = new MonotonicDeque(7);
        Random rng = new Random(6004L);
        long[] history = new long[500];
        for (int i = 0; i < history.length; i++) {
            history[i] = rng.nextLong(10_000);
            window.offer(history[i]);
            int from = Math.max(0, i - 6);
            assertThat(window.max()).as("step %d", i).isEqualTo(bruteMax(history, from, i));
            assertThat(window.size()).isLessThanOrEqualTo(7);
        }
    }

    @Test
    void ascendingInputKeepsOnlyLatest() {
        MonotonicDeque window = new MonotonicDeque(3);
        for (long v = 1; v <= 5; v++) {
            window.offer(v);
        }
        assertThat(window.max()).isEqualTo(5);
        assertThat(window.size()).isEqualTo(1);
    }

    @Test
    void descendingInputKeepsFullWindow() {
        MonotonicDeque window = new MonotonicDeque(3);
        for (long v = 5; v >= 1; v--) {
            window.offer(v);
        }
        assertThat(window.max()).isEqualTo(3);
        assertThat(window.size()).isEqualTo(3);
    }

    @Test
    void equalValuesCollapseAndWindowEvictionAnchored() {
        MonotonicDeque window = new MonotonicDeque(4);
        for (int i = 0; i < 9; i++) {
            window.offer(7);
            assertThat(window.max()).isEqualTo(7);
        }
        assertThat(window.size()).isEqualTo(1);
        window.offer(6);
        assertThat(window.max()).as("窗内仍有三个 7——6 不改最大").isEqualTo(7);
        assertThat(window.size()).isEqualTo(2);
        window.offer(5);
        assertThat(window.max()).isEqualTo(7);
        window.offer(5);
        assertThat(window.max()).isEqualTo(7);
        assertThat(window.size()).isEqualTo(3);
        window.offer(5);
        assertThat(window.max()).as("最后一个 7 越窗——6 成为最大").isEqualTo(6);
        assertThat(window.size()).isEqualTo(2);
        window.offer(5);
        assertThat(window.max()).as("6 越窗——5 成为最大").isEqualTo(5);
        assertThat(window.size()).isEqualTo(1);
    }

    @Test
    void twoInstancesSameOpsSameMaxSequence() {
        MonotonicDeque a = new MonotonicDeque(5);
        MonotonicDeque b = new MonotonicDeque(5);
        Random rng = new Random(77L);
        for (int i = 0; i < 300; i++) {
            long v = rng.nextLong(100);
            a.offer(v);
            b.offer(v);
            assertThat(b.max()).isEqualTo(a.max());
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new MonotonicDeque(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MonotonicDeque(-3)).isInstanceOf(IllegalArgumentException.class);
        MonotonicDeque window = new MonotonicDeque(2);
        assertThat(window.max()).isNull();
        assertThat(window.isEmpty()).isTrue();
    }
}
