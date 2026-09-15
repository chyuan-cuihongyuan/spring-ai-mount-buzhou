package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;

/**
 * 双窗口漂移检测（spec 1854 / T2909 / impl 1455）——Netflix/Google SRE
 * 双窗口异常检测惯例（近期窗 vs 基线窗）：均值漂移要**双闸显著**——
 * 绝对闸（差值 ≥ minAbsoluteDelta——量级上重要吗）与相对闸（差值 ≥
 * minEffectRatio × |基线|——比例上重要吗）同时过才报漂移——单闸要么被
 * 噪声刷屏（只看相对）要么漏报缓变（只看绝对）。方向分开：升漂
 *（变差——延迟涨/错误涨）与降漂（变好——优化生效）处置相反。
 *
 * <p>纯函数零状态、只判漂不归因（根因归宿主）；基线均值为 0 时相对闸
 * 分母取 max(|基线|, 1.0)（退化绝对口径）。
 */
public final class WindowShiftDetector {

    private WindowShiftDetector() {
    }

    /** 漂移三态：STABLE 稳 / SHIFTED_UP 升漂（变差）/ SHIFTED_DOWN 降漂（变好）。 */
    public enum Shift {

        /** 双闸未过——稳定。 */
        STABLE,

        /** 升漂——变差（延迟涨/错误涨/成本涨）。 */
        SHIFTED_UP,

        /** 降漂——变好（优化生效）。 */
        SHIFTED_DOWN
    }

    /**
     * 漂移判定。契约：两窗非空、样本非 null 非 NaN、minAbsoluteDelta ≥ 0、
     * minEffectRatio ≥ 0（fail-fast）；语义：|recentMean − baselineMean| 同时
     * ≥ 绝对闸与 ≥ 相对闸（分母 max(|baselineMean|, 1.0)）才漂。
     */
    public static Shift detect(List<Double> baseline, List<Double> recent,
                               double minAbsoluteDelta, double minEffectRatio) {
        if (baseline == null || baseline.isEmpty() || recent == null
                || recent.isEmpty()) {
            throw new IllegalArgumentException("两窗均须非空");
        }
        if (Double.isNaN(minAbsoluteDelta) || minAbsoluteDelta < 0
                || Double.isNaN(minEffectRatio) || minEffectRatio < 0) {
            throw new IllegalArgumentException(String.format(
                    "闸参非法：delta=%s, ratio=%s（均须 ≥ 0 非 NaN）",
                    minAbsoluteDelta, minEffectRatio));
        }
        double baselineMean = mean(baseline);
        double recentMean = mean(recent);
        double diff = recentMean - baselineMean;
        double magnitude = Math.abs(diff);
        double relativeDenominator = Math.max(Math.abs(baselineMean), 1.0d);
        boolean doubleGated = magnitude >= minAbsoluteDelta
                && magnitude >= minEffectRatio * relativeDenominator;
        if (!doubleGated) {
            return Shift.STABLE;
        }
        return diff > 0 ? Shift.SHIFTED_UP : Shift.SHIFTED_DOWN;
    }

    private static double mean(List<Double> window) {
        double sum = 0;
        for (Double v : window) {
            if (v == null || v.isNaN()) {
                throw new IllegalArgumentException("样本不能为 null 或 NaN");
            }
            sum += v;
        }
        return sum / window.size();
    }
}
