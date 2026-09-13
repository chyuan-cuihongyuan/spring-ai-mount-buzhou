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

    /** impl-667 / spec 914：判定历史环容量（超限丢最旧——有界纪律）。 */
    public static final int HISTORY_CAPACITY = 16;

    private final EvalRunner runner;
    /** impl-667 / spec 914：判定历史环（新→旧；synchronized 单点读写）。 */
    private final java.util.ArrayDeque<GateDecision> history = new java.util.ArrayDeque<>();

    public EvalGate(EvalRunner runner) {
        this.runner = runner;
    }

    /** 单次门判定留痕（impl-667 / spec 914）。 */
    public record GateDecision(java.time.Instant at, String datasetName, String runId,
                               double threshold, double passRate, boolean passed) {
    }

    /** 判定历史快照（新→旧；不可变；容量 ≤ {@value #HISTORY_CAPACITY}）。 */
    public synchronized List<GateDecision> history() {
        return List.copyOf(history);
    }

    /** 门判定结果（verdict + 汇总 + 失败项预览 + CI 单行摘要）。 */
    public record GateResult(boolean passed, String datasetName, String runId,
                             double passRate, double threshold, int total, int passedCount,
                             int failed, int errored, List<String> failurePreviews,
                             double effectivePassRate) {

        /** 10 参兼容构造（spec 933 前旧形态：无有效口径，NaN = 语义「未计算」）。 */
        public GateResult(boolean passed, String datasetName, String runId,
                          double passRate, double threshold, int total, int passedCount,
                          int failed, int errored, List<String> failurePreviews) {
            this(passed, datasetName, runId, passRate, threshold, total, passedCount,
                    failed, errored, failurePreviews, Double.NaN);
        }

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
        GateResult result = new GateResult(run.passRate() >= clamped, datasetName, run.runId(),
                run.passRate(), clamped, run.total(), run.passed(), run.failed(),
                run.errored(), List.copyOf(previews), run.effectivePassRate());
        recordDecision(result);
        return result;
    }

    /**
     * impl-690 / spec 938：阈值漂移读面——相邻判定 threshold 不同的次数
     * （「CI 红了就调阈值」的流程不健康信号显形）。纯函数零副作用。
     */
    public record ThresholdDrift(int transitions, int sampled) {
    }

    public static ThresholdDrift thresholdDrift(List<GateDecision> historyList) {
        if (historyList == null || historyList.size() < 2) {
            return new ThresholdDrift(0, Math.max(0, historyList == null ? 0 : historyList.size() - 1));
        }
        int transitions = 0;
        for (int i = 1; i < historyList.size(); i++) {
            if (historyList.get(i).threshold() != historyList.get(i - 1).threshold()) {
                transitions++;
            }
        }
        return new ThresholdDrift(transitions, historyList.size() - 1);
    }

    /**
     * impl-692 / spec 943：k 次防抖门——「k 次全过才过」的从严门（flaky 数据集/
     * 抖动 judge 误报防护）。循环 k 次既有 enforce（落盘/历史/指标全继承），
     * 任一失败即 fail。k ∈ [1, HISTORY_CAPACITY]（防历史环溢出丢失前序判定）。
     * 返回最后一次 GateResult；全量判定经 {@link #history()} 可查。
     */
    public GateResult enforceStable(String datasetName, Evaluator evaluator,
                                    double threshold, int k) {
        if (k < 1 || k > HISTORY_CAPACITY) {
            throw new IllegalArgumentException("k 须 ∈ [1, " + HISTORY_CAPACITY + "]，收到 " + k);
        }
        GateResult last = null;
        for (int i = 0; i < k; i++) {
            last = enforce(datasetName, evaluator, threshold);
            if (!last.passed()) {
                // 早停：任一失败即 fail（后续判定浪费算力）——已跑判定留史可查
                break;
            }
        }
        return last;
    }

    /** impl-667 / spec 914：判定入史（环形有界；synchronized 单点）。 */
    private synchronized void recordDecision(GateResult result) {
        if (history.size() >= HISTORY_CAPACITY) {
            history.pollLast();
        }
        history.addFirst(new GateDecision(java.time.Instant.now(), result.datasetName(),
                result.runId(), result.threshold(), result.passRate(), result.passed()));
    }

    private static int countNonPass(EvalRunResult run) {
        return run.failed() + run.errored();
    }
}
