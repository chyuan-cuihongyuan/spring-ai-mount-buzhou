package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * Hilbert Curve 希尔伯特曲线（spec 6022 / T6237 续 / impl 2222）——
 * 空间填充曲线思想（PostGIS/地图瓦片同源）：**二维网格 ↔
 * 一维索引双射，保持局部性**——相邻格子索引差有界（Z 序
 * 曲线的对角跳变在此被规避），一维排序即空间聚类——二维
 * 邻域查询降为一维区间扫描的底座。标准 skew 递归式（无
 * 查表、无浮点），index/coordinate 互逆定构。
 *
 * <p>与 ZOrderCurve（recovery）同族不同面：希尔伯特连续
 * 走位（无大跳变）vs Morton 交织位（象限间跳变）。order
 * ≤31（索引 long 域），坐标域 [0, 2^order)。
 */
public final class HilbertCurve {

    private HilbertCurve() {
    }

    /** 二维坐标 → 一维索引（坐标越域/order 越域 fail-fast）。 */
    public static long index(long x, long y, int order) {
        requireOrder(order);
        long side = 1L << order;
        if (x < 0 || x >= side || y < 0 || y >= side) {
            throw new IllegalArgumentException("坐标越出 [0,2^" + order + "): ("
                    + x + ", " + y + ")");
        }
        long d = 0;
        for (long s = side / 2; s > 0; s /= 2) {
            long rx = (x & s) > 0 ? 1 : 0;
            long ry = (y & s) > 0 ? 1 : 0;
            d += s * s * ((3 * rx) ^ ry);
            if (ry == 0) {
                if (rx == 1) {
                    x = s - 1 - x;
                    y = s - 1 - y;
                }
                long t = x;
                x = y;
                y = t;
            }
        }
        return d;
    }

    /** 一维索引 → 二维坐标（index 越域 fail-fast）。 */
    public static long[] coordinate(long index, int order) {
        requireOrder(order);
        long side = 1L << order;
        if (index < 0 || index >= side * side) {
            throw new IllegalArgumentException("索引越出 [0,2^" + (2 * order) + "): " + index);
        }
        long x = 0;
        long y = 0;
        for (long s = 1; s < side; s *= 2) {
            long rx = 1 & (index / 2);
            long ry = 1 & (index ^ rx);
            if (ry == 0) {
                if (rx == 1) {
                    x = s - 1 - x;
                    y = s - 1 - y;
                }
                long t = x;
                x = y;
                y = t;
            }
            x += s * rx;
            y += s * ry;
            index /= 4;
        }
        return new long[]{x, y};
    }

    private static void requireOrder(int order) {
        if (order < 1 || order > 31) {
            throw new IllegalArgumentException("阶须在 [1,31]: " + order);
        }
    }
}
