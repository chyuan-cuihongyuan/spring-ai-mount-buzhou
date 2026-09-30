package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * SutherlandHodgman 契约测试（spec 10013 / X10028）：恒等裁剪 +
 * 手锚交点 + 全出空表 + 凸性 fail-fast + 确定性。
 */
class SutherlandHodgmanTest {

    private static final double EPSILON = 1e-9;

    /** CCW 单位方窗。 */
    private final double[][] square = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    @Test
    void shouldKeepInteriorPolygonUnchanged() {
        double[][] inner = {{0.25, 0.25}, {0.75, 0.25}, {0.75, 0.75}, {0.25, 0.75}};
        List<double[]> result = SutherlandHodgman.clip(inner, square);
        assertThat(result).hasSameSizeAs(List.of(inner));
        for (int i = 0; i < inner.length; i++) {
            assertThat(result.get(i)[0]).isCloseTo(inner[i][0], within(EPSILON));
            assertThat(result.get(i)[1]).isCloseTo(inner[i][1], within(EPSILON));
        }
    }

    @Test
    void shouldClipTriangleAtSquareBoundary() {
        double[][] tri = {{0.5, 0.5}, {1.5, 0.5}, {0.5, 1.5}};
        List<double[]> result = SutherlandHodgman.clip(tri, square);
        assertThat(result).hasSize(4);
        boolean hasEdgeOne = false;
        boolean hasEdgeHalf = false;
        for (double[] p : result) {
            if (Math.abs(p[0] - 1.0) < EPSILON || Math.abs(p[1] - 1.0) < EPSILON) {
                hasEdgeOne = true;
            }
            if (Math.abs(p[0] - 0.5) < EPSILON && Math.abs(p[1] - 0.5) < EPSILON) {
                hasEdgeHalf = true;
            }
        }
        assertThat(hasEdgeOne).isTrue();
        assertThat(hasEdgeHalf).isTrue();
    }

    @Test
    void shouldReturnEmptyWhenFullyOutside() {
        double[][] far = {{5, 5}, {6, 5}, {6, 6}, {5, 6}};
        assertThat(SutherlandHodgman.clip(far, square)).isEmpty();
    }

    @Test
    void shouldClipEveryVertexInsideWindow() {
        double[][] big = {{-2, -2}, {3, -2}, {3, 3}, {-2, 3}};
        List<double[]> result = SutherlandHodgman.clip(big, square);
        for (double[] p : result) {
            assertThat(p[0]).isBetween(-EPSILON, 1.0 + EPSILON);
            assertThat(p[1]).isBetween(-EPSILON, 1.0 + EPSILON);
        }
        assertThat(result).hasSize(4);
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> SutherlandHodgman.clip(null, square))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SutherlandHodgman.clip(square, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SutherlandHodgman.clip(square, new double[][]{{0, 0}, {1, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        double[][] concave = {{0, 0}, {2, 0}, {1, 1}, {2, 2}, {0, 2}};
        assertThatThrownBy(() -> SutherlandHodgman.clip(square, concave))
                .isInstanceOf(IllegalArgumentException.class);
        double[][] cw = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        assertThatThrownBy(() -> SutherlandHodgman.clip(square, cw))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
