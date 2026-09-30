package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/**
 * 切片采样（spec 10028 / X10057 / impl 2431）——Neal 2003 思想
 * （「辅助竖切+步出收缩」——PyMC/NumPyro 同源）：**引入切片高度
 * y=ln f(x)−Exp(1)，步出扩区（定宽倍增边界）+收缩拒绝（超界缩
 * 区）逼近条件分布**——免调提议分布的 MCMC（Metropolis–Hastings
 * 已占异面：接受率调参 vs 无提议面）。对数密度接口（防下溢）；
 * null 密度/非正步宽/计数越域 fail-fast；同种子同链确定。
 */
public final class SliceSampler {

    /** 步出扩区步数上限（重尾护栏）。 */
    private static final int MAX_STEPS_OUT = 32;

    private SliceSampler() {
    }

    /**
     * 采样链（count 个样本，首样本自 initial 起步）。
     *
     * @param logDensity 对数密度（可差常数；确定性纯函数）
     * @param initial 初始点（密度须为正）
     * @param count 采样数
     * @param stepWidth 步出初始宽度
     * @param seed 随机种子
     * @throws IllegalArgumentException null 密度/非正步宽/计数越域/初始密度非正
     */
    public static double[] sample(DoubleUnaryOperator logDensity, double initial,
                                  int count, double stepWidth, long seed) {
        if (logDensity == null) {
            throw new IllegalArgumentException("对数密度非 null");
        }
        if (count < 1) {
            throw new IllegalArgumentException("采样数为正（实际 " + count + "）");
        }
        if (!(stepWidth > 0.0) || !Double.isFinite(stepWidth)) {
            throw new IllegalArgumentException("步宽为正有限（实际 " + stepWidth + "）");
        }
        double current = initial;
        double currentLog = logDensity.applyAsDouble(current);
        if (!Double.isFinite(currentLog)) {
            throw new IllegalArgumentException("初始点密度为正有限（实际 " + currentLog + "）");
        }
        Random random = new Random(seed);
        double[] samples = new double[count];
        for (int i = 0; i < count; i++) {
            double sliceLevel = currentLog + Math.log(1.0 - random.nextDouble());
            double left = current - stepWidth * random.nextDouble();
            double right = left + stepWidth;
            int stepsOut = 0;
            while (logDensity.applyAsDouble(left) > sliceLevel && stepsOut++ < MAX_STEPS_OUT) {
                left -= stepWidth;
            }
            stepsOut = 0;
            while (logDensity.applyAsDouble(right) > sliceLevel && stepsOut++ < MAX_STEPS_OUT) {
                right += stepWidth;
            }
            double candidate;
            do {
                candidate = left + (right - left) * random.nextDouble();
                if (logDensity.applyAsDouble(candidate) > sliceLevel) {
                    break;
                }
                if (candidate < current) {
                    left = candidate;
                } else {
                    right = candidate;
                }
            } while (true);
            current = candidate;
            currentLog = logDensity.applyAsDouble(current);
            samples[i] = current;
        }
        return samples;
    }
}
