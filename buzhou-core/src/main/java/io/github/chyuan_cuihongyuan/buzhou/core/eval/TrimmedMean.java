package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 截尾均值（spec 1852 / T2905 / impl 1453）——统计学 trimmed mean /
 * IPC/AToM 评审惯例：头尾各截 p% 再求均——长尾离群值（一次 30s 的卡顿、
 * 一个异常高分）不再污染中心趋向读数，且不似中位数完全丢序信息。评分
 * 清洗与延迟汇报共用：评委拉分（去最高最低再平均）与延迟中枢（去尾再
 * 平均）是同一形状。
 *
 * <p>纯函数零状态、确定性（排序后截尾）；截空 -1 哨兵。
 */
public final class TrimmedMean {

    /** 单侧截尾上限（截尾过半即无中心语义）。 */
    public static final double MAX_TRIM_FRACTION_PER_SIDE = 0.5d;

    private TrimmedMean() {
    }

    /**
     * 截尾均值入口。契约：trimFraction ∈ [0, 0.5) 双侧各截 ⌊n×f⌋ 个
     *（fail-fast：f ≥ 0.5 无中心语义）；样本非 null 非 NaN；null 列表按
     * 空表；截后为空（样本全被截）→ -1 哨兵。
     */
    public static double mean(List<Double> samples, double trimFraction) {
        if (Double.isNaN(trimFraction) || trimFraction < 0
                || trimFraction >= MAX_TRIM_FRACTION_PER_SIDE) {
            throw new IllegalArgumentException(
                    "trimFraction 须在 [0,0.5)：" + trimFraction);
        }
        List<Double> window = samples == null ? List.of() : samples;
        for (Double v : window) {
            if (v == null || v.isNaN()) {
                throw new IllegalArgumentException("样本不能为 null 或 NaN");
            }
        }
        if (window.isEmpty()) {
            return -1d;
        }
        List<Double> sorted = new ArrayList<>(window);
        sorted.sort(Comparator.naturalOrder());
        int trim = (int) Math.floor(sorted.size() * trimFraction);
        int from = trim;
        int to = sorted.size() - trim;
        if (from >= to) {
            return -1d;
        }
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += sorted.get(i);
        }
        return sum / (to - from);
    }
}
