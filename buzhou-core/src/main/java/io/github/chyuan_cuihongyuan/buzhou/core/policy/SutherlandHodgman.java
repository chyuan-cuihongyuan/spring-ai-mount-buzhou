package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * 多边形裁剪（spec 10013 / X10027 / impl 2416）——Sutherland–Hodgman
 * 1974 思想（「逐凸边切割重连」——OpenGL scissor/游戏引擎几何管
 * 线同源）：**多边形对凸裁剪窗逐边扫割——每边按内侧/外侧四情形
 * 保留/交点插入，输出级联缩腔**——凸窗多边形裁剪的标准直接法。
 * 裁剪窗须凸且 CCW（凸性 fail-fast——凹窗需 Weiler–Atherton 另立）；
 * 完全在外返回空表（契约明示非异常）；null/顶点数不足 fail-fast；
 * 同输入同输出确定。
 */
public final class SutherlandHodgman {

    /** 凸性与坐标非退化判定容差。 */
    private static final double CONVEXITY_TOLERANCE = 1e-12;

    private SutherlandHodgman() {
    }

    /**
     * 裁剪（polygon 顶点序任意方向；clipWindow 凸 CCW 顶点表）。
     *
     * @throws IllegalArgumentException null/窗顶点 <3/窗非凸或非 CCW
     */
    public static List<double[]> clip(double[][] polygon, double[][] clipWindow) {
        validateWindow(clipWindow);
        if (polygon == null || polygon.length == 0) {
            throw new IllegalArgumentException("被裁多边形非空非 null");
        }
        List<double[]> clipped = new ArrayList<>();
        for (double[] v : polygon) {
            clipped.add(v.clone());
        }
        int m = clipWindow.length;
        for (int e = 0; e < m && !clipped.isEmpty(); e++) {
            double[] a = clipWindow[e];
            double[] b = clipWindow[(e + 1) % m];
            List<double[]> input = clipped;
            clipped = new ArrayList<>();
            for (int i = 0; i < input.size(); i++) {
                double[] current = input.get(i);
                double[] next = input.get((i + 1) % input.size());
                boolean currentIn = inside(current, a, b);
                boolean nextIn = inside(next, a, b);
                if (currentIn) {
                    clipped.add(current);
                    if (!nextIn) {
                        clipped.add(intersection(current, next, a, b));
                    }
                } else if (nextIn) {
                    clipped.add(intersection(current, next, a, b));
                }
            }
        }
        // 相邻重复点去重（级联裁剪的角点双发瑕疵——连续两裁剪边各发一次角）
        List<double[]> output = new ArrayList<>();
        for (double[] p : clipped) {
            if (output.isEmpty() || !samePoint(output.get(output.size() - 1), p)) {
                output.add(p);
            }
        }
        if (output.size() > 1 && samePoint(output.get(0), output.get(output.size() - 1))) {
            output.remove(output.size() - 1);
        }
        return output;
    }

    private static boolean samePoint(double[] p, double[] q) {
        return Math.abs(p[0] - q[0]) <= CONVEXITY_TOLERANCE
                && Math.abs(p[1] - q[1]) <= CONVEXITY_TOLERANCE;
    }

    /** 点在边 a→b 左侧（CCW 窗内侧）。 */
    private static boolean inside(double[] p, double[] a, double[] b) {
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -CONVEXITY_TOLERANCE;
    }

    /** 线段与裁剪边直线交点。 */
    private static double[] intersection(double[] p1, double[] p2, double[] a, double[] b) {
        double dx1 = p2[0] - p1[0];
        double dy1 = p2[1] - p1[1];
        double dx2 = b[0] - a[0];
        double dy2 = b[1] - a[1];
        double denom = dx1 * dy2 - dy1 * dx2;
        double t = ((a[0] - p1[0]) * dy2 - (a[1] - p1[1]) * dx2) / denom;
        return new double[]{p1[0] + t * dx1, p1[1] + t * dy1};
    }

    /** 裁剪窗凸且 CCW 校验（叉号同号）。 */
    private static void validateWindow(double[][] window) {
        if (window == null || window.length < 3) {
            throw new IllegalArgumentException("裁剪窗顶点≥3 且非 null（实际 "
                    + (window == null ? "null" : window.length) + "）");
        }
        int sign = 0;
        int m = window.length;
        for (int i = 0; i < m; i++) {
            double[] o = window[i];
            double[] u = window[(i + 1) % m];
            double[] w = window[(i + 2) % m];
            double cross = (u[0] - o[0]) * (w[1] - u[1]) - (u[1] - o[1]) * (w[0] - u[0]);
            if (Math.abs(cross) < CONVEXITY_TOLERANCE) {
                continue;
            }
            int current = cross > 0 ? 1 : -1;
            if (sign == 0) {
                sign = current;
            } else if (sign != current) {
                throw new IllegalArgumentException("裁剪窗须凸且 CCW（顶点 " + (i + 1) + " 处绕向翻转）");
            }
        }
        if (sign < 0) {
            throw new IllegalArgumentException("裁剪窗须 CCW（实际 CW——请反转顶点序）");
        }
    }
}
