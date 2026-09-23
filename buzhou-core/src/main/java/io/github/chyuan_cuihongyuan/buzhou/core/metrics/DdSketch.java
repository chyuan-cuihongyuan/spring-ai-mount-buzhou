package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Map;
import java.util.TreeMap;

/**
 * DDSketch 相对误差分位草图（spec 4039 / T6079 / impl 2140）——
 * 对数桶分位思想（Datadog DDSketch）：γ = (1+α)/(1−α)，桶序
 * idx(v) = ⌈ln v / ln γ⌉——桶宽**相对恒定**（任意量级同精度）；
 * 分位估计 = γ^idx（相对高估 &lt; γ，论文诚实口径）；min/max
 * **精确**旁路记录；{@code merge} 同 γ 草图桶计数相加（分位
 * 误差保证不变）。全量存储内存爆炸与固定绝对精度直方图
 * （低延迟端糊成一桶）的病解。
 *
 * <p>正值域（论文口径）；确定性无随机。与
 * CountMinSketch/FrequencySketch（频次族）、
 * HllCardinalitySketch（基数族）同族不同面：分位数族。
 */
public final class DdSketch {

    private final double relativeAccuracy;
    private final double gamma;
    private final double logGamma;
    private final TreeMap<Long, Long> buckets = new TreeMap<>();
    private long count;
    private double min = Double.NaN;
    private double max = Double.NaN;

    /** 定构（α∈(0,1) 否则 fail-fast）。 */
    public DdSketch(double relativeAccuracy) {
        if (relativeAccuracy <= 0 || relativeAccuracy >= 1) {
            throw new IllegalArgumentException("相对精度 α∈(0,1)：" + relativeAccuracy);
        }
        this.relativeAccuracy = relativeAccuracy;
        this.gamma = (1 + relativeAccuracy) / (1 - relativeAccuracy);
        this.logGamma = Math.log(gamma);
    }

    /** 记入正值观测（v≤0/非有限 fail-fast）。 */
    public void accept(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("观测需正有限值：" + value);
        }
        long index = bucketIndex(value);
        buckets.merge(index, 1L, Long::sum);
        count++;
        min = Double.isNaN(min) ? value : Math.min(min, value);
        max = Double.isNaN(max) ? value : Math.max(max, value);
    }

    /** 合并同 γ 草图（γ 不一致 fail-fast）。 */
    public void merge(DdSketch other) {
        if (other == null || other.gamma != gamma) {
            throw new IllegalArgumentException("merge 需同 γ 草图："
                    + (other == null ? "null" : other.gamma) + " vs " + gamma);
        }
        for (Map.Entry<Long, Long> entry : other.buckets.entrySet()) {
            buckets.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
        count += other.count;
        if (!Double.isNaN(other.min)) {
            min = Double.isNaN(min) ? other.min : Math.min(min, other.min);
        }
        if (!Double.isNaN(other.max)) {
            max = Double.isNaN(max) ? other.max : Math.max(max, other.max);
        }
    }

    /**
     * 分位估计（γ^idx，相对高估 &lt; γ；空草图 ISE 诚实）。
     *
     * @param quantile ∈(0,1]
     */
    public double quantile(double quantile) {
        if (quantile <= 0 || quantile > 1) {
            throw new IllegalArgumentException("q∈(0,1]：" + quantile);
        }
        if (count == 0) {
            throw new IllegalStateException("空草图（零观测）诚实无分位");
        }
        long rank = (long) Math.ceil(quantile * count);
        long accumulated = 0;
        for (Map.Entry<Long, Long> entry : buckets.entrySet()) {
            accumulated += entry.getValue();
            if (accumulated >= rank) {
                return Math.pow(gamma, entry.getKey());
            }
        }
        return max;   // 不可达（rank ≤ count 守恒）
    }

    /** 观测总数读数。 */
    public long count() {
        return count;
    }

    /** 精确最小值读数（零观测 NaN 诚实）。 */
    public double min() {
        return min;
    }

    /** 精确最大值读数（零观测 NaN 诚实）。 */
    public double max() {
        return max;
    }

    /** 相对精度读数。 */
    public double relativeAccuracy() {
        return relativeAccuracy;
    }

    /** 桶序（⌈ln v / ln γ⌉）。 */
    private long bucketIndex(double value) {
        return (long) Math.ceil(Math.log(value) / logGamma);
    }
}
