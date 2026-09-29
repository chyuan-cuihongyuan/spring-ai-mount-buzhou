package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkylineProblemTest {

    @Test
    void shouldMatchClassicSkylineAnchor() {
        int[][] buildings = {{2, 9, 10}, {3, 7, 15}, {5, 12, 12}, {15, 20, 10}, {19, 24, 8}};
        long[][] expected = {{2, 10}, {3, 15}, {7, 12}, {12, 0}, {15, 10}, {20, 8}, {24, 0}};
        long[][] skyline = SkylineProblem.skyline(buildings).toArray(new long[0][]);
        assertThat(skyline).hasDimensions(expected.length, 2);
        for (int i = 0; i < expected.length; i++) {
            assertThat(skyline[i][0]).as("点 %d x", i).isEqualTo(expected[i][0]);
            assertThat(skyline[i][1]).as("点 %d 高", i).isEqualTo(expected[i][1]);
        }
    }

    @Test
    void shouldHandleDegenerateShapes() {
        long[][] single = SkylineProblem.skyline(new int[][]{{0, 5, 4}}).toArray(new long[0][]);
        assertThat(single).hasDimensions(2, 2);
        assertThat(single[0][0]).isEqualTo(0);
        assertThat(single[0][1]).isEqualTo(4);
        assertThat(single[1][0]).isEqualTo(5);
        assertThat(single[1][1]).isEqualTo(0);
        long[][] contained = SkylineProblem.skyline(new int[][]{{0, 10, 5}, {2, 5, 3}}).toArray(new long[0][]);
        assertThat(contained).hasDimensions(2, 2);
        long[][] adjacent = SkylineProblem.skyline(new int[][]{{0, 5, 4}, {5, 10, 4}}).toArray(new long[0][]);
        assertThat(adjacent).hasDimensions(2, 2);
        assertThat(adjacent[0][1]).isEqualTo(4);
        assertThat(adjacent[1][0]).isEqualTo(10);
        long[][] sameX = SkylineProblem.skyline(new int[][]{{0, 5, 3}, {0, 5, 6}}).toArray(new long[0][]);
        assertThat(sameX[0][1]).isEqualTo(6);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[][] buildings = {{2, 9, 10}, {3, 7, 15}};
        long[][] first = SkylineProblem.skyline(buildings).toArray(new long[0][]);
        long[][] second = SkylineProblem.skyline(buildings).toArray(new long[0][]);
        assertThat(first).isEqualTo(second);
        assertThatThrownBy(() -> SkylineProblem.skyline(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkylineProblem.skyline(new int[][]{{5, 5, 3}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkylineProblem.skyline(new int[][]{{5, 3, 3}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkylineProblem.skyline(new int[][]{{0, 5, 0}}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
