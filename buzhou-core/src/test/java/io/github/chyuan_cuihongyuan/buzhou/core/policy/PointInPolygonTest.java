package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7013：PointInPolygon 合同——射线法奇内偶外。
 * 方形/凹多边形手锚；边界=内语义；顶点对齐半开规则；
 * fail-fast。
 */
class PointInPolygonTest {

    private static final long[][] SQUARE = {
            {0, 0}, {10, 0}, {10, 10}, {0, 10}};

    private static final long[][] CONCAVE = {
            {0, 0}, {10, 0}, {10, 10}, {5, 5}, {0, 10}};

    @Test
    void squareInsideOutsideAndBoundary() {
        assertThat(PointInPolygon.contains(SQUARE, 5, 5)).isTrue();
        assertThat(PointInPolygon.contains(SQUARE, 15, 5)).isFalse();
        assertThat(PointInPolygon.contains(SQUARE, -1, 5)).isFalse();
        assertThat(PointInPolygon.contains(SQUARE, 0, 5)).isTrue();
        assertThat(PointInPolygon.contains(SQUARE, 5, 0)).isTrue();
        assertThat(PointInPolygon.contains(SQUARE, 0, 0)).isTrue();
        assertThat(PointInPolygon.contains(SQUARE, 10, 10)).isTrue();
    }

    @Test
    void concaveNotchExcluded() {
        assertThat(PointInPolygon.contains(CONCAVE, 5, 2)).isTrue();
        assertThat(PointInPolygon.contains(CONCAVE, 8, 7)).isTrue();
        assertThat(PointInPolygon.contains(CONCAVE, 5, 8)).isFalse();
        assertThat(PointInPolygon.contains(CONCAVE, 5, 9)).isFalse();
        assertThat(PointInPolygon.contains(CONCAVE, 5, 6)).isFalse();
        assertThat(PointInPolygon.contains(CONCAVE, 2, 2)).isTrue();
        assertThat(PointInPolygon.contains(CONCAVE, 1, 9)).isTrue();
    }

    @Test
    void vertexAlignedRayHalfOpenRule() {
        long[][] triangle = {{0, 0}, {4, 0}, {2, 4}};
        assertThat(PointInPolygon.contains(triangle, 2, 2)).isTrue();
        assertThat(PointInPolygon.contains(triangle, 4, 2)).isFalse();
        assertThat(PointInPolygon.contains(triangle, 2, 4)).isTrue();
        assertThat(PointInPolygon.contains(triangle, 0, 2)).isFalse();
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> PointInPolygon.contains(null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PointInPolygon.contains(new long[][]{{0, 0}, {1, 1}}, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
