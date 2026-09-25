package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;

/**
 * KD-Tree 二维最近邻（spec 6019 / T6237 / impl 2219）——
 * scikit-learn/图形学 kd-tree 思想：**交替轴中位数分割**的
 * 静态空间树——查询沿目标侧下探、回溯检查分割面另一侧
 * （到轴平面距离平方 ≥ 当前最优即剪枝），最近邻 O(√n) 均
 * 摊/平均 O(log n)——每查询全点集线性扫（O(n) 放大）的
 * 病解。平局 canonical（距离同则 x 小、再 y 小——同查询
 * 同结果可回放）。long 坐标距离平方（无浮点误差）。
 *
 * <p>与 QuadTree（spec 6020）同族不同面：轴交替分割最近邻
 * vs 四象限区域查询；与 ZOrderCurve（recovery）不同面：
 * 空间填充序 vs 树形邻域查询。静态建树（查询集不变——确定性）。
 */
public final class KdTree {

    /** 最近邻结果（平局 canonical：距离同→x 小→y 小）。 */
    public record Nearest(long x, long y, long distanceSquared) {
    }

    private final long[][] points;
    private final Integer[] order;
    private final int[] axisAt;

    private KdTree(long[][] pts, Integer[] order, int[] axisAt) {
        this.points = pts;
        this.order = order;
        this.axisAt = axisAt;
    }

    /** 静态构建（null/空/行非二维 fail-fast；防御性副本）。 */
    public static KdTree build(long[][] points) {
        if (points == null || points.length == 0) {
            throw new IllegalArgumentException("点集非空");
        }
        for (long[] p : points) {
            if (p == null || p.length != 2) {
                throw new IllegalArgumentException("每点须为二维坐标");
            }
        }
        long[][] pts = new long[points.length][];
        for (int i = 0; i < points.length; i++) {
            pts[i] = points[i].clone();
        }
        Integer[] order = new Integer[pts.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        int[] axisAt = new int[pts.length];
        Arrays.sort(order, 0, order.length);
        buildRange(pts, order, 0, pts.length - 1, 0, axisAt);
        return new KdTree(pts, order, axisAt);
    }

    /** 最近邻查询（canonical 平局——同查询同结果）。 */
    public Nearest nearest(long x, long y) {
        long[] best = search(0, points.length - 1, x, y, -1, Long.MAX_VALUE);
        long[] p = points[(int) best[0]];
        return new Nearest(p[0], p[1], best[1]);
    }

    /** 点数读数。 */
    public int size() {
        return points.length;
    }

    private static int buildRange(long[][] pts, Integer[] order, int lo, int hi, int depth, int[] axisAt) {
        if (lo > hi) {
            return -1;
        }
        int axis = depth & 1;
        int mid = (lo + hi) >>> 1;
        final int a = axis;
        Arrays.sort(order, lo, hi + 1, (p, q) -> {
            if (pts[p][a] != pts[q][a]) {
                return Long.compare(pts[p][a], pts[q][a]);
            }
            int other = 1 - a;
            if (pts[p][other] != pts[q][other]) {
                return Long.compare(pts[p][other], pts[q][other]);
            }
            return Integer.compare(p, q);
        });
        axisAt[mid] = axis;
        buildRange(pts, order, lo, mid - 1, depth + 1, axisAt);
        buildRange(pts, order, mid + 1, hi, depth + 1, axisAt);
        return mid;
    }

    /** 返回 [bestIdx, bestDist]（深处改良向上传播——剪枝不可用陈旧下界）。 */
    private long[] search(int lo, int hi, long x, long y, int best, long bestDist) {
        if (lo > hi) {
            return new long[]{best, bestDist};
        }
        int mid = (lo + hi) >>> 1;
        int idx = order[mid];
        long[] p = points[idx];
        long dx = p[0] - x;
        long dy = p[1] - y;
        long dist = dx * dx + dy * dy;
        boolean improves = best == -1 || dist < bestDist
                || (dist == bestDist && better(p, points[best]));
        if (improves) {
            best = idx;
            bestDist = dist;
        }
        int axis = axisAt[mid];
        long target = axis == 0 ? x : y;
        long diff = target - p[axis];
        int nearLo = diff <= 0 ? lo : mid + 1;
        int nearHi = diff <= 0 ? mid - 1 : hi;
        long[] near = search(nearLo, nearHi, x, y, best, bestDist);
        best = (int) near[0];
        bestDist = near[1];
        long planeDist = diff * diff;
        if (best == -1 || planeDist <= bestDist) {
            long[] far = search(diff <= 0 ? mid + 1 : lo, diff <= 0 ? hi : mid - 1,
                    x, y, best, bestDist);
            best = (int) far[0];
            bestDist = far[1];
        }
        return new long[]{best, bestDist};
    }

    private boolean better(long[] candidate, long[] incumbent) {
        if (candidate[0] != incumbent[0]) {
            return candidate[0] < incumbent[0];
        }
        return candidate[1] < incumbent[1];
    }
}
