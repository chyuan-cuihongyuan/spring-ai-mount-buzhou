package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * KS 两样本检验（spec 4004 / T6009 / impl 2105）——无参分布对比
 * 思想（Kolmogorov 1933 / Smirnov 1948；scipy.stats.ks_2samp /
 * Statsmodels 同款）：双样本经验分布函数（ECDF）最大竖直距离
 * D = sup|F₁(x) − F₂(x)|——不假设分布形状，只问「两组样本像不像
 * 出自同一个总体」；p 值走渐近 Kolmogorov 分布（Numerical
 * Recipes 小样本校正 en = √ne + 0.12 + 0.11/√ne，ne 为有效样本量）。
 *
 * <p>A/B 两版回答质量分布、两模型延迟分布、两池错误率分布的
 * 「整体错位」一尺量——t 检验只比均值（形状漂移盲区），卡方要
 * 分桶（桶宽主观），KS 直接对全分布形状。与 WilsonInterval
 * （单比例区间）/ChiSquareUniformity（均匀性）成统计三尺。
 */
public final class KsTwoSample {

    private KsTwoSample() {
    }

    /** 检验结果：D 统计量（0–1，越大越不像）与渐近 p 值（越小越拒）。 */
    public record Result(double statistic, double pValue) {
    }

    /** 两样本 KS 检验：D = sup|F₁−F₂| + 渐近 p（同分布 → D 小 p 大）。 */
    public static Result test(double[] xs, double[] ys) {
        if (xs == null || ys == null) {
            throw new IllegalArgumentException("样本非 null");
        }
        if (xs.length == 0 || ys.length == 0) {
            throw new IllegalArgumentException("样本非空");
        }
        double[] a = xs.clone();
        double[] b = ys.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        double d = 0;
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
            d = Math.max(d, Math.abs((double) i / a.length - (double) j / b.length));
        }
        return new Result(d, pValue(d, a.length, b.length));
    }

    /** 渐近 Kolmogorov p：Q(λ) = 2Σ(−1)^(k−1)·e^(−2k²λ²)（截断 1e-12）。 */
    static double pValue(double d, int n1, int n2) {
        if (d <= 0) {
            return 1.0;
        }
        double ne = (double) n1 * n2 / (n1 + n2);
        double en = Math.sqrt(ne) + 0.12 + 0.11 / Math.sqrt(ne);
        double lambda = en * d;
        if (lambda < 0.4) {
            return 1.0;   // 小 λ 区渐近式失效，Q(0.4)≈0.997 即「无法拒绝」
        }
        double p = 0;
        for (int k = 1; k <= 100; k++) {
            double term = Math.exp(-2.0 * k * k * lambda * lambda);
            p += (k % 2 == 1 ? 2 : -2) * term;
            if (term < 1e-12) {
                break;
            }
        }
        return Math.min(1.0, Math.max(0.0, p));
    }
}
