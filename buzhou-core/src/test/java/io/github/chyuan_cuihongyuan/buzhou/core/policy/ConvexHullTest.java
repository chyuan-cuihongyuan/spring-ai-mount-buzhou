package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7011：ConvexHull 合同——monotone chain 严格凸包。
 * 方形/菱形/共线手锚；随机点集凸性+包含性质钉住；重复点
 * 幂等；退化；fail-fast。
 */
class ConvexHullTest {

    @Test
    void squareAndDiamondHandAnchors() {
        long[][] square = {{0, 0}, {0, 2}, {2, 0}, {2, 2}, {1, 1}};
        long[][] hull = ConvexHull.convexHull(square);
        assertThat(hull.length).isEqualTo(4);
        assertThat(hull[0]).containsExactly(0L, 0L);
        assertThat(hull[1]).containsExactly(2L, 0L);
        assertThat(hull[2]).containsExactly(2L, 2L);
        assertThat(hull[3]).containsExactly(0L, 2L);

        long[][] diamond = {{0, 1}, {1, 0}, {1, 2}, {2, 1}};
        assertThat(ConvexHull.convexHull(diamond).length).isEqualTo(4);
    }

    @Test
    void collinearPointsReduceToSegment() {
        long[][] line = {{0, 0}, {1, 1}, {2, 2}, {3, 3}};
        long[][] hull = ConvexHull.convexHull(line);
        assertThat(hull.length).isEqualTo(2);
        assertThat(hull[0]).containsExactly(0L, 0L);
        assertThat(hull[1]).containsExactly(3L, 3L);
    }

    @Test
    void randomSetsAreConvexAndContainAll() {
        Random rng = new Random(7011L);
        for (int round = 0; round < 200; round++) {
            int n = 3 + rng.nextInt(40);
            long[][] points = new long[n][];
            for (int i = 0; i < n; i++) {
                points[i] = new long[]{rng.nextInt(21) - 10, rng.nextInt(21) - 10};
            }
            long[][] hull = ConvexHull.convexHull(points);
            assertThat(hull.length).as("round %d", round).isGreaterThanOrEqualTo(3);
            int m = hull.length;
            for (int i = 0; i < m; i++) {
                long[] a = hull[i];
                long[] b = hull[(i + 1) % m];
                long[] c = hull[(i + 2) % m];
                long cross = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
                assertThat(cross).as("逆时针凸性 round %d", round).isGreaterThan(0);
            }
            for (long[] p : points) {
                assertThat(pointInHull(hull, p)).as("点包含 %s", java.util.Arrays.toString(p)).isTrue();
            }
        }
    }

    @Test
    void duplicatesIdempotentAndFailFast() {
        long[][] dup = {{0, 0}, {2, 0}, {2, 2}, {0, 0}, {2, 2}, {0, 2}};
        assertThat(ConvexHull.convexHull(dup).length).isEqualTo(4);
        long[][] two = {{5, 5}, {1, 1}};
        assertThat(ConvexHull.convexHull(two).length).isEqualTo(2);
        assertThatThrownBy(() -> ConvexHull.convexHull(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ConvexHull.convexHull(new long[][]{{1, 1}}).length).isEqualTo(1);
        assertThatThrownBy(() -> ConvexHull.convexHull(new long[][]{{2_000_000_000L, 0}, {0, 0}, {1, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 凸包包含判定（边界算内）。 */
    private boolean pointInHull(long[][] hull, long[] p) {
        int m = hull.length;
        for (int i = 0; i < m; i++) {
            long[] a = hull[i];
            long[] b = hull[(i + 1) % m];
            long cross = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]);
            if (cross < 0) {
                return false;
            }
        }
        return true;
    }
}
