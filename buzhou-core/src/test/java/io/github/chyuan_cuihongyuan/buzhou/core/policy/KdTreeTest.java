package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6019：KdTree 合同——交替轴中位数分割最近邻。
 * 暴力扫圣像（含平局 canonical）；退化分布；fail-fast。
 */
class KdTreeTest {

    private static long[] bruteNearest(long[][] points, long x, long y) {
        long[] best = points[0];
        long bestDist = Long.MAX_VALUE;
        for (long[] p : points) {
            long dx = p[0] - x;
            long dy = p[1] - y;
            long dist = dx * dx + dy * dy;
            if (dist < bestDist || (dist == bestDist
                    && (p[0] < best[0] || (p[0] == best[0] && p[1] < best[1])))) {
                best = p;
                bestDist = dist;
            }
        }
        return best;
    }

    @Test
    void nearestMatchesBruteForceOracle() {
        Random rng = new Random(6019L);
        long[][] points = new long[200][2];
        for (long[] p : points) {
            p[0] = rng.nextInt(1000);
            p[1] = rng.nextInt(1000);
        }
        KdTree tree = KdTree.build(points);
        for (int q = 0; q < 100; q++) {
            long x = rng.nextInt(1200) - 100;
            long y = rng.nextInt(1200) - 100;
            long[] expected = bruteNearest(points, x, y);
            KdTree.Nearest actual = tree.nearest(x, y);
            assertThat(actual.x()).as("q%d x", q).isEqualTo(expected[0]);
            assertThat(actual.y()).as("q%d y", q).isEqualTo(expected[1]);
            long dx = expected[0] - x;
            long dy = expected[1] - y;
            assertThat(actual.distanceSquared()).isEqualTo(dx * dx + dy * dy);
        }
    }

    @Test
    void degenerateCollinearDistribution() {
        long[][] points = new long[50][2];
        for (int i = 0; i < points.length; i++) {
            points[i] = new long[]{i, 42};
        }
        KdTree tree = KdTree.build(points);
        KdTree.Nearest nearest = tree.nearest(30, 0);
        assertThat(nearest.x()).isEqualTo(30);
        assertThat(nearest.y()).isEqualTo(42);
        assertThat(nearest.distanceSquared()).isEqualTo(1764L);
    }

    @Test
    void duplicatePointsTieBreakCanonical() {
        long[][] points = {{5, 5}, {5, 5}, {5, 5}};
        KdTree tree = KdTree.build(points);
        KdTree.Nearest nearest = tree.nearest(0, 0);
        assertThat(nearest.x()).isEqualTo(5);
        assertThat(nearest.y()).isEqualTo(5);
        assertThat(nearest.distanceSquared()).isEqualTo(50);
    }

    @Test
    void negativeCoordinatesWork() {
        long[][] points = {{-10, -10}, {10, 10}, {-3, 4}};
        KdTree tree = KdTree.build(points);
        assertThat(tree.nearest(-2, 3).x()).isEqualTo(-3);
        assertThat(tree.nearest(-2, 3).distanceSquared()).isEqualTo(2);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> KdTree.build(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KdTree.build(new long[0][]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KdTree.build(new long[][]{{1, 2, 3}}))
                .isInstanceOf(IllegalArgumentException.class);
        KdTree tree = KdTree.build(new long[][]{{0, 0}});
        assertThat(tree.size()).isEqualTo(1);
    }
}
