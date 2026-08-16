package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.time.Instant;
import java.util.List;

/**
 * 评估 run 汇总结果（spec 52 §D / T193）：与 run 记录（store 落盘形态）同构。
 * passRate = passed / total（0 项约定 0.0——空集是合法状态非错误）。
 */
public record EvalRunResult(String runId, String datasetName, Instant startedAt,
                            Instant finishedAt, int total, int passed, int failed,
                            int errored, List<EvalRunItemResult> items) {

    public EvalRunResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** 0 项约定 0.0（0 通过 0 总计；NaN-safe 口径）。 */
    public double passRate() {
        return total == 0 ? 0.0 : (double) passed / total;
    }
}
