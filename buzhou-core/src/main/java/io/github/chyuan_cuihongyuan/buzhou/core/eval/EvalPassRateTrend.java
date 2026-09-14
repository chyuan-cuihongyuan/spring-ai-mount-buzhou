package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估通过率趋势审计（spec 1445 之外的相邻轴——R44 = effort #1444 / spec 1444 /
 * 票 T2189 + T2190 / impl 1096）——Theil–Sen 稳健斜率（成对斜率中位数——
 * 单点噪声不扭曲趋势方向）思想：单次 run 的 pass@1 有噪声，**跨 run 的
 * 通过率走向**（改进/稳定/退化）才回答「这版提示词/这版模型在变好还是变坏」。
 * 门判定（GateDecision）给瞬时过/不过，本面给趋势方向。
 *
 * <p>纯函数零状态：吃按时间序排列的各 run 通过率（0..1）——Theil–Sen：
 * 全部成对斜率的中位数（对离群 run 稳健）；方向判定含死区（|斜率| <
 * {@link #STABLE_EPSILON} 视为稳定）。空/单 run 哨兵 INSUFFICIENT。
 */
public final class EvalPassRateTrend {

    /** 稳定死区（|斜率| < 0.005/run 视为稳定）。 */
    public static final double STABLE_EPSILON = 0.005;

    private EvalPassRateTrend() {
    }

    /** 趋势方向闭集。 */
    public enum Direction { INSUFFICIENT, DEGRADING, STABLE, IMPROVING }

    /**
     * @param runs         run 数
     * @param passRates    逐 run 通过率（时间序 0..1）
     * @param slopeMedian  Theil–Sen 斜率（成对斜率中位数；每 run 一个百分点）
     * @param direction    趋势方向（斜率 > +ε 改进 / < −ε 退化 / 其间稳定）
     */
    public record TrendReport(int runs, List<Double> passRates,
                              double slopeMedian, Direction direction) {
    }

    /** 审计入口：按时间序的逐 run 通过率（0..1）。 */
    public static TrendReport analyze(List<Double> passRates) {
        int n = passRates == null ? 0 : passRates.size();
        if (n < 2) {
            return new TrendReport(n, List.copyOf(passRates == null ? List.of() : passRates),
                    0d, Direction.INSUFFICIENT);
        }
        List<Double> slopes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                slopes.add((passRates.get(j) - passRates.get(i)) / (j - i));
            }
        }
        slopes.sort(Double::compare);
        int mid = slopes.size() / 2;
        double median = slopes.size() % 2 == 1
                ? slopes.get(mid)
                : (slopes.get(mid - 1) + slopes.get(mid)) / 2;
        Direction direction = median > STABLE_EPSILON ? Direction.IMPROVING
                : median < -STABLE_EPSILON ? Direction.DEGRADING : Direction.STABLE;
        return new TrendReport(n, List.copyOf(passRates), median, direction);
    }
}
