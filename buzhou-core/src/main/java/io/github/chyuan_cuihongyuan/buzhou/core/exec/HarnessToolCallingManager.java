package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DedupGate;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
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
import java.time.Instant;
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
import java.util.function.Consumer;

public class HarnessToolCallingManager implements ToolCallingManager {

    /** ToolContext 中携带当前会话 id 的键（供内置工具做会话级解析，如 load_skill 绑定校验）。 */
    public static final String SESSION_ID_KEY = "buzhou.sessionId";

    /** 每轮工具并发上限规范默认（spec「并行工具调用」/ 05 原硬编码，抽常量消除魔法数字）。 */
    public static final int DEFAULT_MAX_CONCURRENT_PER_TURN = 8;

    /** 单工具执行超时规范默认（spec「并行工具调用」/ 05 原硬编码，抽常量消除魔法数字）。 */
    public static final Duration DEFAULT_TOOL_TIMEOUT = Duration.ofSeconds(60);

    /** 事件类型：扇出许可获取超时（工具名 + 已等待时长）。 */
    public static final String EVENT_TOOL_PERMIT_TIMEOUT = "backpressure.tool-permit-timeout";

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
    private final Duration permitAcquireTimeout;
    private final Map<String, String> serialGroups;
    private final SpanContextCarrier spanContextCarrier;
    private final String sessionId;
    private final DedupGate dedupGate;
    private final Consumer<SessionEvent> eventEmitter;
    private final ConcurrentHashMap<String, Object> groupLocks = new ConcurrentHashMap<>();
    private final List<Future<?>> inFlight = new CopyOnWriteArrayList<>();

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
        this(delegate, executor, maxConcurrencyPerTurn, toolTimeout, serialGroups,
                spanContextCarrier, sessionId, null);
    }

    public HarnessToolCallingManager(DefaultToolCallingManager delegate,
                                     ExecutorService executor,
                                     int maxConcurrencyPerTurn,
                                     Duration toolTimeout,
                                     Map<String, String> serialGroups,
                                     SpanContextCarrier spanContextCarrier,
                                     String sessionId,
                                     DedupGate dedupGate) {
        this(delegate, executor, maxConcurrencyPerTurn, toolTimeout, null, serialGroups,
                spanContextCarrier, sessionId, dedupGate, null);
    }

    /**
     * 全参数构造器（spec「背压 · 维度② 扇出闸」）。
     *
     * @param permitAcquireTimeout 扇出许可获取超时；{@code null} = 无限等待（保持现状），
     *                             {@code Duration.ZERO} = 立即失败（FAIL_FAST 档），正值 = 有界 tryAcquire
     * @param eventEmitter         事件发射器（{@code backpressure.tool-permit-timeout}）；{@code null} = no-op
     */
    public HarnessToolCallingManager(DefaultToolCallingManager delegate,
                                     ExecutorService executor,
                                     int maxConcurrencyPerTurn,
                                     Duration toolTimeout,
                                     Duration permitAcquireTimeout,
                                     Map<String, String> serialGroups,
                                     SpanContextCarrier spanContextCarrier,
                                     String sessionId,
                                     DedupGate dedupGate,
                                     Consumer<SessionEvent> eventEmitter) {
        this.delegate = delegate;
        this.executor = executor;
        this.turnPermits = new Semaphore(maxConcurrencyPerTurn);
        this.toolTimeout = toolTimeout;
        this.permitAcquireTimeout = permitAcquireTimeout;
        this.serialGroups = serialGroups == null ? Map.of() : serialGroups;
        this.spanContextCarrier = spanContextCarrier;
        this.sessionId = sessionId;
        this.dedupGate = dedupGate;
        this.eventEmitter = eventEmitter == null ? event -> {} : eventEmitter;
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
        for (ToolCallback callback : options.getToolCallbacks()) {
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
        ToolContext toolContext = new ToolContext(toolContextMap);
        List<Future<ToolResponseMessage.ToolResponse>> futures = new ArrayList<>();
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        boolean returnDirect = false;
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            futures.add(executor.submit(() -> executeOne(toolCall, callbacksByName, toolContext)));
        }
        for (Future<ToolResponseMessage.ToolResponse> future : futures) {
            try {
                ToolResponseMessage.ToolResponse response = future.get();
                responses.add(response);
                returnDirect |= isReturnDirect(callbacksByName.get(response.name()));
            } catch (Exception e) {
                throw new IllegalStateException("Tool execution aggregation failed", e);
            }
        }

        List<Message> conversationHistory = new ArrayList<>(prompt.getInstructions());
        conversationHistory.add(assistantMessage);
        conversationHistory.add(ToolResponseMessage.builder().responses(responses).build());
        return ToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .returnDirect(returnDirect)
                .build();
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
            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                    "未知工具：" + toolCall.name());
        }
        // 幂等去重（spec「幂等三件套」）：调用前原子 reserve；命中已回填结果则直接返回首次结果、
        // 不重执行（重试/恢复两条路径都经此闸门 → 效果恰好一次）
        String dedupKey = null;
        if (dedupGate != null && sessionId != null) {
            dedupKey = dedupGate.keyOf(toolCall.name(), toolCall.id(), toolCall.arguments(), toolContext);
            if (!dedupGate.recorder().reserve(sessionId, dedupKey)) {
                return dedupHitResponse(toolCall, dedupKey);
            }
        }
        Object lock = groupLock(toolCall.name());
        synchronized (lock) {
            String result;
            boolean succeeded = false;
            try {
                if (!acquirePermitOrTimeout(toolCall.name())) {
                    // 许可获取超时（FAIL_FAST 档等价 permitAcquireTimeout=0）：返回错误结果，
                    // 模型可见「工具过载未执行」语义——不阻断同轮其他工具、不吊死轮次。
                    releaseDedup(dedupKey);
                    return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                            "工具过载未执行（许可等待超时）");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                releaseDedup(dedupKey);
                return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                        "执行被中断");
            }
            Future<String> task = executor.submit(() -> callback.call(toolCall.arguments(), toolContext));
            inFlight.add(task);
            try {
                result = task.get(toolTimeout.toMillis(), TimeUnit.MILLISECONDS);
                succeeded = true;
            } catch (java.util.concurrent.CancellationException e) {
                result = "执行已取消";
            } catch (TimeoutException e) {
                task.cancel(true);
                result = "执行超时（" + toolTimeout.toSeconds() + "s）";
            } catch (Exception e) {
                result = "执行失败：" + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
            } finally {
                inFlight.remove(task);
                turnPermits.release();
            }
            // 成功才回填（崩溃窗口「已执行、未落库」内结果已被捕获）；失败/超时/取消释放 pending 允许重试
            if (dedupKey != null) {
                if (succeeded) {
                    dedupGate.recorder().fill(sessionId, dedupKey, result);
                } else {
                    dedupGate.recorder().release(sessionId, dedupKey);
                }
            }
            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), result);
        }
    }

    /**
     * 获取扇出许可（spec「背压 · 维度② 扇出闸」）。
     *
     * <p>{@code permitAcquireTimeout == null} 时无限 {@code acquire()}（保持现状）；
     * {@code Duration.ZERO} 时 {@code tryAcquire()} 立即裁决（FAIL_FAST 档）；
     * 正值时 {@code tryAcquire(timeoutMillis)} 有界等待，超时发 {@code backpressure.tool-permit-timeout}
     * 事件并返回 {@code false}（调用方返回错误结果而非吊死轮次）。
     *
     * @return true = 获取成功，false = 超时 / FAIL_FAST 档无可用许可
     */
    private boolean acquirePermitOrTimeout(String toolName) throws InterruptedException {
        if (permitAcquireTimeout == null) {
            turnPermits.acquire();
            return true;
        }
        if (permitAcquireTimeout.isZero() || permitAcquireTimeout.isNegative()) {
            return turnPermits.tryAcquire();
        }
        Instant start = Instant.now();
        boolean acquired = turnPermits.tryAcquire(
                permitAcquireTimeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!acquired) {
            eventEmitter.accept(new SessionEvent(EVENT_TOOL_PERMIT_TIMEOUT,
                    Map.of("toolName", toolName,
                            "waitedMs", Duration.between(start, Instant.now()).toMillis()),
                    Instant.now()));
        }
        return acquired;
    }

    /** 去重命中：等持有者回填后返回首次结果；超时仍 pending / 记录消失按交断语义处理（不重执行）。 */
    private ToolResponseMessage.ToolResponse dedupHitResponse(AssistantMessage.ToolCall toolCall,
                                                              String dedupKey) {
        java.util.Optional<String> filled = dedupGate.recorder().awaitFilled(sessionId, dedupKey, toolTimeout);
        if (filled.isPresent()) {
            dedupGate.emitHit(toolCall.name(), dedupKey);
            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), filled.get());
        }
        // 前次执行结果未知（崩溃窗口）：与悬空修复交断语义一致，交由模型知情，不擅自重执行副作用
        return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.DanglingCallRepairer.INTERRUPTED_RESULT);
    }

    private void releaseDedup(String dedupKey) {
        if (dedupKey != null) {
            dedupGate.recorder().release(sessionId, dedupKey);
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
