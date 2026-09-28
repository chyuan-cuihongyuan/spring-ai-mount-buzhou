package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 柱状图最大矩形（spec 7043 / U7287 / impl 2295）——单调栈
 * 思想（Largest Rectangle in Histogram 经典，编译器/数据
 * 可视化同源）：**每根柱作为「高度」时其左右第一个更矮柱
 * 边界由单调递增栈一次扫描确定** O(n)——全对边界暴力
 * O(n²)（柱数放大）的病解。哨兵高度 0 收尾清栈（尾段
 * 矩形不漏）；long 高度域；确定性纯函数；null/空 fail-fast。
 *
 * <p>与 MonotonicDeque（同包）同族不同面：滑窗最值维护 vs
 * 边界确定栈。
 */
public final class LargestRectangleHistogram {

    private LargestRectangleHistogram() {
    }

    /** 最大矩形面积（long 域；null/空 fail-fast）。 */
    public static long maxRectangle(long[] heights) {
        if (heights == null || heights.length == 0) {
            throw new IllegalArgumentException("高度列非空");
        }
        Deque<Integer> stack = new ArrayDeque<>();
        long best = 0;
        for (int i = 0; i <= heights.length; i++) {
            long current = i == heights.length ? 0L : heights[i];
            while (!stack.isEmpty() && heights[stack.peek()] >= current) {
                long height = heights[stack.pop()];
                int left = stack.isEmpty() ? -1 : stack.peek();
                long width = i - left - 1;
                best = Math.max(best, height * width);
            }
            stack.push(i);
        }
        return best;
    }
}
