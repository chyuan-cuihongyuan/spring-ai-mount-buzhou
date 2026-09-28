package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * Bresenham 直线（spec 7014 / U7229 / impl 2266）——
 * Bresenham 1965 整数光栅思想：**误差项逐格累积、只判
 * 符号无乘除**——像素格从起点到终点逐格推进（每轴步进
 * ≤1，格数 = max(|dx|,|dy|)+1）——浮点斜率逐步累加误差
 * （长线累积漂移）的病解。整数域全程无浮点——同参数同
 * 格序列完全确定（光栅可回放）。
 *
 * <p>与 ConvexHull（同包）同族不同面：两点间离散走格 vs
 * 点集外包络。
 */
public final class BresenhamLine {

    private BresenhamLine() {
    }

    /** 光栅格序列（含首尾端点；null 无——返回新列表）。 */
    public static List<long[]> line(int x0, int y0, int x1, int y1) {
        List<long[]> cells = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int stepX = x0 < x1 ? 1 : -1;
        int stepY = y0 < y1 ? 1 : -1;
        int error = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            cells.add(new long[]{x, y});
            if (x == x1 && y == y1) {
                break;
            }
            int doubled = 2 * error;
            if (doubled > -dy) {
                error -= dy;
                x += stepX;
            }
            if (doubled < dx) {
                error += dx;
                y += stepY;
            }
        }
        return cells;
    }
}
