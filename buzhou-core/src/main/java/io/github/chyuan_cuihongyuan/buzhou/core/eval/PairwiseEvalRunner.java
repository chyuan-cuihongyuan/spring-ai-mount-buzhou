package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A/B 成对评估 runner（spec 71 §A / T293 / effort#31，Ragas pairwise eval / LiteLLM
 * model-compare 思想）：同一数据集在两个 runtime（模型/配置 A 与 B）各跑一遍，逐项
 * {@link PairwiseJudge} 双向裁定（位置偏差消解内置），汇总胜率。
 *
 * <p><b>组合面</b>：EvalRunner 的项执行语义（隔离会话）+ PairwiseJudge 的成对裁决 +
 * 虚拟线程并行（默认 1）。诚实边界：judge 判别力归模型（#21/#23 同口径）；A/B 两路
 * 执行异常按该项 error 记（不裁胜负）；胜率 = wins/(wins+ties+losses) 口径单独给
 * （error 项不计入分母——诚实分离）。
 *
 * @since 1.0.0
 */
public final class PairwiseEvalRunner {

    /** 单项成对裁决（item 溯源 + 双输出 + 裁决或 error）。 */
    public record PairwiseItemResult(String itemId, String outputA, String outputB,
                                     PairwiseJudge.PairwiseVerdict verdict, String error) {

        static PairwiseItemResult error(String itemId, String error) {
            return new PairwiseItemResult(itemId, null, null, null, error);
        }
    }

    /** A/B 汇总（winsA/winsB/ties/errors + 胜率）。 */
    public record PairwiseSummary(int winsA, int winsB, int ties, int errors,
                                  int total, double winRateA, double winRateB) {
    }

    private final EvalDatasetStore datasetStore;
    private final PairwiseJudge judge;

    public PairwiseEvalRunner(EvalDatasetStore datasetStore, PairwiseJudge judge) {
        this.datasetStore = datasetStore;
        this.judge = judge;
    }

    /**
     * A/B 对比跑：逐项双 runtime 执行 + 成对裁决；parallelism clamp 1..32（虚拟线程）。
     * 结果按数据集项序聚合（确定性）。
     */
    public PairwiseEvalResult compare(String datasetName, AgentRuntime runtimeA,
            AgentRuntime runtimeB, int parallelism) {
        List<EvalItem> items = datasetStore.dataset(datasetName)
                .map(meta -> datasetStore.items(datasetName))
                .orElseThrow(() -> new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                        io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.EVAL_OPERATION_INVALID,
                        "数据集未建：" + datasetName + "（修法：先 createDataset 再 compare）"));
        String runId = "ab" + System.currentTimeMillis() + "-"
                + String.format("%04x", ThreadLocalRandom.current().nextInt(0x10000));
        Instant startedAt = Instant.now();
        int workers = Math.max(1, Math.min(32, parallelism));
        PairwiseItemResult[] byIndex = new PairwiseItemResult[items.size()];
        if (workers == 1 || items.size() <= 1) {
            for (int i = 0; i < items.size(); i++) {
                byIndex[i] = compareItem(runId, items.get(i), runtimeA, runtimeB);
            }
        } else {
            List<java.util.concurrent.Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                final EvalItem item = items.get(i);
                tasks.add(() -> {
                    byIndex[index] = compareItem(runId, item, runtimeA, runtimeB);
                    return null;
                });
            }
            try (java.util.concurrent.ExecutorService pool =
                    java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                pool.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("A/B 评估并行执行被中断", e);
            }
        }
        List<PairwiseItemResult> results = List.of(byIndex);
        int winsA = 0;
        int winsB = 0;
        int ties = 0;
        int errors = 0;
        for (PairwiseItemResult r : results) {
            if (r.error() != null) {
                errors++;
            } else if (r.verdict().winner() == PairwiseJudge.Winner.WINNER_A) {
                winsA++;
            } else if (r.verdict().winner() == PairwiseJudge.Winner.WINNER_B) {
                winsB++;
            } else {
                ties++;
            }
        }
        int decided = winsA + winsB + ties;
        return new PairwiseEvalResult(runId, datasetName, startedAt, Instant.now(), results,
                new PairwiseSummary(winsA, winsB, ties, errors, results.size(),
                        decided == 0 ? 0.0 : (double) winsA / decided,
                        decided == 0 ? 0.0 : (double) winsB / decided));
    }

    /** 单项：双 runtime 各执行（异常 → 该项 error 不裁胜负）+ judge 双向裁定。 */
    private PairwiseItemResult compareItem(String runId, EvalItem item,
            AgentRuntime runtimeA, AgentRuntime runtimeB) {
        String outputA;
        String outputB;
        try (AgentSession sessionA = runtimeA.spawn("buzhou-eval", "eval",
                "ab-" + runId + "-a-" + item.id())) {
            outputA = sessionA.chat(item.input());
        } catch (Exception e) {
            return PairwiseItemResult.error(item.id(),
                    "A 路执行异常：" + e.getClass().getSimpleName() + ": "
                            + String.valueOf(e.getMessage()).lines().findFirst().orElse(""));
        }
        try (AgentSession sessionB = runtimeB.spawn("buzhou-eval", "eval",
                "ab-" + runId + "-b-" + item.id())) {
            outputB = sessionB.chat(item.input());
        } catch (Exception e) {
            return PairwiseItemResult.error(item.id(),
                    "B 路执行异常：" + e.getClass().getSimpleName() + ": "
                            + String.valueOf(e.getMessage()).lines().findFirst().orElse(""));
        }
        return new PairwiseItemResult(item.id(), outputA, outputB,
                judge.compare(item.input(), outputA, outputB), null);
    }

    /** A/B 对比结果（项明细 + 汇总）。 */
    public record PairwiseEvalResult(String runId, String datasetName, Instant startedAt,
                                     Instant finishedAt, List<PairwiseItemResult> items,
                                     PairwiseSummary summary) {
    }
}
