package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 死循环与失控检测 Hook（spec「死循环与失控检测」）。
 *
 * <p>挂在既有 {@link BuzhouHook} 切面，对单轮推理的「行为失控」（步数 / 调用次数 / 时长 / 重复）
 * 提供数值闸门：达软阈值注入剩余预算提醒（由 {@link RunawayBudgetRenderer} 渲染），达硬顶
 * {@link HookResult#block(String)} 终止并携带部分结果。失控事件经既有 {@code emitEvent} 通道发出。
 *
 * <p><b>双窗口</b>：① 轮次级（内存计数，{@code beforeTurn} 重置）；② 会话级累计（持久化在
 * SessionStateStore，跨崩溃保留，参照 {@code recovery.autoresume.attempts} 先例）。
 *
 * <p><b>safe-by-default</b>：阈值默认 null = 不限，显式配置才生效。不配置任何阈值时等价现状。
 * 对齐 07 背压：不设可能误伤生产的魔法默认值。
 *
 * <p><b>切面次序诚实标注</b>：注入视图在 memory advisor(+400) 构建、步数在本 Hook(默认 order 1000)
 * {@code beforeModel} 递增——renderer 本步注入用「上一步末」计数（一步滞后，可接受）。
 */
public class RunawayHook implements BuzhouHook {

    private static final System.Logger LOGGER = System.getLogger(RunawayHook.class.getName());

    // ---- 事件类型（经既有 emitEvent 通道，与 backpressure.*/guard.*/drain.* 同命名约定）----

    /** 软阈值触发，软退出提醒已注入（payload: counter/limit/remaining）。 */
    public static final String EVENT_SOFT_THRESHOLD = "runaway.soft-threshold";

    /** 硬顶终止，携带部分结果（payload: reason/limit/value）。 */
    public static final String EVENT_HARD_STOP = "runaway.hard-stop";

    /** 按工具限额触发（payload: toolName/limit/value）。 */
    public static final String EVENT_PER_TOOL_EXCEEDED = "runaway.per-tool-exceeded";

    /** 重复检测触发（payload: toolName/fingerprint/count）。 */
    public static final String EVENT_REPETITION = "runaway.repetition";

    // ---- 会话级累计计数 SessionStateStore 键（参照 recovery.autoresume.attempts 先例，跨崩溃保留）----
    static final String KEY_SESSION_STEPS = "runaway.session.steps";
    static final String KEY_SESSION_TOOL_CALLS = "runaway.session.tool-calls";

    // ---- 硬顶原因维度（事件 payload reason 字段）----

    public static final String REASON_STEPS = "steps";
    public static final String REASON_TOOL_CALLS = "tool-calls";
    public static final String REASON_WALL_CLOCK = "wall-clock";
    public static final String REASON_SESSION_STEPS = "session-steps";
    public static final String REASON_SESSION_TOOL_CALLS = "session-tool-calls";
    public static final String REASON_REPETITION = "repetition";

    private final BuzhouRunawayProperties props;
    private final RunawayCounters counters;
    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore observabilityStore;

    public RunawayHook(BuzhouRunawayProperties props, RunawayCounters counters) {
        this(props, counters, null);
    }

    /**
     * @param observabilityStore 可选 ObservabilityStore；非空时失控事件双重写入（SessionEvent + EventRecord），
     *                           使 {@code runaway.*} 全族在 dashboard（{@code eventsOfSession}）可查。
     *                           落库路径定案：手动双重写入（参照 {@code GuardAuthApi.emitAudit} 先例），
     *                           因 Hook 上下文不暴露 SpanContext/SpanRecorder，不走 SpanRecorder.emit 直发。
     */
    public RunawayHook(BuzhouRunawayProperties props, RunawayCounters counters,
                       io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore observabilityStore) {
        this.props = props;
        this.counters = counters;
        this.observabilityStore = observabilityStore;
    }

    @Override
    public String name() {
        return "RunawayHook";
    }

    /** {@code beforeTurn}：重置轮次级内存计数并记录 wall-clock 起点。 */
    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (!props.enabled()) {
            return HookResult.CONTINUE;
        }
        counters.resetTurn(ctx.sessionId());
        return HookResult.CONTINUE;
    }

    /**
     * {@code beforeModel}：递增步数，校验轮次级步数硬顶 + wall-clock（步边界）+ 会话级累计。
     *
     * <p><b>诚实边界</b>（wall-clock）：轮次时长上界 = {@code wallClock + 单步时长}（一次模型调用延迟
     * + 一次工具超时），非中途精确打断；wall-clock 在步边界检测，与 10 韧性单步 {@code deadline} 正交共存。
     */
    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        if (!props.enabled()) {
            return HookResult.CONTINUE;
        }
        RunawayCounters.TurnState ts = counters.turnState(ctx.sessionId());
        int step = ts.steps.incrementAndGet();

        // wall-clock 步边界检查（诚实边界：超时在下一个 beforeModel 发现，非中途打断）
        java.time.Duration wallClock = perTurnWallClock();
        if (wallClock != null && ts.turnStart != null) {
            java.time.Duration elapsed = java.time.Duration.between(ts.turnStart, java.time.Instant.now());
            if (elapsed.compareTo(wallClock) > 0) {
                return wallClockHardStop(ctx, wallClock, elapsed);
            }
        }

        // 轮次级步数硬顶：每次模型调用 = 一步；超限在 nextCall 之前 Block
        Integer maxSteps = perTurnMaxSteps();
        if (maxSteps != null && step > maxSteps) {
            return hardStopBlock(ctx, REASON_STEPS, maxSteps, step);
        }
        // 会话级累计步数硬顶（持久化在 SessionStateStore，跨崩溃保留；AUTO_RESUME 不重置预算）
        Integer sessionMaxSteps = perSessionMaxSteps();
        if (sessionMaxSteps != null) {
            int sessionSteps = incrementSessionCounter(ctx, KEY_SESSION_STEPS);
            if (sessionSteps > sessionMaxSteps) {
                return hardStopBlock(ctx, REASON_SESSION_STEPS, sessionMaxSteps, sessionSteps);
            }
        }
        // 软阈值检测（issue 02）：剩余预算占比跌破阈值时发 soft-threshold 事件（每轮仅首次），
        // 软退出提醒文本由 RunawayBudgetRenderer 经 Attachment 通道注入（每步刷新）
        if (maxSteps != null) {
            maybeEmitSoftThreshold(ctx, ts, step, maxSteps);
        }
        return HookResult.CONTINUE;
    }

    /** wall-clock 硬顶：limit/value 用毫秒数（reason=wall-clock 区分维度）。 */
    private HookResult wallClockHardStop(ModelCallContext ctx, java.time.Duration limit, java.time.Duration elapsed) {
        emit(ctx, EVENT_HARD_STOP, java.util.Map.ofEntries(
                java.util.Map.entry("sessionId", ctx.sessionId()),
                java.util.Map.entry("turn", ctx.turn()),
                java.util.Map.entry("reason", REASON_WALL_CLOCK),
                java.util.Map.entry("limit", limit.toMillis()),
                java.util.Map.entry("value", elapsed.toMillis()),
                java.util.Map.entry("partialResultRef", "messageStore:" + ctx.sessionId())
        ));
        return HookResult.block("已达到单轮时长（wall-clock）上限（" + limit.toMillis()
                + "ms，已耗时 " + elapsed.toMillis() + "ms），强制终止。"
                + "本轮已完成的工具调用结果已随轮次保留，可基于部分结果继续。");
    }

    /** 软阈值触发时发一次事件（同一轮不重复发，避免事件刷屏；注入本身每步刷新由 renderer 负责）。 */
    private void maybeEmitSoftThreshold(ModelCallContext ctx, RunawayCounters.TurnState ts,
                                        int step, int maxSteps) {
        if (ts.softThresholdEmitted) {
            return;
        }
        int remaining = maxSteps - step;
        if (remaining <= 0) {
            return;
        }
        double ratio = (double) remaining / maxSteps;
        if (ratio < props.effectiveSoftThresholdRatio()) {
            ts.softThresholdEmitted = true;
            emit(ctx, EVENT_SOFT_THRESHOLD, Map.of(
                    "sessionId", ctx.sessionId(),
                    "turn", ctx.turn(),
                    "counter", step,
                    "limit", maxSteps,
                    "remaining", remaining
            ));
        }
    }

    /** {@code beforeTool}：递增工具调用数，校验轮次级工具调用硬顶 + 按工具单独限额。 */
    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (!props.enabled()) {
            return HookResult.CONTINUE;
        }
        String sid = ctx.sessionId();
        RunawayCounters.TurnState ts = counters.turnState(sid);

        // 轮次级工具调用硬顶：每次工具调用 = 一次；超限在工具执行之前 Block
        // （reason 回注为工具结果文本，对齐 beforeTool Block 语义）
        Integer maxToolCalls = perTurnMaxToolCalls();
        int toolCall = ts.toolCalls.incrementAndGet();
        if (maxToolCalls != null && toolCall > maxToolCalls) {
            return hardStopBlock(ctx, REASON_TOOL_CALLS, maxToolCalls, toolCall);
        }

        // 按工具单独限额（通配匹配工具名，exact 优先 → 最长前缀 *）
        Integer perToolMax = perToolLimit(ctx.toolName());
        if (perToolMax != null) {
            int perToolCount = ts.perTool
                    .computeIfAbsent(ctx.toolName(), k -> new java.util.concurrent.atomic.AtomicInteger(0))
                    .incrementAndGet();
            if (perToolCount > perToolMax) {
                emit(ctx, EVENT_PER_TOOL_EXCEEDED, Map.of(
                        "sessionId", sid,
                        "turn", ctx.turn(),
                        "toolName", ctx.toolName(),
                        "limit", perToolMax,
                        "value", perToolCount
                ));
                return HookResult.block(formatPerToolReason(ctx.toolName(), perToolMax, perToolCount));
            }
        }
        // 确定性重复检测（M2）：连续 N 次同工具同参数调用 → 失控（不做语义相似度，避免误杀合法分页/轮询）
        Integer consecutive = repetitionConsecutive();
        if (consecutive != null && consecutive >= 2) {
            HookResult repetition = checkRepetition(ctx, ts, consecutive);
            if (repetition != HookResult.CONTINUE) {
                return repetition;
            }
        }
        // 会话级累计工具调用数硬顶（持久化在 SessionStateStore，跨崩溃保留）
        Integer sessionMaxToolCalls = perSessionMaxToolCalls();
        if (sessionMaxToolCalls != null) {
            int sessionToolCalls = incrementSessionCounter(ctx, KEY_SESSION_TOOL_CALLS);
            if (sessionToolCalls > sessionMaxToolCalls) {
                return hardStopBlock(ctx, REASON_SESSION_TOOL_CALLS, sessionMaxToolCalls, sessionToolCalls);
            }
        }
        return HookResult.CONTINUE;
    }

    /**
     * 确定性重复检测：指纹 = 规范化 {@code (toolName, canonicalJson(arguments))}，环缓冲保留最近
     * {@code consecutive} 个指纹；全部相同即判定连续重复。不做语义相似度——合法的分页翻读 / 轮询 /
     * 批量处理（参数变化）天然不误伤（确定性同参数规则只命中真重复）。
     *
     * <p>环缓冲非线程安全，工具扇出可并行——同步访问避免损坏（并行同参数调用被判重复也合理）。
     * {@code action=flag-only} 时仅发事件不阻断（默认 block）。
     */
    private HookResult checkRepetition(ToolCallContext ctx, RunawayCounters.TurnState ts, int consecutive) {
        String fingerprint = fingerprint(ctx.toolName(), ctx.arguments());
        synchronized (ts.fingerprintRing) {
            ts.fingerprintRing.addLast(fingerprint);
            while (ts.fingerprintRing.size() > consecutive) {
                ts.fingerprintRing.removeFirst();
            }
            if (ts.fingerprintRing.size() < consecutive || !allSame(ts.fingerprintRing)) {
                return HookResult.CONTINUE;
            }
        }
        // 连续 N 次同指纹 → 重复检测命中
        emit(ctx, EVENT_REPETITION, Map.of(
                "sessionId", ctx.sessionId(),
                "turn", ctx.turn(),
                "toolName", ctx.toolName(),
                "fingerprint", fingerprint,
                "count", consecutive
        ));
        if (repetitionActionBlock()) {
            return HookResult.block(formatRepetitionReason(ctx.toolName(), consecutive));
        }
        return HookResult.CONTINUE;
    }

    /** 指纹：规范化 {@code (toolName, arguments)} 为稳定字符串（key 排序，确保参数同序同指纹）。 */
    static String fingerprint(String toolName, java.util.Map<String, Object> arguments) {
        StringBuilder sb = new StringBuilder(toolName).append('(');
        new java.util.TreeMap<>(arguments).forEach((k, v) -> sb.append(k).append('=').append(v).append(','));
        return sb.append(')').toString();
    }

    private static boolean allSame(java.util.Deque<String> ring) {
        String first = ring.peekFirst();
        for (String s : ring) {
            if (!s.equals(first)) {
                return false;
            }
        }
        return true;
    }

    /** 重复检测触达文本（告知模型连续重复调用同一工具同一参数）。 */
    static String formatRepetitionReason(String toolName, int consecutive) {
        return "检测到连续 " + consecutive + " 次以相同参数调用工具 " + toolName
                + "，疑似死循环，本次调用未执行。请更换参数或停止重复调用。";
    }

    /**
     * 会话级累计计数读-改-写（持久化在 SessionStateStore）。
     *
     * <p>SessionStateHandle.put 以 {@code String.valueOf} 存储，{@code get(key, Integer.class)}
     * 因类型过滤恒空——故读 String 再 parse（与 {@code recovery.autoresume.attempts} 先例同口径）。
     * 计数随会话删除而清除（store 生命周期）；AUTO_RESUME 重驱动时计数不重置（避免崩溃-恢复循环重烧预算）。
     */
    private int incrementSessionCounter(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookContext ctx, String key) {
        // 同步：工具扇出可并行调用 beforeTool，会话级计数 RMW 需按会话加锁防竞争（undercount）
        synchronized (counters.sessionLock(ctx.sessionId())) {
            int current = ctx.state().get(key, String.class).map(Integer::valueOf).orElse(0);
            int next = current + 1;
            ctx.state().put(key, next);
            return next;
        }
    }

    // ---- 硬顶终止：发事件 + Block(reason)，reason 为受控终态文本（可解释终止）----

    /**
     * 硬顶 Block：发 {@code runaway.hard-stop} 事件（携带 {@code partialResultRef} 部分结果指针）+
     * Block(reason)。beforeModel 的 Block reason 经 HookAdvisor 成为本轮最终回复；
     * beforeTool 的 Block reason 回注为工具结果文本。
     */
    private HookResult hardStopBlock(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookContext ctx,
                                     String reason, int limit, int value) {
        emit(ctx, EVENT_HARD_STOP, Map.of(
                "sessionId", ctx.sessionId(),
                "turn", ctx.turn(),
                "reason", reason,
                "limit", limit,
                "value", value,
                "partialResultRef", "messageStore:" + ctx.sessionId()
        ));
        // impl-45：指标（reason tag 有界六枚举）+ WARN 日志（失控终止运维可见）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.runaway.hard-stops", "reason", reason);
        LOGGER.log(System.Logger.Level.WARNING,
                "失控检测硬顶终止：sessionId=" + ctx.sessionId() + ", reason=" + reason
                        + ", limit=" + limit + ", value=" + value);
        return HookResult.block(formatHardStopReason(reason, limit, value));
    }

    /** 受控终态文本：触发维度 + 上限 + 当前值 + 部分结果保留声明（被终止 ≠ 前功尽弃）。 */
    static String formatHardStopReason(String reason, int limit, int value) {
        return "已达到" + reasonLabel(reason) + "上限（" + limit + "，当前 " + value
                + "），强制终止。本轮已完成的工具调用结果已随轮次保留，可基于部分结果继续。";
    }

    /** 按工具限额触达文本（回注为工具结果，告知模型该工具本轮已达上限）。 */
    static String formatPerToolReason(String toolName, int limit, int value) {
        return "工具 " + toolName + " 已达单轮调用上限（" + limit + "，当前 " + value
                + "），本次调用未执行。请改用其他方式或停止重复调用。";
    }

    private static String reasonLabel(String reason) {
        return switch (reason) {
            case REASON_STEPS -> "单轮步数";
            case REASON_TOOL_CALLS -> "单轮工具调用次数";
            case REASON_WALL_CLOCK -> "单轮时长（wall-clock）";
            case REASON_SESSION_STEPS -> "会话累计步数";
            case REASON_SESSION_TOOL_CALLS -> "会话累计工具调用次数";
            case REASON_REPETITION -> "连续重复调用";
            default -> reason;
        };
    }

    /**
     * 双重写入：既经 {@code ctx.emitEvent} 发 SessionEvent（SessionEventListener / hook onEvent 消费），
     * 又写入 ObservabilityStore 的 EventRecord（dashboard / {@code eventsOfSession} 可查）。
     * 参照 {@code GuardAuthApi.emitAudit} 先例；{@code spanId=null}（SessionEvent 不携带 span 身份）。
     * 落库失败不阻断主链路（观测降级，而非业务降级）。
     */
    private void emit(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookContext ctx,
                      String type, Map<String, Object> payload) {
        SessionEvent event = new SessionEvent(type, payload, Instant.now());
        ctx.emitEvent(event);
        if (observabilityStore != null) {
            try {
                observabilityStore.saveEvents(List.of(new io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord(
                        java.util.UUID.randomUUID().toString(), null, ctx.sessionId(),
                        type, event.occurredAt(), payload)));
            } catch (RuntimeException ignored) {
                // 观测落库失败不阻断主链路
            }
        }
    }

    // ---- 配置派生辅助 ----

    private Integer perTurnMaxSteps() {
        return props.perTurn() != null ? props.perTurn().maxSteps() : null;
    }

    private Integer perTurnMaxToolCalls() {
        return props.perTurn() != null ? props.perTurn().maxToolCalls() : null;
    }

    private java.time.Duration perTurnWallClock() {
        return props.perTurn() != null ? props.perTurn().wallClock() : null;
    }

    private Integer perSessionMaxSteps() {
        return props.perSession() != null ? props.perSession().maxSteps() : null;
    }

    private Integer perSessionMaxToolCalls() {
        return props.perSession() != null ? props.perSession().maxToolCalls() : null;
    }

    /** 重复检测连续次数阈值（null 或 <2 = 关闭）。 */
    private Integer repetitionConsecutive() {
        return props.repetition() != null ? props.repetition().consecutive() : null;
    }

    /** 重复检测处置：{@code block}（默认）阻断调用，{@code flag-only} 仅告警不阻断。 */
    private boolean repetitionActionBlock() {
        if (props.repetition() == null || props.repetition().action() == null) {
            return true;
        }
        return !"flag-only".equalsIgnoreCase(props.repetition().action().trim());
    }

    /**
     * 按工具限额生效值（exact 优先 → 最长前缀 {@code *} 通配兜底，与既有 {@code ToolPolicyMatcher} 口径一致）。
     * 返回 null = 该工具不限额。
     */
    private Integer perToolLimit(String toolName) {
        java.util.Map<String, BuzhouRunawayProperties.PerToolLimit> perTool = props.perTool();
        if (perTool == null || perTool.isEmpty()) {
            return null;
        }
        // exact 优先
        BuzhouRunawayProperties.PerToolLimit exact = perTool.get(toolName);
        if (exact != null && exact.maxCalls() != null) {
            return exact.maxCalls();
        }
        // 最长前缀通配
        String bestPattern = null;
        int bestPrefixLen = -1;
        for (var entry : perTool.entrySet()) {
            String pattern = entry.getKey();
            if (!pattern.contains("*")) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue().maxCalls() == null) {
                continue;
            }
            if (!globMatches(pattern, toolName)) {
                continue;
            }
            int prefixLen = pattern.indexOf('*');
            if (prefixLen > bestPrefixLen) {
                bestPrefixLen = prefixLen;
                bestPattern = pattern;
            }
        }
        return bestPattern != null ? perTool.get(bestPattern).maxCalls() : null;
    }

    /**
     * glob 通配匹配（复刻 {@code ToolPolicyMatcher.globMatches} 同口径算法：支持 {@code *} 在任意位置）。
     * {@code ToolPolicyMatcher.globMatches} 为包级私有（{@code core.policy}），跨包不可直接复用；
     * 此处保持算法完全一致，避免行为分叉。
     */
    static boolean globMatches(String pattern, String name) {
        String[] parts = pattern.split("\\*", -1);
        int index = 0;
        if (!parts[0].isEmpty()) {
            if (!name.startsWith(parts[0])) {
                return false;
            }
            index = parts[0].length();
        }
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            int found = name.indexOf(part, index);
            if (found < 0) {
                return false;
            }
            index = found + part.length();
        }
        if (!parts[parts.length - 1].isEmpty()) {
            return name.endsWith(parts[parts.length - 1]);
        }
        return true;
    }
}
