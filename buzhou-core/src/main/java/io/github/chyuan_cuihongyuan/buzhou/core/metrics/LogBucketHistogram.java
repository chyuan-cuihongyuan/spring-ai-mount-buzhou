package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 对数分桶直方图（spec 1860 / T2921 / impl 1461）——HdrHistogram /
 * DDSketch 思想：值按对数指数分桶（桶 k 覆盖 [γ^k, γ^(k+1))）——桶内
 * 任意两点相对差 ≤ γ−1，因此**桶中点做分位数估计的相对误差有界**（γ=1.25
 * 即 ±12.5% 量级）——延迟/成本分位数不用全排序：一遍 O(n) 分桶 + 桶序
 * 遍历。正值域（对数域契约）。
 *
 * <p>纯函数零状态、确定性；只估计不绘制。
 */
public final class LogBucketHistogram {

    /** 默认桶底（相对误差界 ≈ 12.5%）。 */
    public static final double DEFAULT_GAMMA = 1.25d;

    private LogBucketHistogram() {
    }

    /**
     * 桶号：floor(ln(value)/ln(gamma))。契约：value &gt; 0、gamma &gt; 1
     *（fail-fast——γ=1 无桶宽）。
     */
    public static int bucketIndex(double value, double gamma) {
        if (!(value > 0) || Double.isNaN(value)) {
            throw new IllegalArgumentException("value 须为正数：" + value);
        }
        if (!(gamma > 1) || Double.isNaN(gamma)) {
            throw new IllegalArgumentException("gamma 须 > 1：" + gamma);
        }
        return (int) Math.floor(Math.log(value) / Math.log(gamma));
    }

    /**
     * 分桶账：桶号 → 计数（TreeMap 升序——分位数遍历用）。
     */
    public static TreeMap<Integer, Long> buckets(List<Double> samples, double gamma) {
        List<Double> window = samples == null ? List.of() : samples;
        Map<Integer, Long> counts = new HashMap<>();
        for (Double v : window) {
            counts.merge(bucketIndex(v, gamma), 1L, Long::sum);
        }
        return new TreeMap<>(counts);
    }

    /**
     * 近似分位数：按桶序累计到 q×n 处，取该桶几何中点 sqrt(γ^k × γ^(k+1))
     * 为估计。相对误差有界（γ−1）。契约：q ∈ (0,1]；样本非空正值。
     *
     * @return 估计值与相对误差界（γ−1）
     */
    public static ApproxQuantile quantile(List<Double> samples, double q,
                                          double gamma) {
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("样本不能为空");
        }
        if (!(q > 0) || q > 1 || Double.isNaN(q)) {
            throw new IllegalArgumentException("q 须在 (0,1]：" + q);
        }
        TreeMap<Integer, Long> buckets = buckets(samples, gamma);
        long target = (long) Math.ceil(q * samples.size());
        long cumulative = 0;
        for (Map.Entry<Integer, Long> e : buckets.entrySet()) {
            cumulative += e.getValue();
            if (cumulative >= target) {
                int k = e.getKey();
                double lower = Math.pow(gamma, k);
                double upper = Math.pow(gamma, k + 1);
                double midpoint = Math.sqrt(lower * upper);
                return new ApproxQuantile(midpoint, gamma - 1.0d);
            }
        }
        // 不可达（累计终将覆盖 target）——防御
        throw new IllegalStateException("分桶遍历未覆盖目标位");
    }

    /** 近似值 + 相对误差界。 */
    public record ApproxQuantile(double estimate, double relativeErrorBound) {
    }
}
