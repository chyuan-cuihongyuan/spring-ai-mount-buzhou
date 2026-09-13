package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.time.Instant;
import java.util.List;

/**
 * 评估 run 汇总结果（spec 52 §D / T193）：与 run 记录（store 落盘形态）同构。
 * passRate = passed / total（0 项约定 0.0——空集是合法状态非错误）。
 * datasetFingerprint（spec 82 / T319）：run 执行时的数据集内容指纹（旧记录 null）。
 */
public record EvalRunResult(String runId, String datasetName, Instant startedAt,
                            Instant finishedAt, int total, int passed, int failed,
                            int errored, List<EvalRunItemResult> items,
                            String datasetFingerprint) {

    public EvalRunResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** 9 参兼容构造（spec 82 前旧形态：无指纹）。 */
    public EvalRunResult(String runId, String datasetName, Instant startedAt,
                         Instant finishedAt, int total, int passed, int failed,
                         int errored, List<EvalRunItemResult> items) {
        this(runId, datasetName, startedAt, finishedAt, total, passed, failed, errored,
                items, null);
    }

    /** 0 项约定 0.0（0 通过 0 总计；NaN-safe 口径）。 */
    public double passRate() {
        return total == 0 ? 0.0 : (double) passed / total;
    }

    /**
     * impl-685 / spec 933：剪枝项计数（status = pruned 的条数——spec 901 算力止损
     * 事件量级）。
     */
    public long prunedCount() {
        return items.stream().filter(i -> "pruned".equals(i.status())).count();
    }

    /**
     * impl-685 / spec 933：有效通过率——分母排除 pruned 项（「真实评估质量」口径；
     * 剪枝 run 的 {@link #passRate()} 总量口径会被 pruned 稀释，双口径显式并存——
     * CI 硬门用总量口径防剪枝刷分）。分母为 0（全 pruned/空集）约定 0.0。
     */
    public double effectivePassRate() {
        int effective = total - (int) prunedCount();
        return effective <= 0 ? 0.0 : (double) passed / effective;
    }
}
