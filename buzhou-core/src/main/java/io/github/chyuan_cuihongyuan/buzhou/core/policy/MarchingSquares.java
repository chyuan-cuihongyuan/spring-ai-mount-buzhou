package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * 等值线提取（spec 10016 / X10033 / impl 2419）——Marching Squares
 * 思想（「16 位型逐格连线」——d3-contour/图像描边同源）：**逐
 * 2×2 格四角阈值分类 16 位型、跨阈边线性插值取等值点连线**——
 * 标量场等值线的栅格离散直接法（解析等值线求解的病解互补）。
 * 模糊位型（5/10）默认双段解（对角配对契约入档）；网格须 ≥2×2
 * 且等宽；null/尺寸/参差 fail-fast；同输入同线确定。
 */
public final class MarchingSquares {

    /** 位型角权重（TL=8/TR=4/BR=2/BL=1——标准编码）。 */
    private static final int BIT_TL = 8;
    private static final int BIT_TR = 4;
    private static final int BIT_BR = 2;
    private static final int BIT_BL = 1;

    private MarchingSquares() {
    }

    /**
     * 等值线段集（每段 {x1,y1,x2,y2}；格值 (row,col) 映射点 (col,row)）。
     *
     * @throws IllegalArgumentException null/网格 <2×2/参差
     */
    public static List<double[]> contour(double[][] grid, double threshold) {
        if (grid == null || grid.length < 2 || grid[0] == null || grid[0].length < 2) {
            throw new IllegalArgumentException("网格 ≥2×2 且非 null");
        }
        int rows = grid.length;
        int cols = grid[0].length;
        for (double[] row : grid) {
            if (row == null || row.length != cols) {
                throw new IllegalArgumentException("网格行等宽（实际 "
                        + (row == null ? "null" : row.length) + " vs " + cols + "）");
            }
        }
        List<double[]> segments = new ArrayList<>();
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                int caseBits = 0;
                if (grid[r][c] >= threshold) {
                    caseBits |= BIT_TL;
                }
                if (grid[r][c + 1] >= threshold) {
                    caseBits |= BIT_TR;
                }
                if (grid[r + 1][c + 1] >= threshold) {
                    caseBits |= BIT_BR;
                }
                if (grid[r + 1][c] >= threshold) {
                    caseBits |= BIT_BL;
                }
                if (caseBits == 0 || caseBits == 15) {
                    continue;
                }
                double top = interpolate(threshold, grid[r][c], grid[r][c + 1]);
                double bottom = interpolate(threshold, grid[r + 1][c], grid[r + 1][c + 1]);
                double left = interpolate(threshold, grid[r][c], grid[r + 1][c]);
                double right = interpolate(threshold, grid[r][c + 1], grid[r + 1][c + 1]);
                for (int[] pair : caseSegments(caseBits)) {
                    segments.add(new double[]{edgePoint(pair[0], r, c, top, bottom, left, right),
                            edgePointY(pair[0], r, c, top, bottom, left, right),
                            edgePoint(pair[1], r, c, top, bottom, left, right),
                            edgePointY(pair[1], r, c, top, bottom, left, right)});
                }
            }
        }
        return segments;
    }

    /** 跨阈边线性插值（0..1 段内位置——分母为零退 0.5）。 */
    private static double interpolate(double threshold, double v0, double v1) {
        double span = v1 - v0;
        if (Math.abs(span) < 1e-15) {
            return 0.5;
        }
        return (threshold - v0) / span;
    }

    /** 位型→跨阈边对（边编码 0=上 1=右 2=下 3=左；5/10 双段对角解）。 */
    private static int[][] caseSegments(int caseBits) {
        return switch (caseBits) {
            case 1, 14 -> new int[][]{{3, 2}};
            case 2, 13 -> new int[][]{{2, 1}};
            case 3, 12 -> new int[][]{{3, 1}};
            case 4, 11 -> new int[][]{{0, 1}};
            case 6, 9 -> new int[][]{{0, 2}};
            case 7, 8 -> new int[][]{{3, 0}};
            case 5 -> new int[][]{{3, 0}, {2, 1}};
            case 10 -> new int[][]{{0, 1}, {3, 2}};
            default -> new int[0][];
        };
    }

    /** 边编码→x 坐标（格原点 (c,r)）。 */
    private static double edgePoint(int edge, int r, int c,
                                    double top, double bottom, double left, double right) {
        return switch (edge) {
            case 0 -> c + top;
            case 1 -> c + 1;
            case 2 -> c + bottom;
            default -> c;
        };
    }

    /** 边编码→y 坐标。 */
    private static double edgePointY(int edge, int r, int c,
                                     double top, double bottom, double left, double right) {
        return switch (edge) {
            case 0 -> r;
            case 1 -> r + right;
            case 2 -> r + 1;
            default -> r + left;
        };
    }
}
