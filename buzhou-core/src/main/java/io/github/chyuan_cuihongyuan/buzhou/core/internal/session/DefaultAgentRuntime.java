package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultAgentRuntime implements AgentRuntime, AutoCloseable {

    private static final Duration LEASE_TTL = Duration.ofSeconds(90);
    /** impl-33：续租间隔下限（防误配成 0/负数导致调度线程忙转）。 */
    private static final Duration MIN_RENEW_INTERVAL = Duration.ofMillis(50);
    /** impl-30 / spec 13 §core-1：停机排空预算默认值（未显式传入时）。 */
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);
    /** impl-30：排空等待的轮询间隔（50ms 粒度，兼顾响应性与 CPU 空转）。 */
    private static final long DRAIN_POLL_MILLIS = 50L;
    private static final System.Logger LOGGER =
            System.getLogger(DefaultAgentRuntime.class.getName());

    private final ChatModel chatModel;
    private final BuzhouStores stores;
    private final HarnessAssembler assembler;
    private final ToolCallback[] tools;
    private final RuntimeConfig config;
    private final String ownerId = UUID.randomUUID().toString();

    /** impl-33 / spec 13 §core-3：租约参数（TTL 默认 90s；续租间隔默认 TTL/3）。 */
    private final Duration leaseTtl;
    private final Duration leaseRenewInterval;

    /** impl-30 / spec 13 §core-1：停机排空预算（{@code buzhou.lifecycle.timeout-per-shutdown-phase}）。 */
    private final Duration shutdownTimeout;

    /** impl-33：活跃会话的租约哨兵（后台续租遍历对象；close 注销防泄漏）。 */
    private final ConcurrentHashMap<String, SessionLeaseGuard> activeLeases = new ConcurrentHashMap<>();
    private final Object renewLoopLock = new Object();
    private volatile Thread renewLoop;

    /**
     * impl-30 / spec 13 §core-1：spawn 的活跃会话注册表（在途 Turn 计数 + executor 引用）。
     * 会话 close 时注销（{@code onClose} 钩子 finally），防泄漏；停机排空遍历此表。
     */
    private final ConcurrentHashMap<String, TrackedSession> activeSessions = new ConcurrentHashMap<>();

    /** impl-30：停机状态机（NOT_STARTED → DRAINING → COMPLETED）；幂等保证 stop/destroy 双触发无害。 */
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();
    private final AtomicBoolean shutdownCompleted = new AtomicBoolean();
    private volatile boolean shuttingDown;

    public DefaultAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                               HarnessAssembler assembler, RuntimeConfig config,
                               ToolCallback... tools) {
        this(chatModel, stores, assembler, config, null, null, null, tools);
    }

    /**
     * impl-33：租约参数可配入口（Spring 装配经 {@code BuzhouCoreProperties} 流入；
     * 编程式入口经 {@code Buzhou.runtime(...)} 重载）。
     *
     * @param leaseTtl           null/非正 → 默认 90s
     * @param leaseRenewInterval null → TTL/3；下限 {@link #MIN_RENEW_INTERVAL}
     */
    public DefaultAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                               HarnessAssembler assembler, RuntimeConfig config,
                               Duration leaseTtl, Duration leaseRenewInterval,
                               ToolCallback... tools) {
        this(chatModel, stores, assembler, config, leaseTtl, leaseRenewInterval, null, tools);
    }

    /**
     * impl-30 / spec 13 §core-1：租约 + 停机参数完整入口。
     *
     * @param shutdownTimeout 停机排空预算；null/非正 → 默认 30s
     *                         （{@code buzhou.lifecycle.timeout-per-shutdown-phase}）
     */
    public DefaultAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                               HarnessAssembler assembler, RuntimeConfig config,
                               Duration leaseTtl, Duration leaseRenewInterval,
                               Duration shutdownTimeout,
                               ToolCallback... tools) {
        this.chatModel = chatModel;
        this.stores = stores;
        this.assembler = assembler;
        this.config = config == null ? RuntimeConfig.defaults() : config;
        this.tools = tools == null ? new ToolCallback[0] : tools.clone();
        this.leaseTtl = leaseTtl == null || leaseTtl.isZero() || leaseTtl.isNegative()
                ? LEASE_TTL : leaseTtl;
        this.leaseRenewInterval = resolveRenewInterval(leaseRenewInterval, this.leaseTtl);
        this.shutdownTimeout = shutdownTimeout == null
                || shutdownTimeout.isZero() || shutdownTimeout.isNegative()
                ? DEFAULT_SHUTDOWN_TIMEOUT : shutdownTimeout;
    }

    private static Duration resolveRenewInterval(Duration configured, Duration ttl) {
        Duration effective = configured == null || configured.isZero() || configured.isNegative()
                ? ttl.dividedBy(3) : configured;
        return effective.compareTo(MIN_RENEW_INTERVAL) < 0 ? MIN_RENEW_INTERVAL : effective;
    }

    @Override
    public AgentSession spawn(String appId, String agentName) {
        return spawn(appId, agentName, UUID.randomUUID().toString(), SpawnOptions.defaults());
    }

    @Override
    public AgentSession spawn(String appId, String agentName, String sessionId) {
        return spawn(appId, agentName, sessionId, SpawnOptions.defaults());
    }

    @Override
    public AgentSession spawn(String appId, String agentName, String sessionId, SpawnOptions options) {
        // impl-30 / spec 13 §core-1：停机期拒绝新会话（结构化 SHUTDOWN_INTERRUPTED，
        // RETRYABLE——停机窗口结束后可重新发起）
        if (shuttingDown) {
            throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                    "AgentRuntime 已停机，拒绝创建新会话（sessionId=" + sessionId + "）");
        }
        LeaseAcquireResult lease = stores.sessionLeaseStore().tryAcquire(sessionId, ownerId, leaseTtl);
        if (!lease.acquired()) {
            if (!options.steal()) {
                throw new SessionAlreadyActiveException(sessionId);
            }
            lease = stores.sessionLeaseStore().steal(sessionId, ownerId, leaseTtl);
        }
        SessionResourceRegistry registry = new SessionResourceRegistry();
        long fencingToken = lease.fencingToken();
        // impl-33 / spec 13 §core-3：租约哨兵——轮间续租/fence（Advisor 轮缝）+ 后台 TTL/3 续租
        SessionLeaseGuard leaseGuard = new SessionLeaseGuard(
                stores.sessionLeaseStore(), sessionId, ownerId, fencingToken,
                leaseTtl, leaseTtl.dividedBy(3));
        activeLeases.put(sessionId, leaseGuard);
        registry.register("session-lease",
                () -> stores.sessionLeaseStore().release(sessionId, ownerId, fencingToken));
        ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        // impl-30 / spec 13 §core-1：executor 优雅关闭——shutdown() + awaitTermination(预算)，
        // 到点 shutdownNow（既有行为是直接 shutdownNow；改优雅排空防在飞工具任务被硬丢弃）
        registry.register("session-executor", () -> closeExecutorGracefully(executor));
        // impl-33：close 注销续租（LIFO → 先于 lease 释放执行，不再续租即将释放的租约，防泄漏）
        registry.register("lease-renew", () -> activeLeases.remove(sessionId));
        ensureRenewLoopRunning();
        config.sessionCustomizers().forEach(c -> c.customize(registry, appId, agentName, sessionId));
        ToolCallback[] allTools = java.util.stream.Stream.concat(
                        java.util.Arrays.stream(tools),
                        config.autoTools().stream())
                .toArray(ToolCallback[]::new);
        // impl-30：停机排空的在途 Turn 追踪视图（会话内权威计数 + executor 硬截断引用）
        TrackedSession tracked = new TrackedSession(executor);
        AgentSession session = assembler.assemble(appId, agentName, sessionId, chatModel, stores, registry,
                () -> {
                    try {
                        registry.closeAll();
                    } finally {
                        // impl-30：会话资源收尾后从活跃注册表注销（无论收尾成败，防泄漏）
                        tracked.markClosed();
                        activeSessions.remove(sessionId);
                    }
                },
                config.hooks(), config.disabledHookNames(),
                config.idempotentToolNames(), config.viewProcessor(), executor,
                config.serialGroups(), config.assemblyCustomizers(), config.turnLoopPolicy(),
                leaseGuard, allTools);
        if (session instanceof DefaultAgentSession defaultSession) {
            tracked.bind(defaultSession);
        }
        activeSessions.put(sessionId, tracked);
        options.listeners().forEach(session::addEventListener);
        // 注册后可能已并发进入停机——即刻补发拒新标记，保证「拒绝新 Turn」无窗口遗漏
        if (shuttingDown) {
            tracked.rejectNewTurns();
        }
        return session;
    }

    /**
     * impl-30 / spec 13 §core-1：优雅停机序列（幂等；由 core SmartLifecycle stop 与
     * destroy 兜底路径共同复用）：
     * <ol>
     *   <li><b>拒绝新 Turn</b>：置 shuttingDown（spawn 拒绝）+ 对全部活跃会话置
     *       拒新标记（chat/stream 即刻拒绝，SHUTDOWN_INTERRUPTED）；</li>
     *   <li><b>取消在途</b>：逐会话发 {@link CancelMode#AFTER_CURRENT_TURN}——当前工具批
     *       完成后本轮自然收尾（不打断）；</li>
     *   <li><b>排空等待</b>：轮询等待全部在途 Turn 计数归零（预算内）；</li>
     *   <li><b>超时硬截断</b>：到点对仍在途的会话发 {@link CancelMode#IMMEDIATE}（中断在飞
     *       工具）+ executor {@code shutdownNow()}；</li>
     *   <li><b>收尾</b>：关闭全部活跃会话（释放租约、清空资源注册表）、停续租守护线程。</li>
     * </ol>
     *
     * @param timeout 排空预算（null → 构造时配置的 {@code shutdownTimeout}）
     * @return true = 预算内排空完成；false = 超时硬截断
     */
    public boolean shutdownGracefully(Duration timeout) {
        Duration budget = timeout == null || timeout.isZero() || timeout.isNegative()
                ? shutdownTimeout : timeout;
        if (!shutdownStarted.compareAndSet(false, true)) {
            return shutdownCompleted.get(); // 已在停机中/已完成：幂等返回
        }
        // ① 拒绝新 Turn（spawn + 既有会话的 chat/stream）
        shuttingDown = true;
        for (TrackedSession tracked : activeSessions.values()) {
            tracked.rejectNewTurns();
        }
        // ② 在途会话发 AFTER_CURRENT_TURN（当前工具批完成后本轮自然收尾）
        for (TrackedSession tracked : activeSessions.values()) {
            tracked.cancelQuietly(CancelMode.AFTER_CURRENT_TURN);
        }
        // ③ 排空等待（预算内轮询全部在途 Turn 结束）
        long deadlineNanos = System.nanoTime() + budget.toNanos();
        boolean drained = false;
        while (!activeSessions.values().stream().allMatch(TrackedSession::isSettled)) {
            if (System.nanoTime() >= deadlineNanos) {
                break;
            }
            sleepQuietly(DRAIN_POLL_MILLIS);
        }
        drained = activeSessions.values().stream().allMatch(TrackedSession::isSettled);
        // ④ 超时硬截断：仍在途的会话发 IMMEDIATE 取消 + executor shutdownNow
        if (!drained) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "停机排空超时（{0}ms），对在途会话硬截断（IMMEDIATE 取消 + executor shutdownNow）",
                    budget.toMillis());
            for (TrackedSession tracked : activeSessions.values()) {
                if (!tracked.isSettled()) {
                    tracked.cancelQuietly(CancelMode.IMMEDIATE);
                    tracked.shutdownNowExecutor();
                }
            }
            // 截断后再给一个同等预算的收尾宽限：被中断的 Turn 通常毫秒级收口（取消反馈 →
            // 最终回复）；宽限后再关会话（租约释放），避免 release 与在途 Turn 的提交点
            // fence 赛跑产生 LeaseLost 假象。不可中断的挂死任务最多再等一个预算——
            // 停机总体上界 2× 预算。
            long graceDeadlineNanos = System.nanoTime() + budget.toNanos();
            while (!activeSessions.values().stream().allMatch(TrackedSession::isSettled)) {
                if (System.nanoTime() >= graceDeadlineNanos) {
                    break;
                }
                sleepQuietly(DRAIN_POLL_MILLIS);
            }
        }
        // ⑤ 收尾：关闭全部活跃会话（租约释放/资源注册表清空，逐会话隔离失败）+ 停续租守护
        closeAllTrackedSessions();
        stopRenewLoop();
        shutdownCompleted.set(true);
        return drained;
    }

    /** impl-30：停机兜底（容器不调 stop 直接 destroy；幂等——stop 已完成则 no-op）。 */
    @Override
    public void close() {
        if (shutdownStarted.get()) {
            return; // stop 已接管（优雅或硬截断序列均已完成）
        }
        // destroy 未经 stop：跳过排空等待，直接硬截断收尾（尽力而为、不阻塞销毁线程）
        shutdownGracefully(Duration.ofMillis(DRAIN_POLL_MILLIS));
    }

    /** impl-30：是否已进入停机窗口（spawn/chat 拒绝中）。测试与诊断可观测。 */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /** impl-30：当前活跃（未 close）会话数。测试与诊断可观测。 */
    public int activeSessionCount() {
        return activeSessions.size();
    }

    /** impl-30：关闭全部被追踪会话（逐会话 try/catch——单个失败不跳过其余清理）。 */
    private void closeAllTrackedSessions() {
        for (TrackedSession tracked : activeSessions.values()) {
            AgentSession session = tracked.session();
            if (session == null) {
                continue;
            }
            try {
                session.close();
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "停机关闭会话失败（继续关闭其余会话）", e);
            }
        }
    }

    /** impl-30：executor 优雅关闭——shutdown() + awaitTermination(预算)，到点/中断则 shutdownNow。 */
    private void closeExecutorGracefully(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * impl-33 / spec 13 §core-3：后台续租调度——单守护<b>虚拟线程</b>（命名
     * {@code buzhou-lease-renew}，可中断 sleep），按续租间隔对全部活跃会话
     * {@link SessionLeaseGuard#renewQuietly()} 无条件续租（剩余租期恒 ≥ 2/3 TTL）；
     * 懒启动（首个 spawn 才起）、空闲自熄（全部会话 close 后退出、下次 spawn 重启）。
     *
     * <p>说明：不用 {@code ScheduledExecutorService}——平台线程池与「虚拟线程工厂」诉求相悖，
     * {@code SimpleAsyncTaskScheduler} 空闲仍占调度线程；自管虚拟线程 + 可中断 sleep 等价于
     * 固定周期调度（TTL/3 节奏），且生命周期随活跃会话自归零。续租失败仅置 lost 标记
     * （在途 Turn 由轮间/提交点 fence 兜底中止，不在此抛出）。
     */
    private void ensureRenewLoopRunning() {
        if (renewLoop != null) {
            return;
        }
        synchronized (renewLoopLock) {
            if (renewLoop != null) {
                return;
            }
            Thread loop = Thread.ofVirtual().name("buzhou-lease-renew").unstarted(() -> {
                while (true) {
                    try {
                        Thread.sleep(leaseRenewInterval.toMillis());
                    } catch (InterruptedException e) {
                        return; // 线程被清理（停机切片 stopRenewLoop / 空闲自熄）
                    }
                    for (SessionLeaseGuard guard : activeLeases.values()) {
                        guard.renewQuietly();
                    }
                    synchronized (renewLoopLock) {
                        if (activeLeases.isEmpty()) {
                            renewLoop = null;
                            return; // 空闲自熄：无活跃会话不再占用线程
                        }
                    }
                }
            });
            renewLoop = loop;
            loop.start();
        }
    }

    /** impl-30：停机时停续租守护线程（会话全 close 后亦会自熄；此处中断使其即刻退出）。 */
    private void stopRenewLoop() {
        Thread loop = renewLoop;
        if (loop != null) {
            loop.interrupt();
            renewLoop = null;
        }
    }

    /**
     * impl-30 / spec 13 §core-1：单会话的停机追踪视图——在途 Turn 计数（{@link DefaultAgentSession}
     * 内的权威计数，覆盖 chat/stream 全部终结路径）+ executor 引用（硬截断用）+ 会话引用
     * （收尾 close / 拒新标记用）。会话 close 后视为已安定（closed 标记兜底「流式订阅从未
     * 发生终结信号」的计数残留路径）。
     */
    private static final class TrackedSession {

        private final ExecutorService executor;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile DefaultAgentSession session;

        TrackedSession(ExecutorService executor) {
            this.executor = executor;
        }

        void bind(DefaultAgentSession session) {
            this.session = session;
        }

        DefaultAgentSession session() {
            return session;
        }

        void markClosed() {
            closed.set(true);
        }

        /** 已安定 = 会话已 close，或无在途 Turn（排空完成条件）。 */
        boolean isSettled() {
            DefaultAgentSession s = session;
            return closed.get() || s == null || s.inFlightTurns() == 0;
        }

        /** 停机拒新：会话层 chat/stream 即刻拒绝（SHUTDOWN_INTERRUPTED）。 */
        void rejectNewTurns() {
            DefaultAgentSession s = session;
            if (s != null) {
                s.beginShutdown();
            }
        }

        /** 停机取消（逐会话隔离失败——单个会话取消异常不阻断其余会话的停机序列）。 */
        void cancelQuietly(CancelMode mode) {
            DefaultAgentSession s = session;
            if (s == null || closed.get()) {
                return;
            }
            try {
                s.cancel(mode);
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.WARNING, "停机取消会话在途 Turn 失败（mode={0}）",
                        mode);
            }
        }

        void shutdownNowExecutor() {
            executor.shutdownNow();
        }
    }
}
