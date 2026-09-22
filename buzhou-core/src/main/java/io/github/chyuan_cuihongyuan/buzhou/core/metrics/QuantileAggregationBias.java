package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 分位数聚合偏差审计（spec 1891 / T2983 / impl 1492）——Prometheus/M3
 * 惯例：分位数不可加。各分片 p99 的平均 ≠ 全体 p99——尾部长在哪个
 * 分片是概率问题不是平均问题。naive 聚合（分位平均）是常见错误
 * 口径，偏差量入账后 SLA 报告的违约判定才可信。
 *
 * <p>纯函数零状态；低报（负偏差）最危险——判定量用绝对值。
 */
public final class QuantileAggregationBias {

    private QuantileAggregationBias() {
    }

    /**
     * naive 聚合读数：分片分位的算术平均——「错误答案长什么样」
     * 本身可示众。契约：数组非空且逐值 ≥ 0（时延量纲，fail-fast）。
     */
    public static double naiveAverage(double[] shardQuantiles) {
        if (shardQuantiles == null || shardQuantiles.length == 0) {
            throw new IllegalArgumentException("分片分位表不能为空");
        }
        double sum = 0;
        for (double q : shardQuantiles) {
            if (q < 0 || Double.isNaN(q)) {
                throw new IllegalArgumentException("分位值不能为负或 NaN：" + q);
            }
            sum += q;
        }
        return sum / shardQuantiles.length;
    }

    /**
     * 偏差比：(naive − actual)/actual。负 = naive 低报（最危险——
     * SLA 实际已违约而报告未达）；正 = 高报。契约：actual > 0
     * （fail-fast）。
     */
    public static double biasRatio(double naive, double actual) {
        if (actual <= 0) {
            throw new IllegalArgumentException("actual 须 > 0：" + actual);
        }
        return (naive - actual) / actual;
    }

    /**
     * 实质性偏差判定：|偏差比| ≥ tolerance 即 naive 口径不可用。
     * 契约：tolerance ∈ [0,1]（fail-fast）。
     */
    public static boolean isMateriallyBiased(double naive, double actual,
                                             double tolerance) {
        if (tolerance < 0.0 || tolerance > 1.0) {
            throw new IllegalArgumentException(
                    "tolerance 须在 [0,1]：" + tolerance);
        }
        return Math.abs(biasRatio(naive, actual)) >= tolerance;
    }
}
