package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * 百分位排位（spec 1915 / T3031 / impl 1516）——统计学 percentile
 * rank：样本集中 ≤ value 的占比。「这次 12s 算慢吗」的答案是相对
 * 排位（超过历史多少）——绝对阈值跨场景失配，相对排位同一把尺。
 *
 * <p>纯函数零状态；越界钳 0.0/1.0（不捏造中间值）。
 */
public final class PercentileRank {

    private PercentileRank() {
    }

    /**
     * 排位：≤ value 的样本占比（0.0–1.0）。时延语义下排位越高越差。
     * 契约：samples 非空且逐值 ≥ 0（fail-fast）。
     */
    public static double rank(long[] samples, long value) {
        validate(samples);
        long covered = Arrays.stream(samples).filter(s -> s <= value).count();
        return (double) covered / samples.length;
    }

    /**
     * 百分位直读：rank × 100（0–100 整数口径）。
     */
    public static long percentileOf(long[] samples, long value) {
        return Math.round(rank(samples, value) * 100);
    }

    private static void validate(long[] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("样本表不能为空");
        }
        for (long s : samples) {
            if (s < 0) {
                throw new IllegalArgumentException("样本值不能为负：" + s);
            }
        }
    }
}
