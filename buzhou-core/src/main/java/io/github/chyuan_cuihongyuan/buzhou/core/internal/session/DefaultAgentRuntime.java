package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.InterruptedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.LeaseHeartbeat;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoveryConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.DrainResult;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeDrainingException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultAgentRuntime implements AgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(DefaultAgentRuntime.class);

    /** drain 总预算保守默认（编程式入口未给 timeout 时兜底；自装配层由 buzhou.shutdown.drain-timeout 派生）。 */
    private static final Duration DEFAULT_DRAIN_TIMEOUT = Duration.ofSeconds(30);

    private final ChatModel chatModel;
    private final BuzhouStores stores;
    private final HarnessAssembler assembler;
    private final ToolCallback[] tools;
    private final RuntimeConfig config;
    private final RecoveryConfig recoveryConfig;
    private final String ownerId = UUID.randomUUID().toString();

    /** 活跃会话台账：spawn 注册、close 注销（经既有 onClose 回调链挂上，不新增会话生命周期切面）。 */
    private final ConcurrentHashMap<String, AgentSession> liveSessions = new ConcurrentHashMap<>();
    /** drain 状态机：首次 drain 写入 future 并执行；后续调用等待同一 future（幂等）。null = 未 drain。 */
    private final AtomicReference<CompletableFuture<DrainResult>> drainFuture = new AtomicReference<>();
    /** spawn（读）与 drain 快照（写）互斥：drain 快照须看到所有已注册会话，不孤儿刚 assemble 完的会话。 */
    private final ReentrantReadWriteLock drainLock = new ReentrantReadWriteLock();

    public DefaultAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                               HarnessAssembler assembler, RuntimeConfig config,
                               ToolCallback... tools) {
        // 未显式给恢复配置时走规范默认（safe-by-default：心跳/幂等去重生效、VOID 不擅自续跑）
        this(chatModel, stores, assembler, config, RecoveryConfig.defaults(), tools);
    }

    public DefaultAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                               HarnessAssembler assembler, RuntimeConfig config,
                               RecoveryConfig recoveryConfig,
                               ToolCallback... tools) {
        this.chatModel = chatModel;
        this.stores = stores;
        this.assembler = assembler;
        this.config = config == null ? RuntimeConfig.defaults() : config;
        this.recoveryConfig = recoveryConfig == null ? RecoveryConfig.disabled() : recoveryConfig;
        this.tools = tools == null ? new ToolCallback[0] : tools.clone();
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
        // drain 已开始：拒新 spawn（拒绝即调用方路由信号——不排队、不缓冲）。读锁内判定，避免与 drain 快照竞争
        drainLock.readLock().lock();
        try {
            if (drainFuture.get() != null) {
                throw new RuntimeDrainingException(sessionId);
            }
            AgentSession assembled = doSpawn(appId, agentName, sessionId, options);
            liveSessions.put(sessionId, assembled);
            return assembled;
        } finally {
            drainLock.readLock().unlock();
        }
    }

    @Override
    public DrainResult drain(Duration timeout) {
        Duration budget = timeout == null ? DEFAULT_DRAIN_TIMEOUT : timeout;
        CompletableFuture<DrainResult> future = new CompletableFuture<>();
        List<AgentSession> snapshot;
        // 写锁内：CAS 设 future（幂等首生效）+ 快照活跃会话，确保快照看到所有已注册会话
        drainLock.writeLock().lock();
        try {
            CompletableFuture<DrainResult> existing = drainFuture.get();
            if (existing != null) {
                // 已有 drain 在进行：等待首次结果（幂等——重复/并发调用得到同一结果）
                return awaitExistingDrain(existing, budget);
            }
            drainFuture.set(future);
            snapshot = new ArrayList<>(liveSessions.values());
        } finally {
            drainLock.writeLock().unlock();
        }
        // 首次 drain：在锁外执行编排（close 可能阻塞，不持写锁）
        try {
            DrainResult result = executeDrain(snapshot, budget);
            future.complete(result);
            return result;
        } catch (RuntimeException e) {
            future.completeExceptionally(e);
            throw e;
        }
    }

    /** 等待已存在的 drain future 完成（幂等分支）：复用首次调用方的预算做有界等待。 */
    private DrainResult awaitExistingDrain(CompletableFuture<DrainResult> existing, Duration budget) {
        try {
            return existing.get(budget.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("drain did not complete within timeout: " + budget, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("drain interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("drain failed", cause);
        }
    }

    /**
     * drain 编排骨架（ticket 01）：fan-out drain.started → close 全部活跃会话 → fan-out drain.finished。
     *
     * <p>等完在途轮次（02）/ 超时强杀（03）在此方法内增量补入；本票只处理空载/仅空闲会话场景：
     * 直接走既有 {@code SessionResourceRegistry.closeAll()} 正常谢幕链（EXIT flush → 停心跳 → 关执行器 → 释租约）。
     * drain 事件经各在途会话的事件通道 fan-out（每会话均见 drain 全周期，便于观测管线按会话归因）。
     */
    private DrainResult executeDrain(List<AgentSession> snapshot, Duration budget) {
        Instant started = Instant.now();
        int activeCount = snapshot.size();
        SessionEvent startedEvent = new SessionEvent("drain.started",
                Map.of("activeCount", activeCount), Instant.now());
        snapshot.forEach(s -> emitIfDefault(s, startedEvent));

        // 01：无在途轮次判定 → 直接 close（02 增量：等完在途轮次；03 增量：超时强杀）
        int drainedCount = activeCount;
        int forceKilledCount = 0;

        Duration totalDuration = Duration.between(started, Instant.now());
        // drain.finished 须在 close 前 fan-out：close 会清空会话监听器
        SessionEvent finishedEvent = new SessionEvent("drain.finished",
                Map.of("drainedCount", drainedCount, "forceKilledCount", forceKilledCount,
                        "totalDurationMs", totalDuration.toMillis()),
                Instant.now());
        snapshot.forEach(s -> emitIfDefault(s, finishedEvent));

        // close 全部会话：单会话 close 异常不阻塞其他会话（首异常收集后汇总抛出）
        RuntimeException first = null;
        for (AgentSession s : snapshot) {
            try {
                s.close();
            } catch (RuntimeException e) {
                if (first == null) {
                    first = e;
                }
                log.warn("drain 期间关闭会话失败 sessionId={}", s.sessionId(), e);
            }
        }
        if (first != null) {
            throw first;
        }
        return new DrainResult(drainedCount, forceKilledCount, totalDuration);
    }

    /** 经会话既有事件通道发射 drain 事件（仅 DefaultAgentSession 暴露 emit；assembler 恒产出该类型）。 */
    private static void emitIfDefault(AgentSession session, SessionEvent event) {
        if (session instanceof DefaultAgentSession das) {
            das.emit(event);
        }
    }

    private AgentSession doSpawn(String appId, String agentName, String sessionId, SpawnOptions options) {
        java.time.Duration leaseTtl = recoveryConfig.leaseTtl();
        LeaseAcquireResult lease = stores.sessionLeaseStore().tryAcquire(sessionId, ownerId, leaseTtl);
        if (!lease.acquired()) {
            if (!options.steal()) {
                throw new SessionAlreadyActiveException(sessionId);
            }
            lease = stores.sessionLeaseStore().steal(sessionId, ownerId, leaseTtl);
        }
        SessionResourceRegistry registry = new SessionResourceRegistry();
        long fencingToken = lease.fencingToken();
        registry.register("session-lease",
                () -> stores.sessionLeaseStore().release(sessionId, ownerId, fencingToken));
        java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        registry.register("session-executor", executor::shutdownNow);

        // 崩溃恢复机制：轮次执行期租约心跳（长轮次不被误判崩溃）+ 装配后按恢复语义档位处理中断轮次
        boolean recoveryEnabled = recoveryConfig.enabled();
        boolean interrupted = false;
        if (recoveryEnabled) {
            interrupted = InterruptedTurnDetector.wasInterrupted(stores.messageStore().load(sessionId));
        }
        // 持久化强度三档（spec「持久化强度三档」）：存储写路径按档位包装（编排方不分支）；
        // EXIT 档 flush 钩子随会话谢幕触发（06 优雅停机 drain 亦经此钩子联动）
        BuzhouStores effectiveStores = recoveryEnabled
                ? io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DurabilityTieredStores
                        .wrap(stores, recoveryConfig.durabilityTier())
                : stores;
        if (recoveryEnabled) {
            registry.register("durability-flush",
                    () -> io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DurabilityTieredStores
                            .flush(effectiveStores));
        }
        config.sessionCustomizers().forEach(c -> c.customize(registry, appId, agentName, sessionId));
        ToolCallback[] allTools = java.util.stream.Stream.concat(
                        java.util.Arrays.stream(tools),
                        config.autoTools().stream())
                .toArray(ToolCallback[]::new);
        // @BuzhouTool.idempotent 收集从「仅原子工具」扩到全部工具（spec「幂等三件套 ① 声明」）：
        // 与 ToolsModule 既有通道并集，副作用工具默认非幂等（未声明 idempotent=true 即不重放）
        java.util.Set<String> idempotentToolNames = new java.util.HashSet<>(config.idempotentToolNames());
        for (ToolCallback tool : allTools) {
            io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool meta =
                    tool.getClass().getAnnotation(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool.class);
            if (meta != null && meta.idempotent()) {
                idempotentToolNames.add(meta.name());
            }
        }
        // onClose 回调链：既有 registry.closeAll（谢幕资源 LIFO close）+ drain 台账注销。
        // 经既有 onClose 回调链挂上，不新增会话生命周期切面；try/finally 保证台账注销不因 closeAll 失败遗漏。
        Runnable onClose = () -> {
            try {
                registry.closeAll();
            } finally {
                liveSessions.remove(sessionId);
            }
        };
        AgentSession assembled = assembler.assemble(appId, agentName, sessionId, chatModel, effectiveStores, registry,
                onClose, config.hooks(), config.disabledHookNames(),
                idempotentToolNames, config.viewProcessor(), executor,
                config.serialGroups(), recoveryConfig, config.assemblyCustomizers(), allTools);
        options.listeners().forEach(assembled::addEventListener);

        if (recoveryEnabled && assembled instanceof DefaultAgentSession session) {
            // 生效档位进 observability（SRE 审计每个会话的一致性契约）
            session.emit(new SessionEvent("durability-tier",
                    Map.of("tier", recoveryConfig.durabilityTier().name(), "sessionId", sessionId),
                    Instant.now()));
            LeaseHeartbeat heartbeat = new LeaseHeartbeat(stores.sessionLeaseStore(), sessionId, ownerId,
                    fencingToken, leaseTtl, recoveryConfig.heartbeatInterval(), lost -> session.markLeaseLost());
            registry.register("lease-heartbeat", heartbeat::close);
            heartbeat.start();
            emitRecoveryEvent(session, sessionId, interrupted, effectiveResumeStrategy(options));
        }
        return assembled;
    }

    /**
     * 恢复语义分档（spec「崩溃中轮次恢复」）：加载历史后对中断轮次按档位处置并事件化。
     * VOID（默认）不擅自续跑、等用户下一次输入；AUTO_RESUME 无需用户输入续跑被中断轮次，
     * 崩溃循环由硬顶次数兜底（03/04 熔断就绪前的保守闸门）。
     */
    private void emitRecoveryEvent(DefaultAgentSession session, String sessionId, boolean interrupted,
                                   io.github.chyuan_cuihongyuan.buzhou.core.recovery.ResumeStrategy strategy) {
        if (!interrupted) {
            return;
        }
        switch (strategy) {
            case VOID -> session.emit(new SessionEvent("turn-recovered",
                    Map.of("action", "voided", "sessionId", sessionId), Instant.now()));
            case AUTO_RESUME -> autoResume(session, sessionId);
        }
    }

    /** 恢复语义档位生效值：会话级 SpawnOptions 覆盖优先，否则走运行时配置（spec「改动面」）。 */
    private io.github.chyuan_cuihongyuan.buzhou.core.recovery.ResumeStrategy effectiveResumeStrategy(
            SpawnOptions options) {
        return options.resumeStrategy() != null ? options.resumeStrategy() : recoveryConfig.resumeStrategy();
    }

    /** AUTO_RESUME：硬顶内自动续跑被中断轮次；反复崩溃触顶则掐断并事件化（不再自发调用模型）。 */
    private void autoResume(DefaultAgentSession session, String sessionId) {
        int attempts = readResumeAttempts(sessionId);
        if (attempts >= recoveryConfig.crashloopHardCap()) {
            session.emit(new SessionEvent("resume-skipped-crashloop",
                    Map.of("sessionId", sessionId, "attempts", attempts,
                            "hardCap", recoveryConfig.crashloopHardCap()), Instant.now()));
            return;
        }
        writeResumeAttempts(sessionId, attempts + 1);
        String reply = session.resumeInterruptedTurn();
        // 续跑成功完结：重置崩溃循环计数（硬顶只掐「连续崩溃—续跑」循环，不误伤后续正常崩溃恢复）
        stores.sessionStateStore().delete(sessionId, RESUME_ATTEMPTS_KEY);
        session.emit(new SessionEvent("turn-recovered",
                Map.of("action", "auto-resumed", "sessionId", sessionId,
                        "reply", reply == null ? "" : reply), Instant.now()));
    }

    /** 崩溃循环计数键（per-session state，跨崩溃实例累积——硬顶因此能兜住反复崩溃）。 */
    private static final String RESUME_ATTEMPTS_KEY = "recovery.autoresume.attempts";
    /** 计数记录生产者标记。 */
    private static final String RESUME_COUNTER_PRODUCER = "recovery";
    /** 计数记录不归属具体轮次（跨崩溃实例累积），createdTurn 统一占位值。 */
    private static final int RESUME_COUNTER_TURN = 0;

    private int readResumeAttempts(String sessionId) {
        return stores.sessionStateStore().get(sessionId, RESUME_ATTEMPTS_KEY)
                .map(e -> {
                    try {
                        return Integer.parseInt(e.value());
                    } catch (NumberFormatException nfe) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    private void writeResumeAttempts(String sessionId, int attempts) {
        stores.sessionStateStore().put(sessionId,
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                        RESUME_ATTEMPTS_KEY, String.valueOf(attempts), RESUME_COUNTER_PRODUCER,
                        RESUME_COUNTER_TURN, null, Instant.now()));
    }
}
