package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.function.ToDoubleFunction;

/**
 * 刀切法重采样估计（spec 9028 / W9057 / impl 2381）——Jackknife
 * 思想（Quenouille 1949/Tukey 1958 命名——偏差消减与标准误同源）：
 * **逐个剔除观测、对剩余 n−1 重算统计量（leave-one-out 子样本），
 * 偏差 = (n−1)(θ̄_loo − θ̂)、标准误 = √((n−1)/n·Σ(θ_i−θ̄)²)**
 * ——单次全样本估计（无不确定性读数）与 bootstrap（有放回、
 * 计算型）之间的确定性重采样：n 个子样本恰好一次。统计量
 * 入参（ToDoubleFunction<double[]>——任意统计量可刀切）；
 * 确定性纯函数；null/样本 <2/统计量 null fail-fast。
 *
 * <p>与 TrimmedMean/GrubbsOutlier（同包）同域不同面：稳健点
 * 估计 vs 不确定性量化；与 PermutationTest（spec 9027）互补：
 * 显著性 vs 偏差/方差。
 */
public final class JackknifeEstimator {

    /** 刀切结果：偏差消减估计 + 偏差 + 标准误。 */
    public record Result(double estimate, double bias, double standardError) {
    }

    private JackknifeEstimator() {
    }

    /**
     * 任意统计量的刀切估计（leave-one-out 子样本恰好 n 个）。
     *
     * @throws IllegalArgumentException null 样本/统计量、样本 < 2
     */
    public static Result estimate(double[] sample, ToDoubleFunction<double[]> statistic) {
        if (sample == null || statistic == null) {
            throw new IllegalArgumentException("样本与统计量非空引用");
        }
        int n = sample.length;
        if (n < 2) {
            throw new IllegalArgumentException("样本 ≥2（刀切子样本语义，实际 " + n + "）");
        }
        double full = statistic.applyAsDouble(sample);
        double[] loo = new double[n];
        double[] reduced = new double[n - 1];
        for (int omit = 0; omit < n; omit++) {
            int idx = 0;
            for (int i = 0; i < n; i++) {
                if (i != omit) {
                    reduced[idx++] = sample[i];
                }
            }
            loo[omit] = statistic.applyAsDouble(reduced);
        }
        double looMean = 0;
        for (double v : loo) {
            looMean += v;
        }
        looMean /= n;
        double bias = (n - 1) * (looMean - full);
        double squares = 0;
        for (double v : loo) {
            squares += (v - looMean) * (v - looMean);
        }
        double standardError = Math.sqrt((n - 1) / (double) n * squares);
        return new Result(full - bias, bias, standardError);
    }
}
