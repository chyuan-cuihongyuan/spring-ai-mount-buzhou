package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * MarchingSquares 契约测试（spec 10016 / X10034）：单角手锚 + 全
 * 阈值无线 + 斜坡等值线位置 + 鞍 casos + fail-fast + 确定性。
 */
class MarchingSquaresTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldEmitSingleSegmentForSingleCornerPeak() {
        double[][] grid = {{1.0, 0.0}, {0.0, 0.0}};
        List<double[]> segments = MarchingSquares.contour(grid, 0.5);
        assertThat(segments).hasSize(1);
        double[] seg = segments.get(0);
        assertThat(seg[0]).isCloseTo(0.0, within(EPSILON));
        assertThat(seg[1]).isCloseTo(0.5, within(EPSILON));
        assertThat(seg[2]).isCloseTo(0.5, within(EPSILON));
        assertThat(seg[3]).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    void shouldEmitNothingWhenAllAboveOrBelow() {
        double[][] allAbove = {{1.0, 1.0}, {1.0, 1.0}};
        assertThat(MarchingSquares.contour(allAbove, 0.5)).isEmpty();
        double[][] allBelow = {{0.0, 0.0}, {0.0, 0.0}};
        assertThat(MarchingSquares.contour(allBelow, 0.5)).isEmpty();
    }

    @Test
    void shouldPlaceIsoLineOnLinearSlope() {
        double[][] grid = {{0.0, 2.0}, {0.0, 2.0}};
        List<double[]> segments = MarchingSquares.contour(grid, 1.0);
        assertThat(segments).hasSize(1);
        double[] seg = segments.get(0);
        assertThat(seg[0]).isCloseTo(0.5, within(EPSILON));
        assertThat(seg[2]).isCloseTo(0.5, within(EPSILON));
    }

    @Test
    void shouldSplitSaddleCaseIntoTwoSegments() {
        double[][] saddle = {{1.0, 0.0}, {0.0, 1.0}};
        List<double[]> segments = MarchingSquares.contour(saddle, 0.5);
        assertThat(segments).hasSize(2);
    }

    @Test
    void shouldBeDeterministicAcrossRuns() {
        double[][] grid = {{0.2, 0.8, 0.4}, {0.9, 0.1, 0.6}, {0.3, 0.7, 0.5}};
        List<double[]> first = MarchingSquares.contour(grid, 0.5);
        List<double[]> second = MarchingSquares.contour(grid, 0.5);
        assertThat(first).usingRecursiveFieldByFieldElementComparator().isEqualTo(second);
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> MarchingSquares.contour(null, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MarchingSquares.contour(new double[][]{{1.0}}, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MarchingSquares.contour(
                new double[][]{{1.0, 2.0}, {3.0}}, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
