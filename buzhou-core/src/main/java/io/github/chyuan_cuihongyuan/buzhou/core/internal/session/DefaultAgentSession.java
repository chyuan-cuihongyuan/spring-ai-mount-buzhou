package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultSessionEventContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.MediaRef;
import io.github.chyuan_cuihongyuan.buzhou.core.session.StructuredOutputException;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.LeaseLostException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class DefaultAgentSession implements AgentSession {

    private static final System.Logger LOGGER =
            System.getLogger(DefaultAgentSession.class.getName());

    /**
     * impl-28 / spec 13 §core-2：模型调用守卫线程池（虚拟线程：每次守卫一线程，随用随建）。
     * Spring AI ChatClient 无 per-call 超时面，兜底只能把整个调用（含 Advisor 链内的工具
     * 递归循环）搬到守卫线程上、以剩余时间封顶等待——守卫线程被中断即尽力中止底层调用。
     */
    private static final ExecutorService MODEL_CALL_GUARD_EXECUTOR =
            Executors.newThreadPerTaskExecutor(
                    io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory.virtual("model-guard"));
    /**
     * impl-28：Deadline 耗尽后的收尾宽限——工具级 Deadline 在预算点截断并回喂，宽限给模型
     * 最后一次组织最终回复的机会（预算内优雅收尾优先）；宽限耗尽仍无终局（模型自身挂死）
     * 则硬截断上抛 TIMEOUT。硬上界 = 预算 + 本宽限。
     */
    private static final Duration MODEL_FINALIZE_GRACE = Duration.ofSeconds(5);

    private final String appId;
    private final String agentName;
    private final String sessionId;
    private final ChatClient chatClient;
    private final SessionResourceRegistry registry;
    private final Runnable onClose;
    private final HookChain hookChain;
    private final HookEnvironment hookEnv;
    private final HarnessToolCallingManager toolManager;
    private final SpanContextCarrier spanContextCarrier;
    private final List<SessionObserver> observers;
    private final List<SessionEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    /** impl-28：有效 Turn 预算（min(turnDeadline, loopTimeout)）；null = 不设（既有直调行为）。 */
    private final Duration turnBudget;
    /** impl-33 / spec 13 §core-3：会话租约哨兵；null = 无租约语义路径（Buzhou.enhance 等既有行为）。 */
    private final SessionLeaseGuard leaseGuard;
    private final io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakHandle leakHandle;
    /**
     * impl-30 / spec 13 §core-1：在途 Turn 权威计数（chat 入口增/finally 减；stream 入口增/
     * doFinally 减）——停机排空等待的裁决源，覆盖正常/异常/取消全部终结路径。
     * 诚实边界：stream() 返回后从未订阅的流不产生终结信号，计数残留 +1，由会话 close 的
     * closed 标记兜底安定（与既有 onTurnStart 预先通知的泄漏面一致）。
     */
    private final AtomicInteger inFlightTurns = new AtomicInteger();
    /** impl-30 / spec 13 §core-1：停机拒新标记（运行时 shutdown 序列置位；chat/stream 即刻拒绝）。 */
    private volatile boolean rejectingNewTurns;

    /**
     * impl-34 / spec 13 §core-4：事件分发模式（null = SYNC 既有内联行为）；
     * BUFFERED 时懒创建 {@link BufferedEventDispatcher}（首个事件到达才起分发线程）。
     */
    private final io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatchConfig;
    private BufferedEventDispatcher eventDispatcher;
    /** impl-35 / spec 13 §stores-6：级联清理协调器；null = delete() 退化为 close()（既有构造兼容）。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner sessionCleaner;
    /**
     * spec 46 §B / T171 / impl-140：流式回复累计时长上限（慢滴流防护）；null = 不设
     * （显式关闭）。与相邻信号间隔 timeout（{@code turnBudget}）正交，两道上限先到者生效。
     */
    private final Duration streamTotalTimeout;

    /** spec 46 §B：流累计上限框架默认（10 分钟——远超正常流式回复，只拦慢性挂死）。 */
    static final Duration DEFAULT_STREAM_TOTAL_TIMEOUT = Duration.ofMinutes(10);

    /** spec 47 §A / T172 / impl-141：MDC 键（buzhou.* 命名空间，宿主 %X 直接引用）。 */
    static final String MDC_SESSION_ID = "buzhou.sessionId";
    static final String MDC_TURN_SEQ = "buzhou.turnSeq";

    /** spec 47 §B / T173：反馈 state store 键前缀（scanByPrefix 枚举面；导出衔接 T174 消费）。 */
    static final String FEEDBACK_KEY_PREFIX = "buzhou.feedback.";

    /** spec 47 §B / T173：反馈键去重序号（同轮同毫秒多次反馈不撞键）。 */
    private final java.util.concurrent.atomic.AtomicLong feedbackSeq = new java.util.concurrent.atomic.AtomicLong();

    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               HarnessToolCallingManager toolManager) {
        this(appId, agentName, sessionId, chatClient, registry, onClose, hookChain, hookEnv,
                toolManager, new SpanContextCarrier(), List.of());
    }

    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               HarnessToolCallingManager toolManager,
                               SpanContextCarrier spanContextCarrier,
                               List<SessionObserver> observers) {
        this(appId, agentName, sessionId, chatClient, registry, onClose, hookChain, hookEnv,
                toolManager, spanContextCarrier, observers, null);
    }

    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               HarnessToolCallingManager toolManager,
                               SpanContextCarrier spanContextCarrier,
                               List<SessionObserver> observers,
                               Duration turnBudget) {
        this(appId, agentName, sessionId, chatClient, registry, onClose, hookChain, hookEnv,
                toolManager, spanContextCarrier, observers, turnBudget, null);
    }

    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               HarnessToolCallingManager toolManager,
                               SpanContextCarrier spanContextCarrier,
                               List<SessionObserver> observers,
                               Duration turnBudget,
                               SessionLeaseGuard leaseGuard) {
        this(appId, agentName, sessionId, chatClient, registry, onClose, hookChain, hookEnv,
                toolManager, spanContextCarrier, observers, turnBudget, leaseGuard, null);
    }

    /**
     * impl-34 / spec 13 §core-4：完整构造入口——{@code eventDispatchConfig} 为 BUFFERED 时
     * 事件经有界队列异步分发（慢监听器不拖慢 Turn 主链路；溢出按策略处理、丢弃计数可见）。
     */
    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               HarnessToolCallingManager toolManager,
                               SpanContextCarrier spanContextCarrier,
                               List<SessionObserver> observers,
                               Duration turnBudget,
                               SessionLeaseGuard leaseGuard,
                               io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatchConfig) {
        this(appId, agentName, sessionId, chatClient, registry, onClose, hookChain, hookEnv,
                toolManager, spanContextCarrier, observers, turnBudget, leaseGuard,
                eventDispatchConfig, null);
    }

    /**
     * impl-35 / spec 13 §stores-6：完整构造入口 + 级联清理协调器——
     * {@code sessionCleaner} 非 null 时 {@link #delete()} 先 close 再一次级联删存储；
     * null 时 delete() 退化为 close()。
     */
    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               HarnessToolCallingManager toolManager,
                               SpanContextCarrier spanContextCarrier,
                               List<SessionObserver> observers,
                               Duration turnBudget,
                               SessionLeaseGuard leaseGuard,
                               io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatchConfig,
                               io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner sessionCleaner) {
        this(appId, agentName, sessionId, chatClient, registry, onClose, hookChain, hookEnv,
                toolManager, spanContextCarrier, observers, turnBudget, leaseGuard,
                eventDispatchConfig, sessionCleaner, null);
    }

    /**
     * spec 46 §B / T171 / impl-140：完整构造入口 + 流累计上限——{@code streamTotalTimeout}
     * 为 null 时取框架默认 10m；{@link Duration#ZERO} 或负值 = 显式关闭（既有不限语义）。
     */
    public DefaultAgentSession(String appId, String agentName, String sessionId,
                               ChatClient chatClient, SessionResourceRegistry registry,
                               Runnable onClose, HookChain hookChain, HookEnvironment hookEnv,
                               HarnessToolCallingManager toolManager,
                               SpanContextCarrier spanContextCarrier,
                               List<SessionObserver> observers,
                               Duration turnBudget,
                               SessionLeaseGuard leaseGuard,
                               io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatchConfig,
                               io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner sessionCleaner,
                               Duration streamTotalTimeout) {
        this.appId = appId;
        this.agentName = agentName;
        this.sessionId = sessionId;
        this.chatClient = chatClient;
        this.registry = registry;
        this.onClose = onClose;
        this.hookChain = hookChain;
        this.hookEnv = hookEnv;
        this.toolManager = toolManager;
        this.spanContextCarrier = spanContextCarrier;
        this.observers = new CopyOnWriteArrayList<>(observers);
        this.turnBudget = turnBudget;
        this.leaseGuard = leaseGuard;
        this.streamTotalTimeout = streamTotalTimeout == null
                ? DEFAULT_STREAM_TOTAL_TIMEOUT
                : (streamTotalTimeout.isZero() || streamTotalTimeout.isNegative()
                        ? null : streamTotalTimeout);
        // impl-41 / spec 13 §T66：会话挂点——未 close 即被 GC = 资源泄漏嫌疑
        this.leakHandle = io.github.chyuan_cuihongyuan.buzhou.core.leak.LeakDetectorHolder
                .detector().track("session:" + sessionId);
        this.eventDispatchConfig = eventDispatchConfig;
        this.sessionCleaner = sessionCleaner;
        this.hookEnv.bindEventPublisher(this::dispatchEvent);
        observers.forEach(SessionObserver::onOpen);
    }

    public SpanContextCarrier spanContextCarrier() {
        return spanContextCarrier;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String appId() {
        return appId;
    }

    @Override
    public String agentName() {
        return agentName;
    }

    @Override
    public String chat(String input) {
        return chat(input, java.util.List.of());
    }

    /** 多模态输入（spec 27 / T106 / impl-81）：媒体经 PromptUserSpec 随本轮下发。 */
    @Override
    public String chat(String input, java.util.List<MediaRef> media) {
        ensureOpen();
        ensureLeaseHeld();
        ensureNotShuttingDown();
        // impl-30：在途计数（finally 减——任何终结路径均收口，停机排空的裁决源）
        acquireTurnSlot();
        try {
            return doChat(input, media);
        } finally {
            inFlightTurns.decrementAndGet();
        }
    }

    /**
     * 结构化输出（spec 19 / T87 / impl-62）：BeanOutputConverter 注入 schema；解析失败发
     * {@code structured.reask} 事件后追加一次完整 turn（复用 doChat 全管线——hook/预算/失控
     * 检测对 REASK 轮全部生效、诚实计入预算）；再失败抛 {@link StructuredOutputException}。
     */
    @Override
    public <T> T chatForEntity(String input, Class<T> type) {
        return chatForEntity(input, java.util.List.of(), type);
    }

    @Override
    public <T> T chatForEntity(String input, java.util.List<MediaRef> media, Class<T> type) {
        ensureOpen();
        ensureLeaseHeld();
        ensureNotShuttingDown();
        acquireTurnSlot();
        try {
            org.springframework.ai.converter.BeanOutputConverter<T> converter =
                    new org.springframework.ai.converter.BeanOutputConverter<>(type);
            String first = doChat(input + "\n" + converter.getFormat(), media);
            String firstError = parseError(first, converter);
            if (firstError == null) {
                return converter.convert(first);
            }
            dispatchEvent(SessionEvent.of("structured.reask"));
            String second = doChat(input + "\n" + converter.getFormat()
                    + "\n[系统反馈] 你上一次的输出无法解析（" + firstError
                    + "）。请只输出一个符合上述格式的 JSON，不要包含任何其他文本或代码块标记。", media);
            String secondError = parseError(second, converter);
            if (secondError == null) {
                return converter.convert(second);
            }
            throw new StructuredOutputException("结构化输出解析失败（REASK 一次后仍不合规）：首次="
                    + summarize(first) + "，重问=" + summarize(second)
                    + "；解析错误=" + secondError, null);
        } finally {
            inFlightTurns.decrementAndGet();
        }
    }

    /** spec 27 / T106：用户输入 + 媒体引用组装（无媒体走纯文本，与既有行为零差异）。 */
    private static java.util.function.Consumer<org.springframework.ai.chat.client.ChatClient.PromptUserSpec>
    applyMedia(String input, java.util.List<MediaRef> media) {
        return u -> {
            u.text(input);
            if (media != null && !media.isEmpty()) {
                u.media(media.stream()
                        .map(ref -> new org.springframework.ai.content.Media(
                                org.springframework.util.MimeType.valueOf(ref.mimeType()), ref.uri()))
                        .toArray(org.springframework.ai.content.Media[]::new));
            }
        };
    }

    /** 解析失败返回错误摘要（成功返回 null）；convert 抛异常 / 返回 null 均视为失败。 */
    private <T> String parseError(String response, org.springframework.ai.converter.BeanOutputConverter<T> converter) {
        if (response == null || response.isBlank()) {
            return "输出为空";
        }
        try {
            if (converter.convert(response) == null) {
                return "解析结果为 null";
            }
            return null;
        } catch (RuntimeException e) {
            String message = e.getMessage();
            return message == null ? e.getClass().getSimpleName()
                    : message.length() > 200 ? message.substring(0, 200) : message;
        }
    }

    private static String summarize(String response) {
        if (response == null) {
            return "(null)";
        }
        return response.length() <= 120 ? response : response.substring(0, 120) + "…";
    }

    private String doChat(String input, java.util.List<MediaRef> media) {
        int turnSeq = hookEnv.nextTurn();
        // spec 47 §A / T172 / impl-141：轮次调用线程 MDC 关联（终结必清；命名空间 buzhou.*）
        org.slf4j.MDC.put(MDC_SESSION_ID, sessionId);
        org.slf4j.MDC.put(MDC_TURN_SEQ, String.valueOf(turnSeq));
        try {
            return doChatTurn(turnSeq, input, media);
        } finally {
            org.slf4j.MDC.remove(MDC_SESSION_ID);
            org.slf4j.MDC.remove(MDC_TURN_SEQ);
        }
    }

    private String doChatTurn(int turnSeq, String input, java.util.List<MediaRef> media) {
        long turnStartNanos = System.nanoTime(); // impl-41 / spec 13 §T66：turn.duration
        observers.forEach(o -> o.onTurnStart(turnSeq, input));
        DefaultTurnContext turnCtx = new DefaultTurnContext(hookEnv, input);
        HookResult before = hookChain.beforeTurn(turnCtx);
        if (before instanceof HookResult.Block block) {
            return block.reason();
        }
        String response;
        try {
            response = callModelWithinBudget(turnSeq, () -> chatClient.prompt()
                    .user(applyMedia(turnCtx.input(), media))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content());
            // impl-33 / spec 13 §core-3：Turn 收尾提交点 fence——history 的逐条写入发生在
            // 顾问链内（轮间 fence 已守），此处校验通过后 Completed-Turn 的收尾写入
            // （afterTurn 钩子的 run-state 快照、onTurnEnd 观察者的 span 收口）才发生
            verifyLeaseAtCommit();
        } catch (LeaseLostException e) {
            abortTurnAsLeaseLost(turnSeq, e);
            recordTurnDuration(turnStartNanos, "failed");
            throw e;
        } catch (RuntimeException e) {
            recordTurnDuration(turnStartNanos, "failed");
            throw e;
        }
        turnCtx.markResponded(response);
        hookChain.afterTurn(turnCtx);
        observers.forEach(o -> o.onTurnEnd(turnSeq, response));
        recordTurnDuration(turnStartNanos, "ok");
        return turnCtx.response();
    }

    /** impl-41 / spec 13 §T66：Turn 时长（outcome=ok|failed；tag 无 sessionId）。 */
    private void recordTurnDuration(long startNanos, String outcome) {
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .timer("buzhou.turn.duration",
                        java.time.Duration.ofNanos(System.nanoTime() - startNanos),
                        "outcome", outcome);
    }

    /** impl-30 / spec 13 §core-1：在途 Turn 数（0 = 无在途，停机排空安定条件之一）。 */
    public int inFlightTurns() {
        return inFlightTurns.get();
    }

    /**
     * spec 40 §B / T152 / impl-123：会话单飞闸——在途计数 CAS 0→1 占位，占位失败即同会话
     * 已有在途轮次，快速失败（{@link ErrorCode#TURN_IN_FLIGHT}，NON_RETRYABLE）。
     * 语义由既往「并发同会话轮次属未定义使用」升级为确定拒绝；闸在轮次终结（含异常收尾）释放。
     */
    private void acquireTurnSlot() {
        if (!inFlightTurns.compareAndSet(0, 1)) {
            throw new BuzhouException(ErrorCode.TURN_IN_FLIGHT,
                    "会话已有在途轮次（sessionId=" + sessionId + "）：同会话轮次不并发（单飞闸）；"
                            + "请等待在途轮次终结，或为新交互 spawn 独立会话");
        }
    }

    /**
     * impl-28 / spec 13 §core-2：模型调用兜底（挂起点④）。配置了 turnDeadline/loopTimeout 时，
     * 整个 ChatClient 调用（含 Advisor 链内的工具递归循环）不得超过 预算 + {@link #MODEL_FINALIZE_GRACE}。
     * 实现方式：CompletableFuture 在守卫虚拟线程上执行调用、主线程限时等待——Spring AI
     * ChatClient 无 per-call 超时面（观察 {@code .call()} 即返回阻塞 content），故不尝试改
     * Spring AI 依赖，也不给 ChatModel 套装饰器（无法区分「本轮模型调用」与「工具后再次调用」
     * 的归属）。超时走既有错误路径：上抛 {@link BuzhouException}(TIMEOUT) 并通知
     * {@code onTurnError}；模型侧原生异常原样还原类型上抛（与工具侧通道正交、互不吞没）。
     * 未配置预算时保持既有直调行为（默认保守不限）。
     *
     * <p>诚实边界：超时后 {@code cancel(true)} 中断守卫线程——可中断阻塞（sleep/IO 多数）
     * 即刻中止；个别不可中断的系统调用会残留一个守护虚拟线程（不占平台线程、随进程结束），
     * 会话调用方不再被拖住，这是「最坏情况单点泄漏而非整会话僵死」的取舍。
     */
    private String callModelWithinBudget(int turnSeq, Supplier<String> modelCall) {
        Duration budget = turnBudget;
        if (budget == null) {
            return modelCall.get();
        }
        Duration hardBound = budget.plus(MODEL_FINALIZE_GRACE);
        CompletableFuture<String> guarded =
                CompletableFuture.supplyAsync(modelCall, MODEL_CALL_GUARD_EXECUTOR);
        try {
            return guarded.get(hardBound.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            guarded.cancel(true);
            BuzhouException timeout = new BuzhouException(ErrorCode.TIMEOUT,
                    "模型调用超时：Turn 预算 " + budget.toMillis() + "ms（含 "
                            + MODEL_FINALIZE_GRACE.toSeconds() + "s 收尾宽限）已耗尽", e);
            observers.forEach(o -> o.onTurnError(turnSeq, timeout));
            throw timeout;
        } catch (ExecutionException e) {
            // 还原底层异常类型：模型侧异常照常按原类型暴露（既有错误路径不变）
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("模型调用失败", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED, "模型调用等待被中断", e);
        }
    }

    @Override
    public Flux<ChatResponse> stream(String input) {
        return stream(input, java.util.List.of());
    }

    /** 流式 + 多模态输入（spec 27 / T106 / impl-81）。 */
    @Override
    public Flux<ChatResponse> stream(String input, java.util.List<MediaRef> media) {
        ensureOpen();
        ensureLeaseHeld();
        ensureNotShuttingDown();
        // impl-30：在途计数在订阅终结（doFinally）时递减——与 onTurnStart 的预先通知时点对齐
        acquireTurnSlot();
        int turnSeq = hookEnv.nextTurn();
        observers.forEach(o -> o.onTurnStart(turnSeq, input));
        DefaultTurnContext turnCtx = new DefaultTurnContext(hookEnv, input);
        HookResult before = hookChain.beforeTurn(turnCtx);
        if (before instanceof HookResult.Block block) {
            inFlightTurns.decrementAndGet(); // 未返回 Flux 前即终结，同步收口计数
            recordStreamCancelled("guard");
            return Flux.error(new IllegalStateException(block.reason()));
        }
        StringBuilder replyAccumulator = new StringBuilder();
        Flux<ChatResponse> stream = chatClient.prompt()
                .user(applyMedia(turnCtx.input(), media))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .chatResponse();
        if (turnBudget != null) {
            // impl-28：流式兜底——相邻信号间隔不得超过 预算+收尾宽限（挂死/断流的模型调用被
            // TimeoutException 截断，走既有 doOnError→onTurnError 路径）。
            stream = stream.timeout(turnBudget.plus(MODEL_FINALIZE_GRACE));
        }
        if (streamTotalTimeout != null) {
            // spec 46 §B / T171：累计上限——自订阅起整条流不得超过 stream-total-timeout（慢滴流
            // 防护：间隔 timeout 对持续滴流无效）。超限以标记异常 onError 终结（takeUntilOther
            // 语义：other 出错即向下游传播并取消上游），走既有 doOnError→failTurnOnce 收尾。
            Duration cap = streamTotalTimeout;
            stream = stream.takeUntilOther(
                    reactor.core.publisher.Mono.delay(cap).then(
                            reactor.core.publisher.Mono.error(new StreamTotalTimeoutException(cap))));
        }
        // impl-30 / spec 13 §core-1：轮次终结一次性守卫——complete/error/cancel/timeout 四路
        // 终结信号共用（正常完成与异常收尾语义均只执行一次，幂等）
        AtomicBoolean finalized = new AtomicBoolean();
        return stream
                .doOnNext(resp -> {
                    if (resp != null && resp.getResult() != null && resp.getResult().getOutput() != null
                            && resp.getResult().getOutput().getText() != null) {
                        replyAccumulator.append(resp.getResult().getOutput().getText());
                    }
                })
                .doOnComplete(() -> completeStreamTurnOnce(finalized, turnSeq, turnCtx, replyAccumulator))
                .doOnError(e -> {
                    // spec 46 §B / T171：超时类错误归 deadline（间隔 timeout 与累计超限同因计数）
                    if (e instanceof TimeoutException || e instanceof StreamTotalTimeoutException) {
                        recordStreamCancelled("deadline");
                    }
                    failTurnOnce(finalized, turnSeq, e);
                })
                // impl-30 / spec 13 §core-1：doFinally 收尾——订阅者 cancel 与正常完成同路执行
                // span 关闭、turn 记账（onTurnError 终结在途 Turn）；在途计数在此递减
                .doFinally(signal -> {
                    inFlightTurns.decrementAndGet();
                    if (signal == SignalType.CANCEL) {
                        // spec 46 §B / T171：订阅者主动断开归 client（含上游算子主动取消时
                        // 走 error 路径、不与此重复计数的先验：CANCEL 只对应下游 dispose）
                        recordStreamCancelled("client");
                        failTurnOnce(finalized, turnSeq,
                                new CancellationException("流式订阅被取消，Turn 终止"));
                    }
                });
    }

    /** spec 46 §B / T171：流终止原因分类计数（有界枚举 client|deadline|guard；预注册家族）。 */
    private static void recordStreamCancelled(String reason) {
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.stream.cancelled", 1, "reason", reason);
    }

    /**
     * impl-30：流式正常完成收尾（一次性）——与 chat() 对齐的轮次收尾：提交点 fence +
     * afterTurn 钩子 + onTurnEnd（TURN span 关闭防泄漏）；fence 失败按 LeaseLost 语义中止
     * （不入 Completed-Turn；doOnComplete 内抛出 → 订阅者以 onError 看到 LeaseLost，
     * 后续 doOnError 经 finalized 守卫不再重复通知）。
     */
    private void completeStreamTurnOnce(AtomicBoolean finalized, int turnSeq,
                                        DefaultTurnContext turnCtx, StringBuilder replyAccumulator) {
        if (!finalized.compareAndSet(false, true)) {
            return;
        }
        try {
            verifyLeaseAtCommit();
            turnCtx.markResponded(replyAccumulator.toString());
            hookChain.afterTurn(turnCtx);
            observers.forEach(o -> o.onTurnEnd(turnSeq, turnCtx.response()));
        } catch (LeaseLostException e) {
            abortTurnAsLeaseLost(turnSeq, e);
            throw e;
        }
    }

    /**
     * impl-30：轮次异常终结收尾（一次性）——onTurnError 收口在途 Turn span/记账；
     * timeout/模型错误/订阅取消三路共用，finalized 守卫保证与正常完成互斥且不重复。
     */
    private void failTurnOnce(AtomicBoolean finalized, int turnSeq, Throwable error) {
        if (!finalized.compareAndSet(false, true)) {
            return;
        }
        observers.forEach(o -> o.onTurnError(turnSeq, error));
    }

    /** 取消在途轮次：中断全部在途工具调用；会话不谢幕，可继续 chat。 */
    @Override
    public void cancel() {
        cancel(io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode.IMMEDIATE);
    }

    /**
     * spec 47 §B / T173 / impl-142：turn 级反馈捕获——校验 → state store 持久化
     * （{@code buzhou.feedback.<turnSeq>.<epochMillis>}，URLEncoded k=v 五字段）→
     * {@code turn.feedback} 会话事件外发（监听者/webhook 零改造收到）。
     */
    @Override
    public void rateTurn(int turnSeq, String type, String value, String comment, String source) {
        ensureOpen();
        if (turnSeq < 1 || turnSeq > hookEnv.currentTurn()) {
            throw new IllegalArgumentException("rateTurn turnSeq 超范围（" + turnSeq + "）："
                    + "目标轮次须已存在（1 ≤ turnSeq ≤ " + hookEnv.currentTurn() + "）");
        }
        String normalizedType = validateFeedback(turnSeq, type, value, source);
        String src = source == null || source.isBlank() ? "user" : source;
        java.time.Instant at = java.time.Instant.now();
        String key = FEEDBACK_KEY_PREFIX + turnSeq + "." + at.toEpochMilli()
                + "-" + feedbackSeq.incrementAndGet();
        String encoded = encodeFeedback(normalizedType, value, comment, src, at);
        hookEnv.stateStore().put(sessionId, new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                key, encoded, "turn-feedback", turnSeq, null, at));
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("turnSeq", turnSeq);
        payload.put("type", normalizedType);
        payload.put("value", value);
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment);
        }
        payload.put("source", src);
        dispatchEvent(new SessionEvent("turn.feedback", payload, at));
    }

    /** spec 47 §B：反馈校验（非法 IllegalArgumentException，文案含修复建议；normalize type）。 */
    private static String validateFeedback(int turnSeq, String type, String value, String source) {
        if (type == null) {
            throw new IllegalArgumentException("rateTurn type 不可为空：boolean | numeric | categorical");
        }
        String normalized = type.trim().toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "boolean" -> {
                if (value == null || !"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException(
                            "rateTurn boolean 型 value 须为 true/false（收到 " + value + "）");
                }
            }
            case "numeric" -> {
                try {
                    Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "rateTurn numeric 型 value 须可解析为整数（收到 " + value + "）", e);
                }
            }
            case "categorical" -> {
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException("rateTurn categorical 型 value 不可为空短串");
                }
            }
            default -> throw new IllegalArgumentException(
                    "rateTurn type 非法（" + type + "）：boolean | numeric | categorical");
        }
        if (source != null && !source.isBlank()
                && !"user".equals(source) && !"implicit".equals(source)) {
            throw new IllegalArgumentException("rateTurn source 非法（" + source + "）：user | implicit");
        }
        return normalized;
    }

    /** spec 47 §B：反馈 lossless 编码（core 零 JSON 依赖的 k=v& 形态；URLEncoder 双向可解）。 */
    private static String encodeFeedback(String type, String value, String comment,
                                         String source, java.time.Instant at) {
        java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("type", type);
        fields.put("value", value == null ? "" : value);
        fields.put("comment", comment == null ? "" : comment);
        fields.put("source", source);
        fields.put("at", at.toString());
        StringBuilder sb = new StringBuilder();
        fields.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(k).append('=').append(urlEncode(v));
        });
        return sb.toString();
    }

    private static String urlEncode(String raw) {
        try {
            return java.net.URLEncoder.encode(raw == null ? "" : raw, java.nio.charset.StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            // UTF_8 恒可用；防御性兜底（不可达路径）
            return String.valueOf(raw);
        }
    }

    /** impl-05 / T31：按模式取消（三档语义见 {@link io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode}）。 */
    @Override
    public void cancel(io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode mode) {
        ensureOpen();
        io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode effective = mode == null
                ? io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode.IMMEDIATE : mode;
        toolManager.requestCancel(effective);
        observers.forEach(SessionObserver::onCancel);
        dispatchEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                "session.cancelled",
                java.util.Map.of("cancelMode", effective.name())));
    }

    /**
     * impl-30 / spec 13 §core-1：关闭收尾全程「清理优先、异常收集」——逐 observer 隔离
     * onClose、隔离 onClose.run()（资源注册表逆序关闭），<b>无论谁失败</b>都继续执行
     * 事件分发、{@code listeners.clear()} 与 span 清理（既有实现里 observer/listener 异常
     * 会跳过 listeners.clear()——本片补齐）；全部清理完毕后首个失败上抛、其余 suppressed。
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            leakHandle.close();
            if (leaseGuard != null) {
                leaseGuard.close();
            }
            List<RuntimeException> failures = new ArrayList<>();
            for (SessionObserver observer : observers) {
                try {
                    observer.onClose();
                } catch (RuntimeException e) {
                    LOGGER.log(System.Logger.Level.ERROR,
                            "会话关闭通知 observer 失败（不跳过后续清理）：sessionId=" + sessionId, e);
                    failures.add(e);
                }
            }
            try {
                onClose.run();
            } catch (RuntimeException e) {
                failures.add(e);
            }
            dispatchEvent(SessionEvent.of("session.closed"));
            listeners.clear();
            spanContextCarrier.clear();
            throwAggregated(failures);
        }
    }

    /**
     * impl-35 / spec 13 §stores-6：删除会话 = 一次调用清干净。先 close()（资源注册表清空、
     * 租约释放、executor 排空；已 close 时幂等跳过），再 SessionCleaner 一次级联删存储。
     * 两侧失败各自收集、互不跳过——全部尝试完毕后首个失败上抛、其余 suppressed
     * （与 impl-30 close 的「清理优先、异常聚合」语义对齐）。
     */
    @Override
    public void delete() {
        List<RuntimeException> failures = new ArrayList<>();
        try {
            close();
        } catch (RuntimeException e) {
            failures.add(e);
        }
        if (sessionCleaner != null) {
            failures.addAll(sessionCleaner.deleteSession(sessionId).failures().values());
        }
        throwAggregated(failures);
    }

    /** 首个失败上抛、其余 suppressed（impl-30 close 与 impl-35 delete 共用的聚合收口）。 */
    private static void throwAggregated(List<RuntimeException> failures) {
        if (!failures.isEmpty()) {
            RuntimeException first = failures.getFirst();
            for (int i = 1; i < failures.size(); i++) {
                first.addSuppressed(failures.get(i));
            }
            throw first;
        }
    }

    @Override
    public void addEventListener(SessionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeEventListener(SessionEventListener listener) {
        listeners.remove(listener);
    }

    /** impl-34 / spec 13 §core-4：buffered 模式的事件总线统计（丢弃可见）；SYNC 无队列返回空。 */
    @Override
    public java.util.Optional<io.github.chyuan_cuihongyuan.buzhou.core.session.EventBusStats> eventBusStats() {
        BufferedEventDispatcher dispatcher = eventDispatcher;
        return dispatcher == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(dispatcher.stats());
    }

    /**
     * impl-30 / spec 13 §core-1：事件分发逐 listener 隔离——hook 链与每个
     * {@link SessionEventListener} 各自 try/catch（ERROR 日志），单个异常不阻断其余
     * listener 的分发，也绝不向上传播跳过 close() 的后续清理步骤（listeners.clear 等）。
     *
     * <p>impl-34 / spec 13 §core-4：{@code buffered} 模式（opt-in）下事件入有界队列由
     * 分发线程异步交付（本方法体内联逻辑即交付回调 {@link #deliverEvent}）；SYNC 默认
     * 模式维持既有内联行为不变。
     */
    /** 包级分发入口（impl-63：runtime.fork 发 session.forked 用，同管线）。 */
    void dispatchEventInternal(SessionEvent event) {
        dispatchEvent(event);
    }

    private void dispatchEvent(SessionEvent event) {
        if (eventDispatchConfig != null && eventDispatchConfig.isBuffered()) {
            BufferedEventDispatcher dispatcher = eventDispatcher;
            if (dispatcher == null) {
                synchronized (this) {
                    if (eventDispatcher == null) {
                        // 懒创建：首个事件到达才起分发线程；关闭挂进会话资源注册表（LIFO 排空）
                        eventDispatcher = new BufferedEventDispatcher(
                                sessionId, eventDispatchConfig, this::deliverEvent);
                        registry.register("event-dispatcher", () -> eventDispatcher.close());
                    }
                    dispatcher = eventDispatcher;
                }
            }
            dispatcher.enqueue(event);
            return;
        }
        deliverEvent(event);
    }

    private void deliverEvent(SessionEvent event) {
        try {
            hookChain.fireEvent(new DefaultSessionEventContext(hookEnv, event));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "事件 hook 链分发失败（已隔离，继续 listener 分发）：sessionId=" + sessionId
                            + ", event=" + event.type(), e);
        }
        for (SessionEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "事件 listener 异常已隔离（不跳过其余 listener）：sessionId=" + sessionId
                                + ", event=" + event.type(), e);
            }
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Session already closed: " + sessionId);
        }
    }

    /**
     * impl-30 / spec 13 §core-1：停机期拒绝新 Turn——结构化
     * {@link BuzhouException}(SHUTDOWN_INTERRUPTED，RETRYABLE)：停机窗口结束后可重新发起。
     */
    private void ensureNotShuttingDown() {
        if (rejectingNewTurns) {
            throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                    "运行时停机中，拒绝新 Turn（sessionId=" + sessionId + "）");
        }
    }

    /** impl-30：运行时停机序列置位（{@code DefaultAgentRuntime} 调用；chat/stream 即刻拒绝）。 */
    void beginShutdown() {
        rejectingNewTurns = true;
    }

    /**
     * impl-33 / spec 13 §core-3：租约已丢失（本会话曾经历 LeaseLost 中止 / 后台续租发现被
     * steal）后的 chat/stream 调用即刻拒绝——既定错误通道为结构化
     * {@link LeaseLostException}（ErrorCode.LEASE_LOST，NON_RETRYABLE），不静默复活会话。
     */
    private void ensureLeaseHeld() {
        if (leaseGuard != null && leaseGuard.isLost()) {
            throw new LeaseLostException(sessionId);
        }
    }

    /** impl-33：Turn 收尾提交点 fence（history/快照写入前的最后校验）。 */
    private void verifyLeaseAtCommit() {
        if (leaseGuard != null) {
            leaseGuard.checkFence();
        }
    }

    /**
     * impl-33 / spec 13 §core-3：LeaseLost 中止语义——在飞工具结果已在顾问链轮缝被丢弃
     * （不入 history）、Turn 不入 Completed-Turn（不 markResponded、不 afterTurn、不
     * onTurnEnd，快照类写入不发生）；会话标记 leaseLost（后续调用明确拒绝）；以
     * {@code session.lease.lost} 事件可观测、{@code onTurnError} 收口在途 Turn span
     * （防泄漏，与 TIMEOUT 兜底路径同型）。
     */
    private void abortTurnAsLeaseLost(int turnSeq, LeaseLostException e) {
        if (leaseGuard != null) {
            leaseGuard.markLost();
        }
        dispatchEvent(SessionEvent.of("session.lease.lost", java.util.Map.of(
                "sessionId", sessionId,
                "turnSeq", turnSeq)));
        observers.forEach(o -> o.onTurnError(turnSeq, e));
    }
}
