package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.List;

/**
 * 评估门阈值敏感性扫描（spec 1437 兄弟轮编号见台账：R42 = effort #1442 /
 * 票 T2185 + T2186 / impl 1094）——scikit-learn validation_curve（超参
 * 敏感性曲线）思想：评估门阈值（spec 513 PASS 线）定得准不准，要看**分数
 * 密集带**离它多近——阈值 ±δ 的微小移动会翻转多少判定？翻转越多说明门
 * 越脆弱（分数噪声直接决定过/不过），越少说明门立在分数稀疏带上（稳健）。
 *
 * <p>纯函数零状态：吃分数列表 + 当前阈值 + 扫描半宽 δ；PASS 语义 =
 * score ≥ threshold（与 EvalGate 同向）。翻转只计 δ 带内的边界分数，
 * 带外分数对阈值微移免疫（稳健部分不占报告）。
 */
public final class GateThresholdSensitivity {

    private GateThresholdSensitivity() {
    }

    /**
     * @param totalScores  分数总数
     * @param bandCount    δ 带内分数数（[threshold−δ, threshold+δ)）
     * @param tightenFlips 阈值上调 δ 将翻转为 FAIL 的当前 PASS 数（∈ [threshold, threshold+δ)）
     * @param loosenFlips  阈值下调 δ 将翻转为 PASS 的当前 FAIL 数（∈ [threshold−δ, threshold)）
     */
    public record SensitivityReport(int totalScores, int bandCount, int tightenFlips,
                                    int loosenFlips) {

        /** 敏感率 = 带内分数占比（0 分数哨兵 -1）；越低越稳健。 */
        public double sensitivityRatio() {
            return totalScores == 0 ? -1d : (double) bandCount / totalScores;
        }
    }

    /** 扫描入口：scores 任意序；δ ≥ 0。 */
    public static SensitivityReport analyze(List<Double> scores, double threshold, double delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("δ 须非负：" + delta);
        }
        if (scores == null || scores.isEmpty()) {
            return new SensitivityReport(0, 0, 0, 0);
        }
        int band = 0;
        int tighten = 0;
        int loosen = 0;
        for (double score : scores) {
            boolean inUpperBand = score >= threshold && score < threshold + delta;
            boolean inLowerBand = score >= threshold - delta && score < threshold;
            if (inUpperBand || inLowerBand) {
                band++;
            }
            if (inUpperBand) {
                tighten++;
            }
            if (inLowerBand) {
                loosen++;
            }
        }
        return new SensitivityReport(scores.size(), band, tighten, loosen);
    }
}
