package io.github.chyuan_cuihongyuan.buzhou.core.health;

/**
 * SLO 多窗燃烧率联合判定（spec 817 / T1135，Google SRE Workbook multi-window
 * multi-burn-rate 告警借鉴）：快窗（如 5m，高阈值）与慢窗（如 1h，低阈值）
 * <b>同时</b>超阈才判定事故——快窗单超是毛刺（不响），慢窗单超是慢性渗漏
 * （也不响）；两窗共振才是真事故。样本不足一律不判（诚实折中）。
 *
 * <p>纯函数判定脑：两窗 burnRate/样本量由调用方从 {@link ErrorBudget}
 * （各自窗长配置）取——本类不持有时间窗（组合优于改动，ErrorBudget 零变更）。
 */
public final class SloMultiWindowBurn {

    /** 判定结果（不可变）。 */
    public record Verdict(boolean incident, double fastBurn, double slowBurn,
                          long fastSamples, long slowSamples, String reason) {
    }

    private SloMultiWindowBurn() {
    }

    /**
     * 多窗联合判定：incident = fastBurn ≥ fastThreshold 且 slowBurn ≥
     * slowThreshold 且两窗样本均 ≥ minSamples；否则不判（reason 说明）。
     * 阈值须 &gt;0、minSamples ≥1、burn 允许 0（无错误）。
     */
    public static Verdict evaluate(double fastBurn, double slowBurn,
                                   long fastSamples, long slowSamples,
                                   double fastThreshold, double slowThreshold, long minSamples) {
        if (fastThreshold <= 0 || slowThreshold <= 0) {
            throw new IllegalArgumentException("阈值须 >0（fast=" + fastThreshold + ", slow=" + slowThreshold + "）");
        }
        if (minSamples < 1) {
            throw new IllegalArgumentException("minSamples >= 1（当前 " + minSamples + "）");
        }
        boolean fastHot = fastBurn >= fastThreshold;
        boolean slowHot = slowBurn >= slowThreshold;
        boolean enough = fastSamples >= minSamples && slowSamples >= minSamples;

        if (!enough) {
            return new Verdict(false, fastBurn, slowBurn, fastSamples, slowSamples,
                    "样本不足（fast=" + fastSamples + ", slow=" + slowSamples + ", min=" + minSamples + "）");
        }
        if (fastHot && slowHot) {
            return new Verdict(true, fastBurn, slowBurn, fastSamples, slowSamples,
                    "双窗共振：快窗 " + fastBurn + " ≥ " + fastThreshold + " 且慢窗 "
                            + slowBurn + " ≥ " + slowThreshold + "——真事故");
        }
        if (fastHot) {
            return new Verdict(false, fastBurn, slowBurn, fastSamples, slowSamples,
                    "快窗独热——疑似毛刺（慢窗 " + slowBurn + " 未达 " + slowThreshold + "）");
        }
        if (slowHot) {
            return new Verdict(false, fastBurn, slowBurn, fastSamples, slowSamples,
                    "慢窗独热——慢性渗漏（快窗 " + fastBurn + " 未达 " + fastThreshold + "）");
        }
        return new Verdict(false, fastBurn, slowBurn, fastSamples, slowSamples, "两窗均未超阈");
    }
}
