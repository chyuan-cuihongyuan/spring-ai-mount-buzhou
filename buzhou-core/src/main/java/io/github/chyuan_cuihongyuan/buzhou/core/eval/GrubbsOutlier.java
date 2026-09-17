package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.WelfordAccumulator;

import java.util.Arrays;

/**
 * Grubbs 离群检验（spec 3024 / T5049 / impl 2025）——Grubbs 1950
 * 思想（极端学生化偏差）：G = max|xᵢ−mean| / s 对内置临界值表
 * （双侧 α=0.05，n=3..30）——单点离群（毛刺延迟/异常成本/跑分
 * 野值）的判定件；与 IQR 围栏（五数概括，抗多离群掩蔽）互补：
 * Grubbs 对**单**离群最优功效（正态假设下），多离群相互拉宽 s
 * 会自掩蔽——一离群一档，两离群先 IQR。
 *
 * <p>矩计算复用 {@link WelfordAccumulator}；零方差诚实拒绝（全
 * 等样本无离群可言）；表界外（n&lt;3 或 &gt;30）fail-fast。
 */
public final class GrubbsOutlier {

    /** 双侧 α=0.05 临界值表（n=3..32——标准 Grubbs 表）。 */
    private static final double[] CRITICAL_ALPHA_05 = {
            1.153, 1.463, 1.672, 1.822, 1.938, 2.032, 2.110, 2.176, 2.234, 2.285,
            2.331, 2.371, 2.409, 2.443, 2.475, 2.504, 2.532, 2.557, 2.580, 2.603,
            2.624, 2.644, 2.663, 2.681, 2.698, 2.714, 2.730, 2.745, 2.760, 2.774};

    /** 表最小样本数。 */
    private static final int MIN_SAMPLES = 3;

    /** 表最大样本数（表长自洽导出）。 */
    private static final int MAX_SAMPLES = MIN_SAMPLES + CRITICAL_ALPHA_05.length - 1;

    /** 检验结果：矩读数 + G 统计量 + 临界值 + 离群判定与离群值。 */
    public record Result(double mean, double stdDev, double statistic, double criticalValue,
            Double outlier, boolean outlierDetected) {
    }

    private GrubbsOutlier() {
    }

    /** 单遍检验（n∈[3,32]；零方差拒绝）。 */
    public static Result test(double... samples) {
        if (samples == null || samples.length < MIN_SAMPLES || samples.length > MAX_SAMPLES) {
            throw new IllegalArgumentException("样本数 ∈[3," + MAX_SAMPLES + "]："
                    + (samples == null ? "null" : samples.length));
        }
        WelfordAccumulator acc = new WelfordAccumulator();
        for (double x : samples) {
            acc.add(x);
        }
        double mean = acc.mean();
        double stdDev = acc.sampleVariance();
        stdDev = Math.sqrt(stdDev);
        if (stdDev == 0) {
            throw new IllegalArgumentException("零方差（全等样本）——无离群可言");
        }
        double maxDeviation = 0;
        double outlier = mean;
        for (double x : samples) {
            double deviation = Math.abs(x - mean);
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
                outlier = x;
            }
        }
        double statistic = maxDeviation / stdDev;
        double critical = CRITICAL_ALPHA_05[samples.length - MIN_SAMPLES];
        boolean detected = statistic > critical;
        return new Result(mean, stdDev, statistic, critical, detected ? outlier : null, detected);
    }

    /** 表界自证（防手抄错位——表长与界常量自洽）。 */
    static {
        assert CRITICAL_ALPHA_05.length == 30 : "临界表覆盖 n=3..32";
        assert Arrays.stream(CRITICAL_ALPHA_05).allMatch(v -> v > 1 && v < 3) : "临界值量纲 sanity";
    }
}
