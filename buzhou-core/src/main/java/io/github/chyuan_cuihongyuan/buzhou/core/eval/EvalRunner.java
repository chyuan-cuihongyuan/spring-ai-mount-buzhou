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

    /** spec 708：memo 键前缀（同合成会话、run/数据集前缀段外独立）。 */
    static final String MEMO_PREFIX = "eval.memo.";

    private static final System.Logger LOGGER = System.getLogger(EvalRunner.class.getName());

    static final int ACTUAL_PREVIEW_LIMIT = 2048;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentRuntime runtime;
    private final EvalDatasetStore datasetStore;
    private final SessionStateStore stateStore;

    /** spec 520 / T791：run 预算（字符估算累计上限；0 = 关——默认零行为变化）。 */
    private volatile long runBudgetChars;

    /** spec 535 / T823：error 项重试一次（抖动缓解——默认关）。 */
    private volatile boolean errorRetryOnce;

    /** run 前数据集期望门禁（spec 150 §A / T503，Great Expectations 借鉴；null = 无门禁零变化）。 */
    private volatile DatasetExpectations expectations;
    /** spec 198 §A / T561：宽松档（未过只 WARN 不拦）。 */
    private volatile boolean expectationsWarnOnly;
    /** spec 609 / T868：项级超时预算（null = 不设——零行为变化；挂死项收敛 error 不拖死整跑）。 */
    private volatile java.time.Duration perItemTimeout;

    /** spec 708 / T1016：项级记忆化键（null/blank = 关——默认零行为；判定身份指纹由调用方拼装）。 */
    private volatile String memoizationKey;

    /** spec 718 / T1036：漂移基线窗（0 = 关——默认零行为）。 */
    private volatile int driftWindow;
    /** spec 718：漂移告警线（绝对差值 ∈ (0,1]）。 */
    private volatile double driftWarnShift;
    /** spec 718：最近一次 run 的 passRate−baseline（无基线 = NaN——读数面）。 */
    private volatile double lastDriftDelta = Double.NaN;

    /** spec 734 / T1068：最近一次 run 与其前一次的数据集指纹是否不同（首跑 false——读数面）。 */
    private volatile boolean lastFingerprintChanged;

    public EvalRunner(AgentRuntime runtime, EvalDatasetStore datasetStore,
            SessionStateStore stateStore) {
        this.runtime = runtime;
        this.datasetStore = datasetStore;
        this.stateStore = stateStore;
    }

    /**
     * spec 520 / T791：run 预算闸（AWS Budgets/pytest maxfail 早停语义）——
     * 逐项 input+expected 字符估算累计超上限即早停：剩余项不执行、记 error
     * 三态（detail [RUN-BUDGET]），run 照常落盘（partial 显式可见）。
     * 0 = 关（默认零行为变化）。
     */
    public void setRunBudgetChars(long chars) {
        if (chars < 0) {
            throw new IllegalArgumentException("run 预算非负（0 = 关；当前 " + chars + "）");
        }
        this.runBudgetChars = chars;
    }

    /**
     * spec 535 / T823：error 项重试一次（抖动缓解）——仅 STATUS_ERROR 项
     * 重跑一次（语义 fail 不重试——重试语义失败会掩盖真实回归）；取第二次
     * 结果为准，detail 前缀 [RETRIED] 留痕。默认关。
     */
    public void setErrorRetryOnce(boolean retryOnce) {
        this.errorRetryOnce = retryOnce;
    }

    /**
     * spec 609 / T868 / impl 462（pytest-timeout 借鉴）：项级超时预算——单项执行超时
     * 收敛为该条 error（中断挂死项的虚拟线程），其余项照跑、run 必完成；指标
     * {@code buzhou.eval.item.timeouts} 留痕。null / 非正值拒绝（不设 = 默认）。
     */
    public void setPerItemTimeout(java.time.Duration timeout) {
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("perItemTimeout 必须为正时长（当前 " + timeout + "）");
        }
        this.perItemTimeout = timeout;
    }

    /**
     * spec 708 / T1016（scikit-learn Pipeline memory 借鉴）：项级记忆化——
     * sig = sha256(dataset|itemId|input|expected|key) 与上轮一致即复用判定
     * 跳过模型调用（detail 加 [MEMO] 前缀）；数据集就地改项自动失配。
     * key 是判定身份指纹（judge 实现+模型+模板版本由调用方拼装），换 key =
     * 全量重跑。null/blank = 关（默认零行为）。ERROR 项不缓存（瞬时故障
     * 不固化）；memo 读写失败降级直跑（优化不是依赖）。
     */
    public void setMemoizationKey(String key) {
        this.memoizationKey = key == null || key.isBlank() ? null : key;
    }

    /**
     * spec 718 / T1036（Evidently drift 借鉴）：通过率漂移基线——run 完成后取
     * 同数据集早于本次的最近 {@code window} 次 passRate 均值为基线，
     * |当前−基线| ≥ warnShift → WARN+计数。无历史样本跳过；只告警不阻断。
     * 默认关（window=0）。window≥0、warnShift∈(0,1] fail-fast。
     */
    public void setDriftBaseline(int window, double warnShift) {
        if (window < 0) {
            throw new IllegalArgumentException("漂移窗口非负（0 = 关；当前 " + window + "）");
        }
        if (window > 0 && !(warnShift > 0.0 && warnShift <= 1.0)) {
            throw new IllegalArgumentException("漂移告警线必须在 (0,1]（当前 " + warnShift + "）");
        }
        this.driftWindow = window;
        this.driftWarnShift = warnShift;
    }

    /** spec 718：最近一次 run 的 passRate−baseline（无基线 = NaN）。 */
    public double lastDriftDelta() {
        return lastDriftDelta;
    }

    /** spec 734：最近一次 run 的数据集指纹相对其前一次是否变化（首跑 false）。 */
    public boolean lastFingerprintChanged() {
        return lastFingerprintChanged;
    }

    /**
     * spec 734：数据集指纹变更检测（82 指纹入档的消费信号）——当前 run 指纹
     * 与最近一次历史 run 不同即置位+计数。就地改项/增删项都会变指纹——
     * diff 明细归 EvalRunDiff，本面只做「变了」的一眼信号。
     */
    private void checkFingerprintChange(String datasetName, EvalRunResult result) {
        try {
            String latest = null;
            Instant latestAt = Instant.EPOCH;
            for (StateEntry entry : stateStore
                    .scanByPrefix(EvalDatasetStore.SESSION_ID, RUN_PREFIX).values()) {
                try {
                    Map<String, Object> map = decodeMap(entry.value());
                    if (!datasetName.equals(map.get("datasetName"))) {
                        continue;
                    }
                    Instant startedAt = Instant.parse(String.valueOf(map.get("startedAt")));
                    if (startedAt.isAfter(latestAt) && startedAt.isBefore(result.startedAt())) {
                        latestAt = startedAt;
                        Object fp = map.get("datasetFingerprint");
                        latest = fp == null ? null : String.valueOf(fp);
                    }
                } catch (RuntimeException malformedRunRecord) {
                    // 跳过坏记录
                }
            }
            String current = result.datasetFingerprint();
            boolean changed = latest != null && current != null && !latest.equals(current);
            lastFingerprintChanged = changed;
            if (changed) {
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                        .counter("buzhou.eval.fingerprint.changed");
                LOGGER.log(System.Logger.Level.INFO,
                        "数据集指纹变更：dataset=" + datasetName + " 前值 " + latest
                                + " → 现值 " + current + "（diff 明细见 EvalRunDiff）");
            }
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "指纹变更检测失败（跳过）：" + String.valueOf(e.getMessage()));
        }
    }

    /**
     * spec 718：漂移判定（run 落盘后调用——基线只取早于本次的记录）。
     * 读历史失败降级跳过（观测面故障不放大）。
     */
    private void checkDrift(String datasetName, EvalRunResult result) {
        int window = driftWindow;
        if (window <= 0) {
            return;
        }
        try {
            record Sample(Instant startedAt, double passRate) {
            }
            List<Sample> history = new ArrayList<>();
            stateStore.scanByPrefix(EvalDatasetStore.SESSION_ID, RUN_PREFIX)
                    .values().forEach(entry -> {
                        try {
                            Map<String, Object> map = decodeMap(entry.value());
                            if (!datasetName.equals(map.get("datasetName"))) {
                                return;
                            }
                            Instant startedAt = Instant.parse(String.valueOf(map.get("startedAt")));
                            if (!startedAt.isBefore(result.startedAt())) {
                                return; // 只取早于本次的 run——基线防自污染
                            }
                            history.add(new Sample(startedAt,
                                    ((Number) map.getOrDefault("passRate", 0)).doubleValue()));
                        } catch (RuntimeException malformedRunRecord) {
                            // 单条坏记录跳过——漂移面不因存量脏数据失效
                        }
                    });
            history.sort(java.util.Comparator.comparing(Sample::startedAt).reversed());
            List<Sample> windowed = history.stream().limit(window).toList();
            if (windowed.isEmpty()) {
                return; // 首跑无基线
            }
            double baseline = windowed.stream().mapToDouble(Sample::passRate).average().orElse(0);
            double delta = result.passRate() - baseline;
            lastDriftDelta = delta;
            if (Math.abs(delta) >= driftWarnShift) {
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                        .counter("buzhou.eval.drift.alerts");
                LOGGER.log(System.Logger.Level.WARNING,
                        "评估通过率漂移：dataset=" + datasetName + " runId=" + result.runId()
                                + " 当前 " + result.passRate() + " vs 基线 " + baseline
                                + "（差 " + delta + " ≥ 告警线 " + driftWarnShift + "）");
            }
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "漂移基线读取失败（跳过判定）：" + String.valueOf(e.getMessage()));
        }
    }

    /** 装载 run 前期望门禁（失败 fail-fast 挂 EVAL_OPERATION_INVALID——脏数据零 token 成本出局）。 */
    public void setExpectations(DatasetExpectations expectations) {
        this.expectations = expectations;
        this.expectationsWarnOnly = false;
    }

    /** 宽松档（spec 198 §A / T561：未过只 WARN 不拦——灰度期「看到脏但照跑」，
     * 门禁日志仍带 summary + 前三条发现）。 */
    public void setExpectations(DatasetExpectations expectations, boolean warnOnly) {
        this.expectations = expectations;
        this.expectationsWarnOnly = warnOnly;
    }

    /** 执行一次评估 run（dataset 未建 fail-fast 挂 EVAL_OPERATION_INVALID）。 */
    public EvalRunResult run(String datasetName, Evaluator evaluator) {
        return run(datasetName, evaluator, 1); // spec 68：默认串行零变化
    }

    /**
     * 带并行度的评估 run（spec 68 §A / T287，LangSmith/DeepEval 并行评估借鉴）：
     * 虚拟线程池并行执行项（每项仍独占隔离 eval 会话）；<b>结果按数据集项序聚合</b>
     * （与串行同序——确定性不因并行漂移）；并行度 clamp 1..32（防失控）；汇总/落盘/
     * 事件与串行同口径（全部项完成后一次进行）。项内异常经既有三态收敛（不炸整跑）。
     */
    public EvalRunResult run(String datasetName, Evaluator evaluator, int parallelism) {
        List<EvalItem> items = datasetStore.dataset(datasetName)
                .map(meta -> datasetStore.items(datasetName))
                .orElseThrow(() -> new BuzhouException(ErrorCode.EVAL_OPERATION_INVALID,
                        "数据集未建：" + datasetName + "（修法：先 createDataset 再 run）"));
        if (expectations != null) {
            DatasetExpectations.Result gate = expectations.validate(items);
            if (!gate.passed()) {
                StringBuilder detail = new StringBuilder(gate.summary());
                gate.findings().stream().limit(3)
                        .forEach(f -> detail.append("\n  - ").append(f.expectation())
                                .append(" #").append(f.itemIndex()).append(" ").append(f.detail()));
                if (expectationsWarnOnly) {
                    // spec 198 / T561：宽松档——WARN 带 full detail 但不拦（灰度期照跑）
                    System.getLogger(EvalRunner.class.getName()).log(
                            System.Logger.Level.WARNING,
                            "数据集期望未过（warn-only，照跑。dataset={0}）：{1}",
                            datasetName, detail);
                } else {
                    throw new BuzhouException(ErrorCode.EVAL_OPERATION_INVALID,
                            "数据集期望门禁未过（dataset=" + datasetName + "）：" + detail);
                }
            }
        }
        String runId = "r" + System.currentTimeMillis() + "-"
                + String.format("%04x", ThreadLocalRandom.current().nextInt(0x10000));
        try (var registration = EvalRunRegistry.global().begin(EvalRunRegistry.KIND_EVAL, runId)) {
        Instant startedAt = Instant.now();
        int workers = Math.max(1, Math.min(32, parallelism)); // clamp 1..32
        java.util.concurrent.atomic.AtomicLong spent =
                new java.util.concurrent.atomic.AtomicLong(); // spec 520：预算累计（字符估算）
        List<EvalRunItemResult> results;
        if (workers == 1 || items.size() <= 1) {
            results = new ArrayList<>();
            for (EvalItem item : items) {
                results.add(budgetedItem(spent, item,
                        () -> memoizedItem(runId, datasetName, item, evaluator)));
            }
        } else {
            EvalRunItemResult[] byIndex = new EvalRunItemResult[items.size()];
            List<java.util.concurrent.Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                final EvalItem item = items.get(i);
                tasks.add(() -> {
                    byIndex[index] = budgetedItem(spent, item,
                            () -> memoizedItem(runId, datasetName, item, evaluator));
                    return null;
                });
            }
            try (java.util.concurrent.ExecutorService pool =
                    java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                pool.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BuzhouException(ErrorCode.EVAL_OPERATION_INVALID,
                        "评估并行执行被中断（dataset=" + datasetName + "）");
            }
            results = List.of(byIndex); // 按项序聚合（与串行同序）
        }
        Instant finishedAt = Instant.now();
        int passed = (int) results.stream().filter(r -> EvalRunItemResult.STATUS_PASS.equals(r.status())).count();
        int failed = (int) results.stream().filter(r -> EvalRunItemResult.STATUS_FAIL.equals(r.status())).count();
        int errored = (int) results.stream().filter(r -> EvalRunItemResult.STATUS_ERROR.equals(r.status())).count();
        // spec 82 §A / T319：run 执行时刻的数据集指纹入档（diff 据此显形就地改项型漂移）
        EvalRunResult result = new EvalRunResult(runId, datasetName, startedAt, finishedAt,
                results.size(), passed, failed, errored, results,
                datasetStore.fingerprint(datasetName).orElse(null));
        stateStore.put(EvalDatasetStore.SESSION_ID,
                new StateEntry(RUN_PREFIX + runId, encode(resultToMap(result)),
                        "eval", 0, null, finishedAt));
        // spec 718 / T1036：通过率漂移判定（落盘后——基线只取早于本次的记录）
        checkDrift(datasetName, result);
        // spec 734 / T1068：数据集指纹变更信号
        checkFingerprintChange(datasetName, result);
        // spec 111 §A / T403：run 总时长 timer（per-item durationMs 之外的整跑视角）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .timer("buzhou.eval.run.duration",
                        java.time.Duration.between(startedAt, finishedAt));
        emitRunCompleted(result, startedAt, finishedAt);
        return result;
        }
    }

    /**
     * spec 535 / T823：error 项重试一次（抖动缓解）——首跑 STATUS_ERROR 时
     * 重跑一次（语义 fail 不重试——重试会掩盖真实回归），取第二次结果、
     * detail 加 [RETRIED] 前缀留痕。默认关（errorRetryOnce=false 直通）。
     */
    private EvalRunItemResult runItemWithRetry(String runId, EvalItem item, Evaluator evaluator) {
        EvalRunItemResult first = runItemWithTimeout(runId, item, evaluator);
        if (!errorRetryOnce || !EvalRunItemResult.STATUS_ERROR.equals(first.status())) {
            return first;
        }
        EvalRunItemResult second = runItemWithTimeout(runId, item, evaluator);
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.eval.error-retried");
        return new EvalRunItemResult(item.id(), second.status(),
                "[RETRIED] " + second.detail(), second.actualPreview(), second.durationMs());
    }

    /**
     * spec 708 / T1016：项级记忆化包装——命中即复用上轮三态结果（跳过模型
     * 调用与 judge），miss/失配照常执行后回写；ERROR 不缓存；读写失败降级
     * 直跑（WARN 一次——优化不是依赖）。包装在 retry 外层：命中零重试语义。
     */
    private EvalRunItemResult memoizedItem(String runId, String datasetName,
            EvalItem item, Evaluator evaluator) {
        String key = memoizationKey;
        if (key == null) {
            return runItemWithRetry(runId, item, evaluator);
        }
        String memoKey = MEMO_PREFIX + datasetName + "." + item.id();
        String sig = memoSig(datasetName, item, key);
        try {
            var existing = stateStore.get(EvalDatasetStore.SESSION_ID, memoKey);
            if (existing.isPresent()) {
                Map<String, Object> memo = decodeMap(existing.get().value());
                if (sig.equals(memo.get("sig"))) {
                    io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                            .counter("buzhou.eval.memo.hits");
                    return new EvalRunItemResult(item.id(), (String) memo.get("status"),
                            "[MEMO] " + memo.get("detail"),
                            (String) memo.get("actual"),
                            memo.get("durationMs") == null ? 0L
                                    : ((Number) memo.get("durationMs")).longValue());
                }
            }
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "memo 读取失败（降级直跑）：" + String.valueOf(e.getMessage()));
        }
        EvalRunItemResult result = runItemWithRetry(runId, item, evaluator);
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.eval.memo.misses");
        if (!EvalRunItemResult.STATUS_ERROR.equals(result.status())) {
            try {
                Map<String, Object> memo = new LinkedHashMap<>();
                memo.put("sig", sig);
                memo.put("status", result.status());
                memo.put("detail", result.detail());
                memo.put("actual", result.actualPreview());
                memo.put("durationMs", result.durationMs());
                stateStore.put(EvalDatasetStore.SESSION_ID,
                        new StateEntry(memoKey, encode(memo), "eval", 0, null, Instant.now()));
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "memo 回写失败（忽略——优化不是依赖）：" + String.valueOf(e.getMessage()));
            }
        }
        return result;
    }

    /** 记忆化签名（sha256 hex——数据/判定身份任一变化即失配）。 */
    private static String memoSig(String datasetName, EvalItem item, String key) {
        String material = datasetName + "|" + item.id() + "|"
                + (item.input() == null ? "" : item.input()) + "|"
                + (item.expected() == null ? "" : item.expected()) + "|" + key;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * spec 520 / T791：预算记账包装——估算（input+expected 字符）累计超上限
     * → 该项不执行、error 三态 [RUN-BUDGET]（errored 计数；partial 显式）。
     * 预算关（0）= 直通零开销。共享 spent 为软上限（并行竞态容忍——早停语义）。
     */
    private EvalRunItemResult budgetedItem(java.util.concurrent.atomic.AtomicLong spent,
            EvalItem item, java.util.function.Supplier<EvalRunItemResult> execute) {
        if (runBudgetChars <= 0) {
            return execute.get();
        }
        long estimate = (item.input() == null ? 0 : item.input().length())
                + (item.expected() == null ? 0 : item.expected().length());
        if (spent.addAndGet(estimate) > runBudgetChars) {
            return new EvalRunItemResult(item.id(), EvalRunItemResult.STATUS_ERROR,
                    "[RUN-BUDGET] run 预算耗尽（估算上限 " + runBudgetChars
                            + " 字符）——本项未执行", "", 0);
        }
        return execute.get();
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

    /**
     * spec 609 / T868：带项级超时的执行包装——超时中断挂死项（shutdownNow 传播中断，
     * 与模型超时兜底同取舍：可中断阻塞即刻中止）并收敛为 error；未设预算直通。
     */
    private EvalRunItemResult runItemWithTimeout(String runId, EvalItem item, Evaluator evaluator) {
        java.time.Duration timeout = perItemTimeout;
        if (timeout == null) {
            return runItem(runId, item, evaluator);
        }
        long start = System.nanoTime();
        java.util.concurrent.ExecutorService one =
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            return one.submit(() -> runItem(runId, item, evaluator))
                    .get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.eval.item.timeouts");
            return new EvalRunItemResult(item.id(), EvalRunItemResult.STATUS_ERROR,
                    "项超时（预算 " + timeout + "）：单项未在预算内完成，已中断——挂死项不断批"
                            + "（pytest-timeout 语义）",
                    null, (System.nanoTime() - start) / 1_000_000);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return new EvalRunItemResult(item.id(), EvalRunItemResult.STATUS_ERROR,
                    "执行异常：" + cause.getClass().getSimpleName() + ": "
                            + String.valueOf(cause.getMessage()).lines().findFirst().orElse(""),
                    null, (System.nanoTime() - start) / 1_000_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new EvalRunItemResult(item.id(), EvalRunItemResult.STATUS_ERROR,
                    "执行被中断（dataset 评估整跑中断路径）", null,
                    (System.nanoTime() - start) / 1_000_000);
        } finally {
            one.shutdownNow(); // 超时/完成都中断残留线程（挂死项的会话随中断关闭）
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
        EvalScore score;
        try {
            score = evaluator.evaluate(actual, item.expected(), item);
        } catch (Exception e) {
            // spec 61 §A / T273：评估器异常收敛为该条 error（judge 调用抖动等不炸整跑；
            // 与执行异常同三态语义）
            return new EvalRunItemResult(item.id(), EvalRunItemResult.STATUS_ERROR,
                    "评估器异常：" + e.getClass().getSimpleName() + ": "
                            + String.valueOf(e.getMessage()).lines().findFirst().orElse(""),
                    preview(actual), ms);
        }
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
        if (r.datasetFingerprint() != null) {
            map.put("datasetFingerprint", r.datasetFingerprint());
        }
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
                items,
                (String) map.get("datasetFingerprint"));
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
