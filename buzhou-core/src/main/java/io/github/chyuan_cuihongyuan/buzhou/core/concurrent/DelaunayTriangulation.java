package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delaunay 三角剖分（spec 10012 / X10025 / impl 2415）——Bowyer–
 * Watson 1981 思想（「空外接圆逐点增量」——CGAL/scipy.spatial
 * Delaunay 同源）：**逐点插入——外接圆含该点的坏三角形成腔，
 * 腔边界（不被两坏三角共享的边）与新点重连**——最大化最小角的
 * 空圆性质（无点落入任意三角形外接圆）。退化契约：重复点/
 * 全共线 fail-fast（一般位置输入域明示）；超三角形虚点不出结果；
 * <n 或 null fail-fast；同输入同剖分确定（点序驱动增量——确定性
 * 面以输入序为锚）。
 */
public final class DelaunayTriangulation {

    private DelaunayTriangulation() {
    }

    /**
     * 剖分结果三角形（顶点索引三元组，逆时针序）。
     */
    public record Triangle(int v0, int v1, int v2) {
    }

    /**
     * 增量剖分（输入点集须一般位置：无重复、不全共线）。
     *
     * @throws IllegalArgumentException null/点数 <3/重复点/全共线
     */
    public static List<Triangle> triangulate(double[][] points) {
        if (points == null || points.length < 3) {
            throw new IllegalArgumentException("点数≥3 且非 null（实际 "
                    + (points == null ? "null" : points.length) + "）");
        }
        for (double[] p : points) {
            if (p == null || p.length != 2) {
                throw new IllegalArgumentException("点须二维坐标");
            }
        }
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                if (points[i][0] == points[j][0] && points[i][1] == points[j][1]) {
                    throw new IllegalArgumentException("点集无重复（索引 " + i + " 与 " + j + "）");
                }
            }
        }
        if (allCollinear(points)) {
            throw new IllegalArgumentException("点集不全共线");
        }
        double[] bounds = superBounds(points);
        int n = points.length;
        // 超三角形顶点索引 n/n+1/n+2（虚点——结果中滤除；CCW 绕向锚）
        double[][] all = Arrays.copyOf(points, n + 3);
        all[n] = new double[]{bounds[0], bounds[1]};
        all[n + 1] = new double[]{bounds[2], bounds[1]};
        all[n + 2] = new double[]{bounds[1], bounds[2]};
        List<int[]> triangles = new ArrayList<>();
        triangles.add(new int[]{n, n + 1, n + 2});
        for (int p = 0; p < n; p++) {
            List<int[]> bad = new ArrayList<>();
            Map<Long, Boolean> directedBoundary = new HashMap<>();
            for (int[] t : triangles) {
                if (inCircumcircle(all, t, points[p])) {
                    bad.add(t);
                    for (int e = 0; e < 3; e++) {
                        long fwd = edgeKey(t[e], t[(e + 1) % 3]);
                        long rev = edgeKey(t[(e + 1) % 3], t[e]);
                        if (directedBoundary.containsKey(rev)) {
                            directedBoundary.remove(rev);
                        } else {
                            directedBoundary.put(fwd, Boolean.TRUE);
                        }
                    }
                }
            }
            triangles.removeAll(bad);
            for (long edge : directedBoundary.keySet()) {
                // 有向腔边界（绕腔 CCW）→ 新三角 {a,b,p} 保持 CCW 绕向
                triangles.add(new int[]{(int) (edge >>> 32), (int) edge, p});
            }
        }
        List<Triangle> result = new ArrayList<>();
        for (int[] t : triangles) {
            if (t[0] < n && t[1] < n && t[2] < n) {
                result.add(new Triangle(t[0], t[1], t[2]));
            }
        }
        return result;
    }

    /** 点 d 是否在三角形 abc 外接圆内（行列式口径——严格内；共圆不入）。 */
    private static boolean inCircumcircle(double[][] pts, int[] t, double[] d) {
        double ax = pts[t[0]][0] - d[0];
        double ay = pts[t[0]][1] - d[1];
        double bx = pts[t[1]][0] - d[0];
        double by = pts[t[1]][1] - d[1];
        double cx = pts[t[2]][0] - d[0];
        double cy = pts[t[2]][1] - d[1];
        double det = (ax * ax + ay * ay) * (bx * cy - by * cx)
                - (bx * bx + by * by) * (ax * cy - ay * cx)
                + (cx * cx + cy * cy) * (ax * by - ay * bx);
        double signed = orientation(pts, t) > 0 ? det : -det;
        return signed > 1e-12;
    }

    /** 三角形定向（>0 逆时针）。 */
    private static double orientation(double[][] pts, int[] t) {
        double ax = pts[t[1]][0] - pts[t[0]][0];
        double ay = pts[t[1]][1] - pts[t[0]][1];
        double bx = pts[t[2]][0] - pts[t[0]][0];
        double by = pts[t[2]][1] - pts[t[0]][1];
        return ax * by - ay * bx;
    }

    private static boolean allCollinear(double[][] points) {
        double ref = (points[1][0] - points[0][0]) * (points[2][1] - points[0][1])
                - (points[1][1] - points[0][1]) * (points[2][0] - points[0][0]);
        for (int i = 2; i < points.length; i++) {
            double cross = (points[1][0] - points[0][0]) * (points[i][1] - points[0][1])
                    - (points[1][1] - points[0][1]) * (points[i][0] - points[0][0]);
            if (Math.abs(cross) > 1e-12 * Math.max(1.0, Math.abs(ref))) {
                return false;
            }
        }
        return true;
    }

    /** 超三角形覆盖域（非整偏移破格点共圆巧合—— CCW 绕向锚）。 */
    private static double[] superBounds(double[][] points) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (double[] p : points) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        double span = Math.max(maxX - minX, maxY - minY);
        return new double[]{minX - 3.7 * span - 1.3, minY - 2.9 * span - 0.7,
                maxX + 4.3 * span + 1.9, maxY + 5.1 * span + 1.1};
    }

    /** 有向边编码（起点高 32 位——腔边界绕向锚）。 */
    private static long edgeKey(int from, int to) {
        return ((long) from << 32) | to;
    }
}
