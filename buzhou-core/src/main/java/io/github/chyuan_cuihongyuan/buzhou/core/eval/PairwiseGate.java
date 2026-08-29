package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * A/B 胜率门（spec 101 §A / T375，spec 80 fog 项收口；Promptfoo model-compare gate /
 * LiteLLM model-router 验收思想）：CI 里「跑对比 → B 不劣于 A 才绿」的一步收口。
 * 判定口径沿用 spec 71：winRate = wins/(wins+ties+losses)（error 不入分母——诚实
 * 分离：基础设施故障不是质量结论）；门 = winRateA ≥ threshold（B 视角等价
 * winRateB ≤ 1-threshold）。预览截 {@value #PREVIEW_LIMIT} 条（CI 可读性——
 * EvalGate 同纪律）。
 *
 * @since 1.0.0
 */
public final class PairwiseGate {

    public static final int PREVIEW_LIMIT = 10;

    private final PairwiseEvalRunner runner;

    public PairwiseGate(PairwiseEvalRunner runner) {
        this.runner = runner;
    }

    /** 门判定结果（verdict + 汇总 + 逐项预览 + CI 单行摘要）。 */
    public record AbGateResult(boolean passed, String datasetName, String runId,
                               double winRateA, double winRateB, int winsA, int winsB,
                               int ties, int errors, int total,
                               List<String> itemPreviews) {

        /** CI 单行摘要（人读；B 视角——「B 相对 A 的表现」）。 */
        public String summary() {
            String head = passed ? "OK" : "FAIL";
            return "ab-gate " + head + " dataset=" + datasetName + " runId=" + runId
                    + " winRateA=" + String.format("%.3f", winRateA)
                    + " winRateB=" + String.format("%.3f", winRateB)
                    + " winsA=" + winsA + " winsB=" + winsB + " ties=" + ties
                    + " errors=" + errors + "/" + total;
        }
    }

    /**
     * 跑一次对比并按「A 胜率下限」判定（pass = winRateA ≥ minWinRateA；clamp 0..1）。
     * B 侧换版验收语义：minWinRateA = 0.4 即「B 不显著劣于 A」（A 胜率不超过 0.4）。
     */
    public AbGateResult enforce(String datasetName, AgentRuntime runtimeA, AgentRuntime runtimeB,
            int parallelism, double minWinRateA) {
        PairwiseEvalRunner.PairwiseEvalResult result = runner.compare(datasetName,
                runtimeA, runtimeB, parallelism);
        double clamped = Math.max(0.0, Math.min(1.0, minWinRateA));
        List<String> previews = new ArrayList<>();
        for (PairwiseEvalRunner.PairwiseItemResult item : result.items()) {
            if (previews.size() >= PREVIEW_LIMIT) {
                previews.add("…（其余 " + (result.items().size() - PREVIEW_LIMIT) + " 项省略）");
                break;
            }
            String line = item.error() != null
                    ? item.itemId() + " [error] " + item.error()
                    : item.itemId() + " [" + item.verdict().winner().name() + "] "
                            + item.verdict().reason();
            previews.add(line.lines().findFirst().orElse(line));
        }
        PairwiseEvalRunner.PairwiseSummary s = result.summary();
        return new AbGateResult(s.winRateA() >= clamped, datasetName, result.runId(),
                s.winRateA(), s.winRateB(), s.winsA(), s.winsB(), s.ties(), s.errors(),
                s.total(), List.copyOf(previews));
    }
}
