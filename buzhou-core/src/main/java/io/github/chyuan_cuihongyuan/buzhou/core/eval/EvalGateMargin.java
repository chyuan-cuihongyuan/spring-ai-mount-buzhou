package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.List;

/**
 * 门限边际直方读面（L 会话 1700 系 R6 = effort #1705 / spec 1705 /
 * 票 T2611 + T2612 / impl 1305）——Google SRE 告警边际 / SPRT 边际思想：
 * 门判定「过 0.003」与「过 0.2」天壤之别——边际 = |passRate − threshold|，
 * 最小边际与危险带内计数回答「这轮门过得多悬」。
 *
 * <p>纯函数零状态：吃逐 run 通过率与门限，吐边际列表/最小/最大/带内计数。
 *
 * @since 1.0.0
 */
public final class EvalGateMargin {

    private EvalGateMargin() {
    }

    /**
     * @param runs       run 数
     * @param threshold  门限（0..1）
     * @param margins    逐 run 边际（入参序，非负）
     * @param minMargin  最小边际（runs=0 哨兵 −1）
     * @param maxMargin  最大边际
     */
    public record MarginReport(int runs, double threshold, List<Double> margins,
                               double minMargin, double maxMargin) {

        /** 危险带内 run 数（边际 ≤ band）——「多悬」的直接读数。 */
        public int withinBand(double band) {
            return (int) margins.stream().filter(m -> m <= band).count();
        }
    }

    /** 审计入口：逐 run 通过率（0..1，时间序无要求）与门限。 */
    public static MarginReport analyze(List<Double> passRates, double threshold) {
        List<Double> data = passRates == null ? List.of() : passRates;
        List<Double> margins = data.stream()
                .map(rate -> Math.abs(rate - threshold))
                .toList();
        if (margins.isEmpty()) {
            return new MarginReport(0, threshold, List.of(), -1d, -1d);
        }
        double min = margins.stream().mapToDouble(Double::doubleValue).min().orElse(-1d);
        double max = margins.stream().mapToDouble(Double::doubleValue).max().orElse(-1d);
        return new MarginReport(margins.size(), threshold, List.copyOf(margins), min, max);
    }
}
