package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 评估结果只读查询面（spec 52 §E / T194）：run 列表（按 dataset 过滤、startedAt 倒序）、
 * 单 run 明细、最新 run。scanByPrefix 下推复用；不触碰写路径。
 */
public final class EvalQueryService {

    private final SessionStateStore stateStore;

    public EvalQueryService(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /** run 摘要行（无 items 明细——明细走 {@link #run(String)}）。 */
    public record EvalRunSummary(String runId, String datasetName, java.time.Instant startedAt,
                                 int total, int passed, int failed, int errored, double passRate) {

        static EvalRunSummary of(EvalRunResult r) {
            return new EvalRunSummary(r.runId(), r.datasetName(), r.startedAt(),
                    r.total(), r.passed(), r.failed(), r.errored(), r.passRate());
        }
    }

    /** 指定 dataset 的 run 摘要列表（startedAt 倒序）。 */
    public List<EvalRunSummary> runs(String datasetName) {
        return allRuns().stream()
                .filter(r -> r.datasetName().equals(datasetName))
                .sorted(Comparator.comparing(EvalRunSummary::startedAt).reversed())
                .toList();
    }

    /** 全部 run 摘要（跨 dataset；startedAt 倒序）。 */
    public List<EvalRunSummary> allRuns() {
        return stateStore.scanByPrefix(EvalDatasetStore.SESSION_ID, EvalRunner.RUN_PREFIX)
                .values().stream()
                .map(e -> EvalRunner.mapToResult(EvalRunner.decodeMap(e.value())))
                .map(EvalRunSummary::of)
                .sorted(Comparator.comparing(EvalRunSummary::startedAt).reversed())
                .toList();
    }

    /** 单 run 完整明细（未知 runId = empty）。 */
    public Optional<EvalRunResult> run(String runId) {
        return stateStore.get(EvalDatasetStore.SESSION_ID, EvalRunner.RUN_PREFIX + runId)
                .map(e -> EvalRunner.mapToResult(EvalRunner.decodeMap(e.value())));
    }

    /** dataset 最新 run（无 run = empty）。 */
    public Optional<EvalRunResult> latestRun(String datasetName) {
        return runs(datasetName).stream()
                .findFirst()
                .flatMap(s -> run(s.runId()));
    }
}
