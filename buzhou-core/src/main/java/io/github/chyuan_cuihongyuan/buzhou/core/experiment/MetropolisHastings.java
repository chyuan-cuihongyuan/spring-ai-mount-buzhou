package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/**
 * Metropolis–Hastings MCMC 采样（spec 9026 / W9053 / impl 2379）——
 * Metropolis 1953 思想（Hastings 1970 一般化——物理系综/贝叶斯后验
 * 采样同源）：**对称随机游走提议 + 对数密度比的接受检验
 * log(u) < logP(x')−logP(x)——拒绝即留原点**（细致平衡保平稳
 * 分布）——直接逆 CDF（需解析形式）与拒绝采样（需包络）之外的
 * 只需「未归一化密度可算」的通用形态。单变量对称游走变体；
 * log 域防下溢；burn-in 弃前置；种子化确定可回放。
 *
 * <p>与 RejectionSampler（同包）同域不同面：需包络常数 vs
 * 只需密度比；与 GaussianSampler（policy 域）不同面：已知分布
 * 直采 vs 任意密度构造采样。
 */
public final class MetropolisHastings {

    private MetropolisHastings() {
    }

    /**
     * 抽样（返回 draws−burnIn 个样本；对称游走步长入参）。
     *
     * @throwsIllegalArgumentException logDensity null、步长/数量/烧入越域
     */
    public static double[] sample(DoubleUnaryOperator logDensity, double start, double stepSize,
                                  int draws, int burnIn, Random random) {
        if (logDensity == null) {
            throw new IllegalArgumentException("对数密度非空引用");
        }
        if (stepSize <= 0 || Double.isNaN(stepSize)) {
            throw new IllegalArgumentException("步长为正（实际 " + stepSize + "）");
        }
        if (draws < 1) {
            throw new IllegalArgumentException("抽样数为正（实际 " + draws + "）");
        }
        if (burnIn < 0 || burnIn >= draws) {
            throw new IllegalArgumentException("烧入 ∈[0,draws)（实际 " + burnIn + "/" + draws + "）");
        }
        double x = start;
        double logPx = logDensity.applyAsDouble(x);
        if (Double.isNaN(logPx)) {
            throw new IllegalArgumentException("起点密度 NaN（" + start + "）");
        }
        double[] samples = new double[draws - burnIn];
        int kept = 0;
        for (int i = 0; i < draws; i++) {
            double candidate = x + stepSize * random.nextGaussian();
            double logPc = logDensity.applyAsDouble(candidate);
            double logAlpha = logPc - logPx;
            boolean accept = !Double.isNaN(logPc) && !Double.isInfinite(logPc)
                    && (logAlpha >= 0 || Math.log(random.nextDouble()) < logAlpha);
            if (accept) {
                x = candidate;
                logPx = logPc;
            }
            if (i >= burnIn) {
                samples[kept++] = x;
            }
        }
        return samples;
    }
}
