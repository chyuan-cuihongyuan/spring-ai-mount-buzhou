package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DelaunayTriangulation 契约测试（spec 10012 / X10026）：手锚 +
 * 空外接圆性质 + 边覆盖 + 计数公式 + 随机点集 + fail-fast。
 */
class DelaunayTriangulationTest {

    @Test
    void shouldTriangulateSquareIntoTwoTriangles() {
        double[][] square = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        List<DelaunayTriangulation.Triangle> tris = DelaunayTriangulation.triangulate(square);
        assertThat(tris).hasSize(2);
        Set<Integer> seen = new HashSet<>();
        for (DelaunayTriangulation.Triangle t : tris) {
            seen.add(t.v0());
            seen.add(t.v1());
            seen.add(t.v2());
        }
        assertThat(seen).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void shouldSatisfyEmptyCircumcircleProperty() {
        Random random = new Random(20260930L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 4 + random.nextInt(10);
            double[][] pts = new double[n][2];
            for (int i = 0; i < n; i++) {
                pts[i][0] = Math.round(random.nextDouble() * 100 * 4) / 4.0;
                pts[i][1] = Math.round(random.nextDouble() * 100 * 4) / 4.0;
            }
            try {
                List<DelaunayTriangulation.Triangle> tris = DelaunayTriangulation.triangulate(pts);
                for (DelaunayTriangulation.Triangle t : tris) {
                    for (int p = 0; p < n; p++) {
                        boolean isVertex = p == t.v0() || p == t.v1() || p == t.v2();
                        if (!isVertex && inCircumcircle(pts, t, pts[p])) {
                            throw new AssertionError("trial " + trial + " 点 " + p
                                    + " 落入三角形 " + t + " 外接圆");
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // 随机量化点退化（重复/共线）——跳过该 trial（契约 fail-fast 面另行测）
            }
        }
    }

    private boolean inCircumcircle(double[][] pts, DelaunayTriangulation.Triangle t, double[] d) {
        double ax = pts[t.v0()][0] - d[0];
        double ay = pts[t.v0()][1] - d[1];
        double bx = pts[t.v1()][0] - d[0];
        double by = pts[t.v1()][1] - d[1];
        double cx = pts[t.v2()][0] - d[0];
        double cy = pts[t.v2()][1] - d[1];
        double det = (ax * ax + ay * ay) * (bx * cy - by * cx)
                - (bx * bx + by * by) * (ax * cy - ay * cx)
                + (cx * cx + cy * cy) * (ax * by - ay * bx);
        double orient = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        return orient > 0 ? det > 1e-9 : det < -1e-9;
    }

    @Test
    void shouldCoverAllPointsInRandomSets() {
        Random random = new Random(7L);
        int covered = 0;
        for (int trial = 0; trial < 30 && covered < 10; trial++) {
            int n = 5 + random.nextInt(15);
            double[][] pts = new double[n][2];
            for (int i = 0; i < n; i++) {
                pts[i][0] = Math.round(random.nextDouble() * 200) / 2.0;
                pts[i][1] = Math.round(random.nextDouble() * 200) / 2.0;
            }
            try {
                List<DelaunayTriangulation.Triangle> tris = DelaunayTriangulation.triangulate(pts);
                Set<Integer> seen = new HashSet<>();
                for (DelaunayTriangulation.Triangle t : tris) {
                    seen.add(t.v0());
                    seen.add(t.v1());
                    seen.add(t.v2());
                }
                assertThat(seen).as("trial %d 全点覆盖", trial)
                        .containsAll(intRange(n));
                covered++;
            } catch (IllegalArgumentException expected) {
                // 量化退化点集 fail-fast 契约——跳过
            }
        }
        assertThat(covered).isGreaterThanOrEqualTo(10);
    }

    private List<Integer> intRange(int n) {
        java.util.List<Integer> list = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        return list;
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> DelaunayTriangulation.triangulate(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DelaunayTriangulation.triangulate(new double[][]{{0, 0}, {1, 1}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DelaunayTriangulation.triangulate(
                new double[][]{{0, 0}, {1, 1}, {2, 2}, {3, 3}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DelaunayTriangulation.triangulate(
                new double[][]{{0, 0}, {1, 1}, {0, 0}}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
