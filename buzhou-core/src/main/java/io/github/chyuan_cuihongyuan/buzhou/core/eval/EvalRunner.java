package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 批次评估 runner（spec 52 §D / T193）：dataset 逐项 spawn 独立评估会话
 * （appId={@code buzhou-eval}、sessionId={@code eval-<runId>-i<itemId>}，项粒度隔离、
 * 不占业务会话命名空间）→ chat 执行 → {@link Evaluator} 打分 → run 记录落
 * {@link EvalDatasetStore#SESSION_ID} 合成会话（键 {@code eval.run.<runId>}）。
 *
 * <p>顺序执行；单项执行异常记 error 不断批；空数据集 = 零项 run（合法状态，
 * passRate 约定 0.0）。事件外发在 {@code EvalSessionEvents}（T195）补。
 */
public final class EvalRunner {

    /** run 记录键前缀（与数据集键同合成会话、不同前缀段）。 */
    static final String RUN_PREFIX = "eval.run.";

    static final int ACTUAL_PREVIEW_LIMIT = 2048;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentRuntime runtime;
    private final EvalDatasetStore datasetStore;
    private final SessionStateStore stateStore;

    public EvalRunner(AgentRuntime runtime, EvalDatasetStore datasetStore,
            SessionStateStore stateStore) {
        this.runtime = runtime;
        this.datasetStore = datasetStore;
        this.stateStore = stateStore;
    }

    /** 执行一次评估 run（dataset 未建 fail-fast 挂 EVAL_OPERATION_INVALID）。 */
    public EvalRunResult run(String datasetName, Evaluator evaluator) {
        List<EvalItem> items = datasetStore.dataset(datasetName)
                .map(meta -> datasetStore.items(datasetName))
                .orElseThrow(() -> new BuzhouException(ErrorCode.EVAL_OPERATION_INVALID,
                        "数据集未建：" + datasetName + "（修法：先 createDataset 再 run）"));
        String runId = "r" + System.currentTimeMillis() + "-"
                + String.format("%04x", ThreadLocalRandom.current().nextInt(0x10000));
        Instant startedAt = Instant.now();
        List<EvalRunItemResult> results = new ArrayList<>();
        for (EvalItem item : items) {
            results.add(runItem(runId, item, evaluator));
        }
        Instant finishedAt = Instant.now();
        int passed = (int) results.stream().filter(r -> EvalRunItemResult.STATUS_PASS.equals(r.status())).count();
        int failed = (int) results.stream().filter(r -> EvalRunItemResult.STATUS_FAIL.equals(r.status())).count();
        int errored = (int) results.stream().filter(r -> EvalRunItemResult.STATUS_ERROR.equals(r.status())).count();
        EvalRunResult result = new EvalRunResult(runId, datasetName, startedAt, finishedAt,
                results.size(), passed, failed, errored, results);
        stateStore.put(EvalDatasetStore.SESSION_ID,
                new StateEntry(RUN_PREFIX + runId, encode(resultToMap(result)),
                        "eval", 0, null, finishedAt));
        emitRunCompleted(result, startedAt, finishedAt);
        return result;
    }

    /**
     * spec 52 §F / T195：run 完成事件（total > 0 才发——空集无评估发生，事件语义为
     * 「评估完成」非「run 建档」）。实现裁定：spec 原案「末项评估会话上发」改为独立收尾
     * 会话 {@code eval-<runId>-done}（项会话逐项 close 的资源语义优先；诚实入档）。
     */
    private void emitRunCompleted(EvalRunResult result, java.time.Instant startedAt,
            java.time.Instant finishedAt) {
        if (result.total() == 0) {
            return;
        }
        java.util.Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", result.runId());
        payload.put("datasetName", result.datasetName());
        payload.put("total", result.total());
        payload.put("passed", result.passed());
        payload.put("failed", result.failed());
        payload.put("errored", result.errored());
        payload.put("passRate", result.passRate());
        payload.put("durationMs", java.time.Duration.between(startedAt, finishedAt).toMillis());
        try (var done = runtime.spawn("buzhou-eval", "eval", "eval-" + result.runId() + "-done")) {
            done.emitEvent("eval.run.completed", payload);
        }
    }

    private EvalRunItemResult runItem(String runId, EvalItem item, Evaluator evaluator) {
        long start = System.nanoTime();
        String actual = null;
        try (var session = runtime.spawn("buzhou-eval", "eval",
                "eval-" + runId + "-i" + item.id())) {
            actual = session.chat(item.input());
        } catch (Exception e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new EvalRunItemResult(item.id(), EvalRunItemResult.STATUS_ERROR,
                    "执行异常：" + e.getClass().getSimpleName() + ": "
                            + String.valueOf(e.getMessage()).lines().findFirst().orElse(""),
                    null, ms);
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        EvalScore score = evaluator.evaluate(actual, item.expected(), item);
        if (score == null) {
            return new EvalRunItemResult(item.id(), EvalRunItemResult.STATUS_ERROR,
                    "评估器返回 null（违反 SPI 契约，按 error 记录）",
                    preview(actual), ms);
        }
        String status = score.passed() ? EvalRunItemResult.STATUS_PASS : EvalRunItemResult.STATUS_FAIL;
        return new EvalRunItemResult(item.id(), status, score.detail(), preview(actual), ms);
    }

    private static String preview(String actual) {
        if (actual == null) {
            return null;
        }
        return actual.length() > ACTUAL_PREVIEW_LIMIT
                ? actual.substring(0, ACTUAL_PREVIEW_LIMIT) + "…" : actual;
    }

    // ---- run 记录编解码（store 落盘形态） ----

    static Map<String, Object> resultToMap(EvalRunResult r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("runId", r.runId());
        map.put("datasetName", r.datasetName());
        map.put("startedAt", r.startedAt().toString());
        map.put("finishedAt", r.finishedAt().toString());
        map.put("total", r.total());
        map.put("passed", r.passed());
        map.put("failed", r.failed());
        map.put("errored", r.errored());
        map.put("passRate", r.passRate());
        List<Map<String, Object>> items = new ArrayList<>();
        for (EvalRunItemResult item : r.items()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", item.itemId());
            row.put("status", item.status());
            row.put("detail", item.detail());
            row.put("actual", item.actualPreview());
            row.put("durationMs", item.durationMs());
            items.add(row);
        }
        map.put("items", items);
        return map;
    }

    static EvalRunResult mapToResult(Map<String, Object> map) {
        List<EvalRunItemResult> items = new ArrayList<>();
        for (Object row : (List<?>) map.getOrDefault("items", List.of())) {
            Map<?, ?> m = (Map<?, ?>) row;
            items.add(new EvalRunItemResult(
                    (String) m.get("itemId"),
                    (String) m.get("status"),
                    (String) m.get("detail"),
                    (String) m.get("actual"),
                    m.get("durationMs") == null ? 0L : ((Number) m.get("durationMs")).longValue()));
        }
        return new EvalRunResult(
                (String) map.get("runId"),
                (String) map.get("datasetName"),
                Instant.parse(String.valueOf(map.get("startedAt"))),
                Instant.parse(String.valueOf(map.get("finishedAt"))),
                ((Number) map.getOrDefault("total", 0)).intValue(),
                ((Number) map.getOrDefault("passed", 0)).intValue(),
                ((Number) map.getOrDefault("failed", 0)).intValue(),
                ((Number) map.getOrDefault("errored", 0)).intValue(),
                items);
    }

    static String encode(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION, "评估 run 记录编码失败：" + e.getMessage(), e);
        }
    }

    static Map<String, Object> decodeMap(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION, "评估 run 记录解析失败：" + e.getMessage(), e);
        }
    }
}
