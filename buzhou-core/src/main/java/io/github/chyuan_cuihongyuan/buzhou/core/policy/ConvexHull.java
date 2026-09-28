package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 凸包（spec 7011 / U7223 / impl 2263）——Andrew 1979
 * monotone chain 思想：**按 (x,y) 排序后上下链各扫一遍**
 * O(n log n)——两两枚举判断 O(n³)（点数放大不可承受）的
 * 病解。严格凸包（共线边界点不保留——面最小化）；退化：
  * 全共线返回两端点；重复点幂等。叉积 long 域——坐标
 * 绝对值 >10^9 fail-fast（乘积溢出诚实拒绝，不做浮点）。
 * 输出逆时针、自最左最低点起（同点集同序列完全确定）。
 *
 * <p>与 KdTree（同包）同族不同面：最近邻查询面 vs 外壳
 * 包络面；与 HilbertCurve 不同面：排序局部性 vs 几何凸性。
 */
public final class ConvexHull {

    private static final long COORD_LIMIT = 1_000_000_000L;

    private ConvexHull() {
    }

    /** 严格凸包（逆时针，最左最低点起；共线不保留；null fail-fast）。 */
    public static long[][] convexHull(long[][] points) {
        if (points == null) {
            throw new IllegalArgumentException("点集非空");
        }
        for (long[] p : points) {
            if (p == null || p.length != 2) {
                throw new IllegalArgumentException("点须为 [x,y] 二元组");
            }
            if (Math.abs(p[0]) > COORD_LIMIT || Math.abs(p[1]) > COORD_LIMIT) {
                throw new IllegalArgumentException("坐标越 ±10^9 安全域: " + Arrays.toString(p));
            }
        }
        long[][] unique = dedup(points);
        if (unique.length < 3) {
            Arrays.sort(unique, (a, b) -> a[0] != b[0]
                    ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
            return unique;
        }
        Arrays.sort(unique, (a, b) -> a[0] != b[0]
                ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        if (cross(unique[0], unique[unique.length - 1], unique[1]) == 0
                && cross(unique[0], unique[unique.length - 1],
                        unique[unique.length - 2]) == 0
                && cross(unique[0], unique[1], unique[unique.length - 1]) == 0) {
            return new long[][]{unique[0], unique[unique.length - 1]};
        }
        int n = unique.length;
        long[] half = new long[2 * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            while (k >= 2 && cross(unique[(int) half[k - 2]], unique[(int) half[k - 1]],
                    unique[i]) <= 0) {
                k--;
            }
            half[k++] = i;
        }
        int lower = k;
        for (int i = n - 2; i >= 0; i--) {
            while (k > lower && cross(unique[(int) half[k - 2]], unique[(int) half[k - 1]],
                    unique[i]) <= 0) {
                k--;
            }
            half[k++] = i;
        }
        List<long[]> hull = new ArrayList<>();
        for (int i = 0; i < k - 1; i++) {
            hull.add(unique[(int) half[i]]);
        }
        return hull.toArray(new long[0][]);
    }

    /** 叉积 (b−a)×(c−a)。 */
    private static long cross(long[] a, long[] b, long[] c) {
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
    }

    private static long[][] dedup(long[][] points) {
        long[][] sorted = points.clone();
        Arrays.sort(sorted, (a, b) -> a[0] != b[0]
                ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        List<long[]> out = new ArrayList<>();
        for (long[] p : sorted) {
            if (out.isEmpty() || out.get(out.size() - 1)[0] != p[0]
                    || out.get(out.size() - 1)[1] != p[1]) {
                out.add(p.clone());
            }
        }
        return out.toArray(new long[0][]);
    }
}
