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
    private final ConcurrentHashMap<String, Object> groupLocks = new ConcurrentHashMap<>();
    private final List<Future<?>> inFlight = new CopyOnWriteArrayList<>();
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
        List<Future<ToolResponseMessage.ToolResponse>> futures = new ArrayList<>();
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        boolean returnDirect = false;
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            futures.add(executor.submit(() -> executeOne(toolCall, callbacksByName, toolContext)));
        }
        for (int i = 0; i < futures.size(); i++) {
            AssistantMessage.ToolCall toolCall = toolCalls.get(i);
            ToolResponseMessage.ToolResponse response;
            try {
                response = futures.get(i).get();
            } catch (Exception e) {
                // 兜底：任何漏网的执行异常也降级为错误反馈，保证每个 tool_call 恒有一个
                // ToolResponse（协议要求）且 Turn 不死，而非上抛终结整轮。
                Throwable cause = e.getCause() == null ? e : e.getCause();
                response = new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                        ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                                "执行失败：" + cause));
            }
            responses.add(response);
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

    private static boolean isErrorFeedback(String content) {
        return content != null && (content.startsWith("[工具执行失败]")
                || content.startsWith("[工具参数校验失败]"));
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
        Object lock = groupLock(toolCall.name());
        synchronized (lock) {
            String result;
            try {
                turnPermits.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String interrupted = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(), "执行被中断");
                recordOutcome(toolCall,
                        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.CANCELLED, interrupted);
                return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), interrupted);
            }
            Future<String> task = executor.submit(() -> callback.call(toolCall.arguments(), toolContext));
            inFlight.add(task);
            try {
                result = task.get(toolTimeout.toMillis(), TimeUnit.MILLISECONDS);
                recordOutcome(toolCall,
                        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.COMPLETED, result);
            } catch (java.util.concurrent.CancellationException e) {
                result = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(), "执行已取消");
                recordOutcome(toolCall,
                        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome.CANCELLED, result);
            } catch (TimeoutException e) {
                task.cancel(true);
                result = ToolErrorFeedback.format(toolCall.name(), toolCall.arguments(),
                        "执行超时（" + toolTimeout.toSeconds() + "s）");
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
    }

    private Object groupLock(String toolName) {
        String group = serialGroups.get(toolName);
        return group == null ? new Object() : groupLocks.computeIfAbsent(group, k -> new Object());
    }

    private boolean isReturnDirect(ToolCallback callback) {
        return callback != null && callback.getToolMetadata() != null
                && callback.getToolMetadata().returnDirect();
    }
}
