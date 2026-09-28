package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7043：LargestRectangleHistogram 合同——单调栈最大
 * 矩形。经典手锚；随机 vs 暴力圣像；单柱/全等高/递增；
 * fail-fast。
 */
class LargestRectangleHistogramTest {

    @Test
    void classicHandAnchors() {
        assertThat(LargestRectangleHistogram.maxRectangle(new long[]{2, 1, 5, 6, 2, 3})).isEqualTo(10L);
        assertThat(LargestRectangleHistogram.maxRectangle(new long[]{2, 4})).isEqualTo(4L);
        assertThat(LargestRectangleHistogram.maxRectangle(new long[]{6, 2, 5, 4, 5, 1, 6})).isEqualTo(12L);
    }

    @Test
    void degenerateShapes() {
        assertThat(LargestRectangleHistogram.maxRectangle(new long[]{7})).isEqualTo(7L);
        assertThat(LargestRectangleHistogram.maxRectangle(new long[]{3, 3, 3, 3})).isEqualTo(12L);
        assertThat(LargestRectangleHistogram.maxRectangle(new long[]{1, 2, 3, 4, 5})).isEqualTo(9L);
        assertThat(LargestRectangleHistogram.maxRectangle(new long[]{5, 4, 3, 2, 1})).isEqualTo(9L);
    }

    @Test
    void randomSetsMatchBruteForce() {
        Random rng = new Random(7043L);
        for (int round = 0; round < 300; round++) {
            int n = 1 + rng.nextInt(60);
            long[] heights = new long[n];
            for (int i = 0; i < n; i++) {
                heights[i] = rng.nextInt(50);
            }
            long fast = LargestRectangleHistogram.maxRectangle(heights);
            long brute = 0;
            for (int i = 0; i < n; i++) {
                long min = Long.MAX_VALUE;
                for (int j = i; j < n; j++) {
                    min = Math.min(min, heights[j]);
                    brute = Math.max(brute, min * (j - i + 1));
                }
            }
            assertThat(fast).as("round %d n=%d", round, n).isEqualTo(brute);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> LargestRectangleHistogram.maxRectangle(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LargestRectangleHistogram.maxRectangle(new long[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
