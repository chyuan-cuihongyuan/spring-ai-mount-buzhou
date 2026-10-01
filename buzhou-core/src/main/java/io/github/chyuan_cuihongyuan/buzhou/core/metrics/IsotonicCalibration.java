package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * 保序回归（spec 11015 / Y11031 / impl 2468）——PAVA（Pool Adjacent
 * Violators）思想（scikit-learn IsotonicRegression 同源）：**栈式压块
 * （sum,count）+相邻块均值违序即合并直至无违序**的非降最小二乘投影——
 * 校准概率序的基座原语（总和守恒：块均值展开的 Σ=Σx）。
 *
 * <p>null/空/非有限值 fail-fast；复算确定。
 */
public final class IsotonicCalibration {

    private IsotonicCalibration() {
    }

    /**
     * 非降保序拟合。
     *
     * @param values 观测值（长度 ≥1）
     * @return 拟合值（非降，与输入等长）
     * @throws IllegalArgumentException null/空/非有限值
     */
    public static double[] fit(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("值序列非空");
        }
        for (int i = 0; i < values.length; i++) {
            if (!Double.isFinite(values[i])) {
                throw new IllegalArgumentException("值非有限（第 " + i + " 位 " + values[i] + "）");
            }
        }
        double[] blockSums = new double[values.length];
        int[] blockCounts = new int[values.length];
        int top = -1;
        for (double value : values) {
            top++;
            blockSums[top] = value;
            blockCounts[top] = 1;
            while (top > 0 && blockSums[top - 1] / blockCounts[top - 1]
                    > blockSums[top] / blockCounts[top]) {
                blockSums[top - 1] += blockSums[top];
                blockCounts[top - 1] += blockCounts[top];
                top--;
            }
        }
        double[] fitted = new double[values.length];
        int position = 0;
        for (int block = 0; block <= top; block++) {
            double mean = blockSums[block] / blockCounts[block];
            Arrays.fill(fitted, position, position + blockCounts[block], mean);
            position += blockCounts[block];
        }
        return fitted;
    }
}
