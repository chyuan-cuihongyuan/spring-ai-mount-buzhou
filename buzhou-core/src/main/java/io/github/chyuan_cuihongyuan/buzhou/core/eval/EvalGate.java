package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估回归门（spec 80 §A / T313，Promptfoo eval CI gate / LangSmith eval-as-gate
 * 借鉴）：CI 里「跑数据集 → 低于阈值即红」的一步收口。跑一个 {@link EvalRunner}
 * run，passRate &ge; threshold 判过（error 计入分母——回归门语义从严：基础设施
 * 故障不是绿）。失败项预览（fail/error 各带 itemId + detail 首行）截 {@value
 * #PREVIEW_LIMIT} 条——CI 日志可读性优先，不刷屏。
 *
 * <p>run 本身的落盘/事件/注册表语义由 EvalRunner 既有管线承担（gate 是判定面，
 * 不重复执行面）。
 */
public final class EvalGate {

    /** 失败项预览上限（防 CI 日志刷屏）。 */
    public static final int PREVIEW_LIMIT = 10;

    private final EvalRunner runner;

    public EvalGate(EvalRunner runner) {
        this.runner = runner;
    }

    /** 门判定结果（verdict + 汇总 + 失败项预览 + CI 单行摘要）。 */
    public record GateResult(boolean passed, String datasetName, String runId,
                             double passRate, double threshold, int total, int passedCount,
                             int failed, int errored, List<String> failurePreviews) {

        /** CI 单行摘要（人读；exit-code 由宿主按 {@link #passed()} 定）。 */
        public String summary() {
            String head = passed ? "OK" : "FAIL";
            return "eval-gate " + head + " dataset=" + datasetName + " runId=" + runId
                    + " passRate=" + String.format("%.3f", passRate)
                    + " threshold=" + String.format("%.3f", threshold)
                    + " passed=" + passedCount + " failed=" + failed + " errored=" + errored
                    + "/" + total;
        }
    }

    /**
     * 跑一次 run 并按阈值判定（serial；需要并行用 {@link #enforce(String, Evaluator,
     * double, int)}）。threshold clamp 0..1。
     */
    public GateResult enforce(String datasetName, Evaluator evaluator, double threshold) {
        return enforce(datasetName, evaluator, threshold, 1);
    }

    /** 带并行度的门判定（透传 EvalRunner.run——执行语义同 spec 68）。 */
    public GateResult enforce(String datasetName, Evaluator evaluator, double threshold,
            int parallelism) {
        EvalRunResult run = runner.run(datasetName, evaluator, parallelism);
        double clamped = Math.max(0.0, Math.min(1.0, threshold));
        List<String> previews = new ArrayList<>();
        for (EvalRunItemResult item : run.items()) {
            if (EvalRunItemResult.STATUS_PASS.equals(item.status())) {
                continue;
            }
            if (previews.size() >= PREVIEW_LIMIT) {
                previews.add("…（其余 " + (countNonPass(run) - PREVIEW_LIMIT) + " 项省略）");
                break;
            }
            String detail = item.detail() == null ? "" : item.detail().lines()
                    .findFirst().orElse("");
            previews.add(item.itemId() + " [" + item.status() + "] " + detail);
        }
        return new GateResult(run.passRate() >= clamped, datasetName, run.runId(),
                run.passRate(), clamped, run.total(), run.passed(), run.failed(),
                run.errored(), List.copyOf(previews));
    }

    private static int countNonPass(EvalRunResult run) {
        return run.failed() + run.errored();
    }
}
