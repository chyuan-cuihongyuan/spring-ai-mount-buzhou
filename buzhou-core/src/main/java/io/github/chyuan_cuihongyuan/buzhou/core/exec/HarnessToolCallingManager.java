package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class HarnessToolCallingManager implements ToolCallingManager {

    /** ToolContext 中携带当前会话 id 的键（供内置工具做会话级解析，如 load_skill 绑定校验）。 */
    public static final String SESSION_ID_KEY = "buzhou.sessionId";

    /** 从 ToolContext 取当前会话 id（无则 null；内置工具的会话级解析统一经此读取）。 */
    public static String sessionIdOf(org.springframework.ai.chat.model.ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object value = toolContext.getContext().get(SESSION_ID_KEY);
        return value instanceof String s ? s : null;
    }

    private final DefaultToolCallingManager delegate;
    private final ExecutorService executor;
    private final Semaphore turnPermits;
    private final Duration toolTimeout;
    private final Map<String, String> serialGroups;
    private final SpanContextCarrier spanContextCarrier;
    private final String sessionId;
    /**
     * impl-28 / spec 13 §core-2：串行组互斥从 {@code synchronized} 改为 {@link ReentrantLock}——
     * 可限时获取（tryLock(剩余)）也可响应中断（lockInterruptibly），挂起的持锁同伴不再连带
     * 后续等待者永久阻塞。键为组名；无组工具每次新建无竞争锁（与既有 {@code new Object()} 等价）。
     */
    private final ConcurrentHashMap<String, ReentrantLock> groupLocks = new ConcurrentHashMap<>();
    private final List<Future<?>> inFlight = new CopyOnWriteArrayList<>();
    /** impl-28：本 Turn 硬 Deadline（none 哨兵 = 不设界，保持既有无限等待行为）。 */
    private volatile io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline turnDeadline =
            io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline.none();
    /** impl-04 / T30：入参 schema 校验开关（默认开）。 */
    private volatile boolean argsValidation = true;
    /** impl-04 / T30：本 Turn 累计校验反馈次数（BoundedToolCallingAdvisor 在 Turn 开始时复位）。 */
    private final java.util.concurrent.atomic.AtomicInteger validationFailures =
            new java.util.concurrent.atomic.AtomicInteger();
    /** impl-05 / T31：待生效的取消请求（BoundedToolCallingAdvisor 在 Turn 开始时清零）。 */
    private final java.util.concurrent.atomic.AtomicReference<
            io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode> pendingCancel =
            new java.util.concurrent.atomic.AtomicReference<>();
    /** impl-07 / T33：事件溯源工具调用日志（可选；RecoverySupport 经装配上下文注入）。 */
    private volatile io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog toolCallLog;
    /** impl-10 / T35：并行批回喂策略（默认 ALL；FAILED_ONLY 见枚举语义）。 */
    private volatile BatchFeedbackPolicy batchFeedbackPolicy = BatchFeedbackPolicy.ALL;
    /** spec 122 / impl-271：superstep 原子批开关（默认关=per-tool 既有行为零变化）。 */
    private volatile boolean atomicBatchValidation = false;
    /** spec 31 / T110 / impl-85：工具结果尺寸防护（Holder 默认 20K + read_range 豁免）。 */
    private volatile ToolResultLimiter resultLimiter = ToolResultLimiterHolder.current();

    /** spec 300 / impl-323：批内/在飞合并器（默认 null = 关，既有 per-tool 行为零变化；139 原语接线）。 */
    private volatile ToolCallCoalescer batchCoalescer;

    /**
     * spec 300 / impl-323：启用/停用批内合并（经 {@code SessionAssemblyContext.toolManager()}
     * 注入，与 batchFeedbackPolicy / atomicBatchValidation 同通道）。开启后批内同工具同参
     * 调用执行一次、全部位共享值（合并位回喂逐位重写 id）；{@code null} = 停用。
     */
    public void setBatchCoalescer(ToolCallCoalescer coalescer) {
        this.batchCoalescer = coalescer;
    }

    /** spec 31：per-session 覆盖限幅器（经 SessionAssemblyContext.toolManager() 注入）。 */
    public void setResultLimiter(ToolResultLimiter limiter) {
        this.resultLimiter = limiter == null ? ToolResultLimiter.disabled() : limiter;
    }

    /** impl-10 / T35：批提交回喂策略（LangGraph superstep 修正版语义的显式化）。 */
    public enum BatchFeedbackPolicy {
        /** 全部回喂（默认）：成功与失败结果都注入模型。 */
        ALL,
        /**
         * 仅失败回喂：任一失败时，成功者结果<b>暂存事件日志</b>（executeOne 已 append-only 记录、
         * 可经 ToolCallLog 回查），上下文替换为占位提示——批内失败信号更聚焦、省窗口。
         * 诚实边界：状态层原子（本批消息同轮注入）；副作用不回滚（不谎称事务回滚）。
         */
        FAILED_ONLY
    }

    /** impl-10 / T35：设置批回喂策略（经 SessionAssemblyContext.toolManager() 注入）。 */
    public void setBatchFeedbackPolicy(BatchFeedbackPolicy policy) {
        this.batchFeedbackPolicy = policy == null ? BatchFeedbackPolicy.ALL : policy;
    }

    /**
     * spec 122 / impl-271：superstep 原子批开关（经 SessionAssemblyContext.toolManager()
     * 注入）。开启后批派发前对全部调用做「工具存在 + 入参校验」前检，任一未过则整批
     * 不派发（合法同伴记 BATCH_ABORTED，零副作用）；默认关 = 既有 per-tool 行为。
     */
    public void setAtomicBatchValidation(boolean atomicBatchValidation) {
        this.atomicBatchValidation = atomicBatchValidation;
    }

    /** 当前 superstep 原子批开关状态（测试与诊断用）。 */
    public boolean atomicBatchValidation() {
        return atomicBatchValidation;
    }

    /**
     * impl-28 / spec 13 §core-2：Turn 开始设定硬 Deadline（由 {@code BoundedToolCallingAdvisor}
     * 在每次工具递归循环初始化时调用，预算 = min(turnDeadline, loopTimeout)）；
     * {@code null} 或哨兵 = 不设界（既有无限等待行为）。剩余时间在派发/组锁/许可/join
     * 各等待点统一按本对象递减，嵌套不重新计时。
     */
    public void beginTurn(io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline deadline) {
        this.turnDeadline = deadline == null
                ? io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline.none() : deadline;
    }

    /** 当前生效的 Turn Deadline（测试与诊断用；无则哨兵）。 */
    public io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline turnDeadline() {
        return turnDeadline;
    }

    public HarnessToolCallingManager(DefaultToolCallingManager delegate,
                                     ExecutorService executor,
                                     int maxConcurrencyPerTurn,
                                     Duration toolTimeout,
                                     Map<String, String> serialGroups) {
        this(delegate, executor, maxConcurrencyPerTurn, toolTimeout, serialGroups, null);
    }

    public HarnessToolCallingManager(DefaultToolCallingManager delegate,
                                     ExecutorService executor,
                                     int maxConcurrencyPerTurn,
                                     Duration toolTimeout,
                                     Map<String, String> serialGroups,
                                     SpanContextCarrier spanContextCarrier) {
        this(delegate, executor, maxConcurrencyPerTurn, toolTimeout, serialGroups,
                spanContextCarrier, null);
    }

    public HarnessToolCallingManager(DefaultToolCallingManager delegate,
                                     ExecutorService executor,
                                     int maxConcurrencyPerTurn,
                                     Duration toolTimeout,
                                     Map<String, String> serialGroups,
                                     SpanContextCarrier spanContextCarrier,
                                     String sessionId) {
        this.delegate = delegate;
        this.executor = executor;
        this.turnPermits = new Semaphore(maxConcurrencyPerTurn);
        this.toolTimeout = toolTimeout;
        this.serialGroups = serialGroups == null ? Map.of() : serialGroups;
        this.spanContextCarrier = spanContextCarrier;
        this.sessionId = sessionId;
    }

    /** impl-04 / T30：入参 schema 校验开关（默认开；关闭后回到「直接执行」旧行为）。 */
    public void setArgsValidation(boolean argsValidation) {
        this.argsValidation = argsValidation;
    }

    /** 本 Turn 累计的校验反馈次数（供停止条件裁决）。 */
    public int validationFailures() {
        return validationFailures.get();
    }

    /** Turn 开始时复位校验失败计数（BoundedToolCallingAdvisor 调用）。 */
    public void resetValidationFailures() {
        validationFailures.set(0);
    }

    /**
     * impl-05 / T31：请求取消。IMMEDIATE 立即中断在飞工具（丢弃在飞结果）；其余档位
     * 置位标记、由护栏在下一轮工具执行前裁决。Turn 开始时标记清零（空闲期取消视为 no-op）。
     */
    public void requestCancel(io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode mode) {
        io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode effective = mode == null
                ? io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode.IMMEDIATE : mode;
        pendingCancel.set(effective);
        if (effective == io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode.IMMEDIATE) {
            cancelInFlight();
        }
    }

    /** 待生效的取消请求（无则 null）。 */
    public io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode pendingCancel() {
        return pendingCancel.get();
    }

    /** Turn 开始清零取消标记（BoundedToolCallingAdvisor 调用）。 */
    public void clearPendingCancel() {
        pendingCancel.set(null);
    }

    /** impl-07 / T33：事件溯源工具调用日志（append-only 记录每次工具结局；null = 不记录）。 */
    public void setToolCallLog(io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog toolCallLog) {
        this.toolCallLog = toolCallLog;
    }

    public io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog toolCallLog() {
        return toolCallLog;
    }

    /** 结局入日志（append-only；COMPLETED 只记录一次——Temporal Activity 结果语义）。 */
    private void recordOutcome(
            AssistantMessage.ToolCall toolCall,
            io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome outcome,
            String result) {
        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog log = this.toolCallLog;
        if (log == null || sessionId == null) {
            return;
        }
        log.append(new io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry(
                sessionId, toolCall.id(), toolCall.name(),
                io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry.argsHash(
                        toolCall.arguments()),
                outcome, result, null));
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions toolCallingChatOptions) {
        return delegate.resolveToolDefinitions(toolCallingChatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
        if (toolCalls.isEmpty()) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }

        ToolCallingChatOptions options = prompt.getOptions() instanceof ToolCallingChatOptions t
                ? t : ToolCallingChatOptions.builder().build();
        Map<String, ToolCallback> callbacksByName = new java.util.HashMap<>();
        for (ToolCallback callback : options.getToolCallbacks() == null
                ? List.<ToolCallback>of() : options.getToolCallbacks()) {
            callbacksByName.put(callback.getToolDefinition().name(), callback);
        }

        Map<String, Object> toolContextMap = options.getToolContext() == null
                ? new java.util.HashMap<>() : new java.util.HashMap<>(options.getToolContext());
        if (spanContextCarrier != null) {
            toolContextMap.put(SpanContextCarrier.KEY, spanContextCarrier);
        }
        if (sessionId != null) {
            toolContextMap.put(SESSION_ID_KEY, sessionId);
        }
        // impl-05 / T31：取消令牌贯穿工具执行链（协作式取消：长任务轮询提前中止）
        toolContextMap.put(CancellationToken.KEY,
                CancellationToken.of(() -> pendingCancel.get() != null));
        ToolContext toolContext = new ToolContext(toolContextMap);
        // spec 122 / impl-271：superstep 原子批前检——任一未过则整批不派发（零锁零许可零副作用）。
        if (atomicBatchValidation) {
            List<ToolResponseMessage.ToolResponse> aborted = atomicPreflight(toolCalls, callbacksByName);
            if (aborted != null) {
                List<ToolResponseMessage.ToolResponse> limited = new ArrayList<>(aborted.size());
                boolean direct = false;
                for (ToolResponseMessage.ToolResponse response : aborted) {
                    ToolResponseMessage.ToolResponse applied = resultLimiter.apply(response);
                    limited.add(applied);
                    direct |= isReturnDirect(callbacksByName.get(applied.name()));
                }
                List<Message> abortedHistory = new ArrayList<>(prompt.getInstructions());
                abortedHistory.add(assistantMessage);
                abortedHistory.add(ToolResponseMessage.builder().responses(limited).build());
                return ToolExecutionResult.builder()
                        .conversationHistory(abortedHistory)
                        .returnDirect(direct)
                        .build();
            }
        }
        List<Future<ToolResponseMessage.ToolResponse>> futures = new ArrayList<>();
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        boolean returnDirect = false;
        ToolCallCoalescer coalescer = this.batchCoalescer;
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            if (coalescer == null) {
                futures.add(executor.submit(() -> executeOne(toolCall, callbacksByName, toolContext)));
            } else {
                // spec 300 / impl-323：键 = 工具名 + 全参串（零碰撞——合并正确性优先于键紧凑）
                String key = toolCall.name() + "#" + toolCall.arguments();
                futures.add(coalescer.submit(key,
                        () -> executeOne(toolCall, callbacksByName, toolContext), executor));
            }
        }
        for (int i = 0; i < futures.size(); i++) {
            AssistantMessage.ToolCall toolCall = toolCalls.get(i);
            ToolResponseMessage.ToolResponse response;
            try {
                response = awaitCompletion(futures.get(i), toolCall);
            } catch (Exception e) {
                // 兜底：任何漏网的执行异常也降级为错误反馈，保证每个 tool_call 恒有一个
                // ToolResponse（协议要求）且 Turn 不死，而非上抛终结整轮。
                Throwable cause = e.getCause() == null ? e : e.getCause();
                response = new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                        ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                                "执行失败：" + cause));
            }
            // spec 300 / impl-323：合并位共享值、独占 id——协议要求每个调用位有 id 一致回喂
            if (!toolCall.id().equals(response.id())) {
                response = new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolCall.name(), response.responseData());
            }
            responses.add(resultLimiter.apply(response));
            returnDirect |= isReturnDirect(callbacksByName.get(response.name()));
        }

        List<Message> conversationHistory = new ArrayList<>(prompt.getInstructions());
        conversationHistory.add(assistantMessage);
        conversationHistory.add(ToolResponseMessage.builder()
                .responses(responsesForModel(responses)).build());
        return ToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .returnDirect(returnDirect)
                .build();
    }

    /**
     * spec 122 / impl-271：superstep 原子批前检——逐项做「工具存在 + 入参 schema 校验」
     * （纯内存、零锁/零许可/零派发）。任一未过：违规者按既有 REASK / missing-tool 词汇
     * 回喂，合法同伴回喂「原子中止」并以 {@link io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome#BATCH_ABORTED}
     * 落事件日志；全过返回 {@code null} 走正常并行派发。
     */
    private List<ToolResponseMessage.ToolResponse> atomicPreflight(
            List<AssistantMessage.ToolCall> toolCalls,
            Map<String, ToolCallback> callbacksByName) {
        List<ToolResponseMessage.ToolResponse> preflight = new ArrayList<>(toolCalls.size());
        boolean anyViolation = false;
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            ToolCallback callback = callbacksByName.get(toolCall.name());
            if (callback == null) {
                preflight.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                        ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                                ToolErrorFeedback.missingToolReason(toolCall.name()))));
                anyViolation = true;
                continue;
            }
            if (argsValidation) {
                Optional<String> violation = ToolArgsValidator.validate(
                        callback.getToolDefinition().inputSchema(), toolCall.arguments());
                if (violation.isPresent()) {
                    validationFailures.incrementAndGet();
                    String feedback = ToolValidationFeedback.format(toolCall.name(),
                            toolCall.arguments(), violation.get());
                    recordOutcome(toolCall,
                            io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.VALIDATION_REJECTED,
                            feedback);
                    preflight.add(new ToolResponseMessage.ToolResponse(
                            toolCall.id(), toolCall.name(), feedback));
                    anyViolation = true;
                    continue;
                }
            }
            preflight.add(null);
        }
        if (!anyViolation) {
            return null;
        }
        for (int i = 0; i < preflight.size(); i++) {
            if (preflight.get(i) == null) {
                AssistantMessage.ToolCall toolCall = toolCalls.get(i);
                String feedback = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                        "同伴参数校验未过，本批原子中止（本调用未执行）");
                recordOutcome(toolCall,
                        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.BATCH_ABORTED,
                        feedback);
                preflight.set(i, new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolCall.name(), feedback));
            }
        }
        return preflight;
    }

    /**
     * impl-28 / spec 13 §core-2：外层 join 限时化（挂起点①）。无 Deadline 时保持既有
     * {@code get()} 无限等待（兼容默认行为）；配置 Deadline 后以剩余时间为限，超时
     * 取消该 future + 中断同批在飞工具，并按 TIMEOUT outcome 回喂（词汇与单工具超时一致，
     * 复用 {@link ToolErrorFeedback} 通道）——不响应中断的挂死工具最多残留一个守护虚拟线程，
     * 会话绝不因此僵死。诚实边界：worker 若恰在取消后落盘 COMPLETED/FAILED 结局，事件日志
     * 会追加两条（先 TIMEOUT 后终局），与既有 {@code cancelInFlight} 语义一致。
     */
    private ToolResponseMessage.ToolResponse awaitCompletion(
            Future<ToolResponseMessage.ToolResponse> future,
            AssistantMessage.ToolCall toolCall) throws Exception {
        io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline deadline = this.turnDeadline;
        if (deadline.isNone()) {
            return future.get();
        }
        long remainingMillis = deadline.remainingMillis();
        if (remainingMillis > 0) {
            try {
                return future.get(remainingMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException expectedOnDeadlineExhausted) {
                // 剩余耗尽：落入下方统一 TIMEOUT 回喂
            }
        }
        future.cancel(true);
        cancelInFlight();
        return timeoutResponse(toolCall, "Turn 剩余预算耗尽，已取消");
    }

    /** impl-28：TIMEOUT 语义回喂（单一定语点：组锁/许可/派发/join 四处共用同一词汇）。 */
    private ToolResponseMessage.ToolResponse timeoutResponse(
            AssistantMessage.ToolCall toolCall, String detail) {
        String result = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                "执行超时（" + detail + "）");
        recordOutcome(toolCall,
                io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.TIMEOUT, result);
        return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), result);
    }

    /** impl-28：中断回喂（复用既有「执行被中断」词汇与 CANCELLED 结局）。 */
    private ToolResponseMessage.ToolResponse interruptedResponse(AssistantMessage.ToolCall toolCall) {
        Thread.currentThread().interrupt();
        String interrupted = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(), "执行被中断");
        recordOutcome(toolCall,
                io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.CANCELLED, interrupted);
        return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), interrupted);
    }

    /** impl-10 / T35：按策略整备回喂内容（FAILED_ONLY=同伴失败时成功者以占位提示替代）。 */
    private List<ToolResponseMessage.ToolResponse> responsesForModel(
            List<ToolResponseMessage.ToolResponse> responses) {
        if (batchFeedbackPolicy != BatchFeedbackPolicy.FAILED_ONLY) {
            return responses;
        }
        boolean anyFailure = responses.stream().anyMatch(r -> isErrorFeedback(r.responseData()));
        if (!anyFailure) {
            return responses;
        }
        return responses.stream().map(r -> {
            if (isErrorFeedback(r.responseData())) {
                return r;
            }
            return new ToolResponseMessage.ToolResponse(r.id(), r.name(),
                    "[本批有同伴失败：此工具已成功执行，结果已入事件日志（toolCallId="
                            + r.id() + "）可回查；本轮仅回喂失败信号]");
        }).toList();
    }

    /** ticket 29：错误反馈识别走结构化标记（{@link ToolFeedbackType}），不再散落字符串前缀判断。 */
    private static boolean isErrorFeedback(String content) {
        return ToolFeedbackType.isErrorFeedback(content);
    }

    public void cancelInFlight() {
        inFlight.forEach(future -> future.cancel(true));
    }

    private ToolResponseMessage.ToolResponse executeOne(
            AssistantMessage.ToolCall toolCall,
            Map<String, ToolCallback> callbacksByName,
            ToolContext toolContext) {
        ToolCallback callback = callbacksByName.get(toolCall.name());
        if (callback == null) {
            // 工具缺失同样回喂为结构化错误结果（含原入参），让模型自我纠错，而非崩溃/终结 Turn。
            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                    ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                            ToolErrorFeedback.missingToolReason(toolCall.name())));
        }
        // impl-04 / T30：执行前 schema 校验——未过则不执行工具，回喂校验反馈（REASK 通道）
        if (argsValidation) {
            Optional<String> violation = ToolArgsValidator.validate(
                    callback.getToolDefinition().inputSchema(), toolCall.arguments());
            if (violation.isPresent()) {
                validationFailures.incrementAndGet();
                String feedback = ToolValidationFeedback.format(toolCall.name(),
                        toolCall.arguments(), violation.get());
                recordOutcome(toolCall,
                        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.VALIDATION_REJECTED,
                        feedback);
                return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), feedback);
            }
        }
        io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline deadline = this.turnDeadline;
        if (deadline.isExpired()) {
            // impl-28：预算已耗尽——不占组锁/许可，直接 TIMEOUT 回喂（免无谓排队）
            return timeoutResponse(toolCall, "Turn 剩余预算已耗尽，未派发");
        }
        ReentrantLock lock = groupLock(toolCall.name());
        boolean locked;
        try {
            locked = tryAcquireGroupLock(lock, deadline);
        } catch (InterruptedException e) {
            return interruptedResponse(toolCall);
        }
        if (!locked) {
            return timeoutResponse(toolCall, "等待串行组「" + serialGroups.get(toolCall.name())
                    + "」执行权超时");
        }
        try {
            boolean permitted;
            try {
                permitted = tryAcquirePermit(deadline);
            } catch (InterruptedException e) {
                return interruptedResponse(toolCall);
            }
            if (!permitted) {
                return timeoutResponse(toolCall, "等待本 Turn 并发许可超时");
            }
            return dispatchTool(toolCall, callback, toolContext, deadline);
        } finally {
            lock.unlock();
        }
    }

    /**
     * impl-28 / spec 13 §core-2：组锁限时获取（挂起点②）。无 Deadline 时
     * {@code lockInterruptibly()}（互斥语义与既有 {@code synchronized} 等价，但可响应取消/
     * 中断）；配置 Deadline 后 {@code tryLock(剩余)}——组内同伴挂死至多消耗本 Turn 剩余时间。
     */
    private boolean tryAcquireGroupLock(ReentrantLock lock,
            io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline deadline)
            throws InterruptedException {
        if (deadline.isNone()) {
            lock.lockInterruptibly();
            return true;
        }
        long remainingMillis = deadline.remainingMillis();
        return remainingMillis > 0 && lock.tryLock(remainingMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * impl-28 / spec 13 §core-2：并发许可限时获取（挂起点③）。无 Deadline 时保持既有
     * {@code acquire()} 阻塞语义；配置 Deadline 后 {@code tryAcquire(剩余)}——许可等待
     * 超时按 TIMEOUT 回喂（许可只是并发闸门，等待超时本质是 Turn 预算耗尽，故与超时同
     * 词汇而非配额拒绝语义）。
     */
    private boolean tryAcquirePermit(
            io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline deadline)
            throws InterruptedException {
        if (deadline.isNone()) {
            turnPermits.acquire();
            return true;
        }
        long remainingMillis = deadline.remainingMillis();
        return remainingMillis > 0 && turnPermits.tryAcquire(remainingMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * impl-28：单次工具派发时限 = min(单工具超时, Deadline 剩余)——嵌套/子调用传递剩余时间
     * 而非重新计时。本方法约定「已持有一枚许可」，任何路径返回前都会归还。
     */
    private ToolResponseMessage.ToolResponse dispatchTool(
            AssistantMessage.ToolCall toolCall,
            ToolCallback callback,
            ToolContext toolContext,
            io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline deadline) {
        long timeoutMillis = effectiveToolTimeoutMillis(deadline);
        if (timeoutMillis <= 0) {
            turnPermits.release();
            return timeoutResponse(toolCall, "Turn 剩余预算已耗尽，未派发");
        }
        Future<String> task = executor.submit(() -> callback.call(toolCall.arguments(), toolContext));
        inFlight.add(task);
        String result;
        try {
            result = task.get(timeoutMillis, TimeUnit.MILLISECONDS);
            recordOutcome(toolCall,
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.COMPLETED, result);
        } catch (java.util.concurrent.CancellationException e) {
            result = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(), "执行已取消");
            recordOutcome(toolCall,
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.CANCELLED, result);
        } catch (TimeoutException e) {
            task.cancel(true);
            result = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                    "执行超时（" + formatTimeout(timeoutMillis) + "）");
            recordOutcome(toolCall,
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.TIMEOUT, result);
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            result = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                    "执行失败：" + cause);
            recordOutcome(toolCall,
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.FAILED, result);
        } finally {
            inFlight.remove(task);
            turnPermits.release();
        }
        return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), result);
    }

    /** impl-28：单次派发时限 = min(单工具超时, Deadline 剩余)；无 Deadline 即单工具超时。 */
    private long effectiveToolTimeoutMillis(
            io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline deadline) {
        if (deadline.isNone()) {
            return toolTimeout.toMillis();
        }
        return Math.min(toolTimeout.toMillis(), deadline.remainingMillis());
    }

    /** impl-28：超时时长文案（≥1s 用秒、否则毫秒；无 Deadline 时与既有「60s」格式一致）。 */
    private static String formatTimeout(long timeoutMillis) {
        return timeoutMillis >= 1000 ? timeoutMillis / 1000 + "s" : timeoutMillis + "ms";
    }

    private ReentrantLock groupLock(String toolName) {
        String group = serialGroups.get(toolName);
        return group == null ? new ReentrantLock()
                : groupLocks.computeIfAbsent(group, k -> new ReentrantLock());
    }

    private boolean isReturnDirect(ToolCallback callback) {
        return callback != null && callback.getToolMetadata() != null
                && callback.getToolMetadata().returnDirect();
    }
}
