package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;

/**
 * 最近点对（spec 7012 / U7225 / impl 2264）——分治剪枝
 * 思想（Bentley-Shamos）：**x 排序对半分治 + 中带 y 序
 * 有界比较**（带内每点只比窗口内证据点）——O(n log²n)
 * （strip 逐层重排的诚实变体，非镜像归并 O(n log n) 面）
 * ——全对暴力 O(n²)（万级点不可承受）的病解。返回距离
 * 平方（long 域无浮点误差）；重合点距离 0（诚实）；坐标
 * ±10^9 安全域 fail-fast（乘积溢出诚实拒绝）。
 *
 * <p>与 KdTree（同包）同族不同面：全点集最近「对」vs
 * 查询点最近「邻」。
 */
public final class ClosestPair {

    private static final long COORD_LIMIT = 1_000_000_000L;

    private ClosestPair() {
    }

    /** 最近点对距离平方（分治；<2 点/null/越域 fail-fast）。 */
    public static long closestPairSquared(long[][] points) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("至少两个点");
        }
        long[][] sorted = new long[points.length][];
        for (int i = 0; i < points.length; i++) {
            long[] p = points[i];
            if (p == null || p.length != 2) {
                throw new IllegalArgumentException("点须为 [x,y] 二元组");
            }
            if (Math.abs(p[0]) > COORD_LIMIT || Math.abs(p[1]) > COORD_LIMIT) {
                throw new IllegalArgumentException("坐标越 ±10^9 安全域: " + Arrays.toString(p));
            }
            sorted[i] = p.clone();
        }
        Arrays.sort(sorted, (a, b) -> a[0] != b[0]
                ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        return divide(sorted, 0, sorted.length);
    }

    /** [from,to) 分治（x 升序区间上）。 */
    private static long divide(long[][] byX, int from, int to) {
        int count = to - from;
        if (count <= 3) {
            long best = Long.MAX_VALUE;
            for (int i = from; i < to; i++) {
                for (int j = i + 1; j < to; j++) {
                    best = Math.min(best, distSq(byX[i], byX[j]));
                }
            }
            return best;
        }
        int mid = from + count / 2;
        long midX = byX[mid][0];
        long best = Math.min(divide(byX, from, mid), divide(byX, mid, to));
        long[][] strip = new long[count][];
        int stripCount = 0;
        for (int i = from; i < to; i++) {
            long dx = byX[i][0] - midX;
            if (dx * dx < best) {
                strip[stripCount++] = byX[i];
            }
        }
        Arrays.sort(strip, 0, stripCount, (a, b) -> a[1] != b[1]
                ? Long.compare(a[1], b[1]) : Long.compare(a[0], b[0]));
        for (int i = 0; i < stripCount; i++) {
            for (int j = i + 1; j < stripCount
                    && dySq(strip[i], strip[j]) < best; j++) {
                best = Math.min(best, distSq(strip[i], strip[j]));
            }
        }
        return best;
    }

    private static long dySq(long[] a, long[] b) {
        long dy = a[1] - b[1];
        return dy * dy;
    }

    private static long distSq(long[] a, long[] b) {
        long dx = a[0] - b[0];
        long dy = a[1] - b[1];
        return dx * dx + dy * dy;
    }
}
