package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * 轨迹抽稀（spec 10014 / X10029 / impl 2417）——Douglas–Peucker
 * 1973 思想（「最大垂距递归二分」——Mapbox/GDAL/postgis simplify
 * 同源）：**首末锚定，找垂距最大点——超容差则分裂递归、否则整段
 * 收缩为两端**——保留形状特征的折线抽稀（逐点均匀抽稀丢拐点的
 * 病解）。端点必保留契约；输出为输入子集契约；null/点数 <2/负
 * 容差 fail-fast；同输入同输出确定。
 */
public final class DouglasPeucker {

    private DouglasPeucker() {
    }

    /**
     * 抽稀（容差=垂距阈值，单位与坐标同）。
     *
     * @throws IllegalArgumentException null/点数 <2/容差为负
     */
    public static List<double[]> simplify(double[][] points, double tolerance) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("点数≥2 且非 null（实际 "
                    + (points == null ? "null" : points.length) + "）");
        }
        if (!(tolerance >= 0.0) || !Double.isFinite(tolerance)) {
            throw new IllegalArgumentException("容差为非负有限（实际 " + tolerance + "）");
        }
        boolean[] keep = new boolean[points.length];
        keep[0] = true;
        keep[points.length - 1] = true;
        split(points, 0, points.length - 1, tolerance, keep);
        List<double[]> result = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            if (keep[i]) {
                result.add(points[i]);
            }
        }
        return result;
    }

    private static void split(double[][] points, int from, int to,
                              double tolerance, boolean[] keep) {
        if (to - from < 2) {
            return;
        }
        double maxDistance = -1.0;
        int maxIndex = -1;
        for (int i = from + 1; i < to; i++) {
            double distance = perpendicularDistance(points[i], points[from], points[to]);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }
        if (maxDistance > tolerance) {
            keep[maxIndex] = true;
            split(points, from, maxIndex, tolerance, keep);
            split(points, maxIndex, to, tolerance, keep);
        }
    }

    /** 点到线段两端连线的垂距（退化共线锚段退化为点距）。 */
    static double perpendicularDistance(double[] p, double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq < 1e-24) {
            double ex = p[0] - a[0];
            double ey = p[1] - a[1];
            return Math.sqrt(ex * ex + ey * ey);
        }
        double cross = Math.abs(dy * (p[0] - a[0]) - dx * (p[1] - a[1]));
        return cross / Math.sqrt(lengthSq);
    }
}
