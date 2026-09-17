package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Welford 在线方差（spec 3001 / T5003 / impl 2002）——Welford
 * 增量算法思想：单遍流式递推均值与二阶矩 M2（方差 = M2/(n−1) 或
 * M2/n），免定义式「Σx² − n·mean²」的两个大数相减——大偏移 + 小
 * 波动场景（1e9 量级基线上的毫秒抖动）朴素公式灾难性精度抵消的
 * 根治；Chan et al. 并合并——分片各自累计后 pairwise 合并，与
 * 单遍全量数学等价。
 *
 * <p>可变累积器、单流口径（非线程安全）；跨线程/分片先各自累计
 * 再 {@link #merge}。诚实边界：空累积器 mean/方差 NaN；单点
 * sampleVariance NaN（无离散可言）。
 */
public final class WelfordAccumulator {

    private long count;
    private double mean;
    private double m2;

    /** 增量吸收一个样本（Welford 递推：delta 双用——先修均值再修矩）。 */
    public void add(double x) {
        count++;
        double delta = x - mean;
        mean += delta / count;
        m2 += delta * (x - mean);
    }

    /**
     * Chan 合并：this ← this + other（pairwise 公式；与把 other
     * 样本逐个 add 进 this 数学等价）。空侧恒等（双向）；不可传 this。
     */
    public void merge(WelfordAccumulator other) {
        if (other.count == 0) {
            return;
        }
        if (count == 0) {
            count = other.count;
            mean = other.mean;
            m2 = other.m2;
            return;
        }
        long total = count + other.count;
        double delta = other.mean - mean;
        mean += delta * other.count / total;
        m2 += other.m2 + delta * delta * count * other.count / total;
        count = total;
    }

    /** 样本数。 */
    public long count() {
        return count;
    }

    /** 均值（空累积器 NaN——诚实边界，不臆答 0）。 */
    public double mean() {
        return count == 0 ? Double.NaN : mean;
    }

    /** 样本方差（n−1 分母；n&lt;2 NaN——单点无离散）。 */
    public double sampleVariance() {
        return count < 2 ? Double.NaN : m2 / (count - 1);
    }

    /** 总体方差（n 分母；空累积器 NaN）。 */
    public double populationVariance() {
        return count < 1 ? Double.NaN : m2 / count;
    }
}
