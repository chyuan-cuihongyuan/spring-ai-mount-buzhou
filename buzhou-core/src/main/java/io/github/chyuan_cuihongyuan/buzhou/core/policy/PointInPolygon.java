package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * 点在多边形判定（spec 7013 / U7227 / impl 2265）——射线法
 * （crossing number；GIS/图形学同源思想）：**自查询点引
 * 水平射线，数与边界的穿越次数**，奇内偶外——逐边角度求和
 * O(n²) 或对每查询调 GPU（简单面过度设计）的病解。边界
 * 语义：点恰在边上=内（true——GIS「多边形含其边界」约定，
 * 显式可审计）；「半开穿越」规则处理顶点恰好对齐（边端点
 * 一上一下才计穿越——标准稳定性技巧）。简单多边形假设
 * （自相交多边形语义未定义——明示不做）。
 *
 * <p>与 KdTree（同包）同族不同面：点查询最近邻 vs 点查询
 * 多边形隶属。
 */
public final class PointInPolygon {

    private PointInPolygon() {
    }

    /**
     * 判定（边界=内；多边形须 ≥3 顶点闭链语义——首尾无需
     * 重复；null/退化 fail-fast）。
     */
    public static boolean contains(long[][] polygon, long x, long y) {
        if (polygon == null || polygon.length < 3) {
            throw new IllegalArgumentException("多边形至少三个顶点");
        }
        boolean inside = false;
        int n = polygon.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            long xi = polygon[i][0];
            long yi = polygon[i][1];
            long xj = polygon[j][0];
            long yj = polygon[j][1];
            if (onSegment(xi, yi, xj, yj, x, y)) {
                return true;
            }
            if ((yi > y) != (yj > y)) {
                long s = (x - xi) * (yj - yi);
                long t = (y - yi) * (xj - xi);
                if (yj > yi ? s < t : s > t) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    /** 点是否在闭线段上（共线 + 包围盒内）。 */
    private static boolean onSegment(long xi, long yi, long xj, long yj, long x, long y) {
        long cross = (xj - xi) * (y - yi) - (yj - yi) * (x - xi);
        if (cross != 0) {
            return false;
        }
        return x >= Math.min(xi, xj) && x <= Math.max(xi, xj)
                && y >= Math.min(yi, yj) && y <= Math.max(yi, yj);
    }
}
