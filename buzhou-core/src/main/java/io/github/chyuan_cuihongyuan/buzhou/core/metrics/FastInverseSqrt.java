package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 平方根倒数速算（spec 9032 / W9065 / impl 2385）——Fast
 * inverse sqrt 思想（Quake III Arena 1999 `Q_rsqrt` 同源——
 * 魔数 0x5f3759df 位级初值 + 一次牛顿迭代）：**IEEE 754 位型
 * 整数域减半指数近似 1/√x 初值，x·(1.5−½x·x²·x) 一次牛顿
 * 精化**——相对误差 <0.2% 的硬件级近似——Math.sqrt 求逆的
 * 精确路径（归一化/L2 预除等热路径上过度精确）的病解（历史
 * 面：现代 JVM/硬件已被超越——诚实边界明示，教学与低精度
 * 热路径价值）。float 位型域；x>0 契约（≤0/NaN fail-fast）；
 * 确定性纯函数。
 *
 * <p>与 KahanSummator（同包）同域不同面：数值误差补偿 vs
 * 有界近似直给；与 EwmaEstimator（同包）不同面：统计平滑
 * vs 代数速算。
 */
public final class FastInverseSqrt {

    private static final int MAGIC = 0x5f3759df;

    private FastInverseSqrt() {
    }

    /**
     * 1/√x 近似（一次牛顿迭代——相对误差 <0.2% 契约）。
     *
     * @throws IllegalArgumentException x ≤ 0、NaN 或 ∞（有限正数契约——位级初值对 ∞ 无意义）
     */
    public static float inverseSqrt(float x) {
        if (x <= 0 || Float.isNaN(x) || Float.isInfinite(x)) {
            throw new IllegalArgumentException("x 为有限正数（实际 " + x + "）");
        }
        float halfX = 0.5f * x;
        int bits = Float.floatToIntBits(x);
        bits = MAGIC - (bits >> 1);
        float estimate = Float.intBitsToFloat(bits);
        // 一次牛顿迭代：y_{n+1} = y_n·(1.5 − ½x·y_n²)
        return estimate * (1.5f - halfX * estimate * estimate);
    }
}
