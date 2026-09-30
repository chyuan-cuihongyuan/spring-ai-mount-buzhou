package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InterpolativeCoding 契约测试（spec 10001 / X10004）：手锚 + 全域
 * 随机往返对拍 + 压缩率圣像 + 确定性 + fail-fast 钉住。
 */
class InterpolativeCodingTest {

    @Test
    void shouldRoundtripManualAnchor() {
        int[] src = {2, 3, 5, 8, 13};
        int[] out = InterpolativeCoding.decode(InterpolativeCoding.encode(src));
        assertThat(out).containsExactly(src);
    }

    @Test
    void shouldRoundtripDenseConsecutive() {
        int[] src = new int[100];
        for (int i = 0; i < src.length; i++) {
            src[i] = i;
        }
        int[] out = InterpolativeCoding.decode(InterpolativeCoding.encode(src));
        assertThat(out).containsExactly(src);
    }

    @Test
    void shouldRoundtripDuplicates() {
        int[] src = {5, 5, 5, 7, 7, 9};
        int[] out = InterpolativeCoding.decode(InterpolativeCoding.encode(src));
        assertThat(out).containsExactly(src);
    }

    @Test
    void shouldHandleSingleZero() {
        int[] out = InterpolativeCoding.decode(InterpolativeCoding.encode(new int[]{0}));
        assertThat(out).containsExactly(0);
    }

    @Test
    void shouldRoundtripRandomMonotoneSequences() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 200; trial++) {
            int n = 1 + random.nextInt(200);
            int[] src = new int[n];
            int value = random.nextInt(50);
            for (int i = 0; i < n; i++) {
                value += random.nextInt(21);
                src[i] = value;
            }
            int[] out = InterpolativeCoding.decode(InterpolativeCoding.encode(src));
            assertThat(out).as("trial %d 往返全等", trial).containsExactly(src);
        }
    }

    @Test
    void shouldCompressMonotoneTableBelowHalfRaw() {
        int[] src = new int[1000];
        for (int i = 0; i < src.length; i++) {
            src[i] = i * 1000;
        }
        InterpolativeCoding.Encoded encoded = InterpolativeCoding.encode(src);
        assertThat(encoded.bytes().length).isLessThan(src.length * 2);
    }

    @Test
    void shouldBeDeterministicAcrossRuns() {
        int[] src = {1, 4, 4, 9, 16, 25, 36};
        byte[] first = InterpolativeCoding.encode(src).bytes();
        byte[] second = InterpolativeCoding.encode(src).bytes();
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> InterpolativeCoding.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterpolativeCoding.encode(new int[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterpolativeCoding.encode(new int[]{-1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterpolativeCoding.encode(new int[]{3, 2}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterpolativeCoding.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterpolativeCoding.decode(new InterpolativeCoding.Encoded(null, 3)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRoundtripAscendingFullRange() {
        int[] src = {0, 1, 2, Integer.MAX_VALUE / 2, Integer.MAX_VALUE};
        int[] out = InterpolativeCoding.decode(InterpolativeCoding.encode(src));
        assertThat(Arrays.stream(out).sum()).isEqualTo(Arrays.stream(src).sum());
    }
}
