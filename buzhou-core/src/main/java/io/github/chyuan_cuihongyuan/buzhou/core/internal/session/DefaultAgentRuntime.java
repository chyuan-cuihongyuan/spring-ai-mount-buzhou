package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.InterruptedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.LeaseHeartbeat;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoveryConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DefaultAgentRuntime implements AgentRuntime {

    private final ChatModel chatModel;
    private final BuzhouStores stores;
    private final HarnessAssembler assembler;
    private final ToolCallback[] tools;
    private final RuntimeConfig config;
    private final RecoveryConfig recoveryConfig;
    private final String ownerId = UUID.randomUUID().toString();

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
        AgentSession assembled = assembler.assemble(appId, agentName, sessionId, chatModel, effectiveStores, registry,
                registry::closeAll, config.hooks(), config.disabledHookNames(),
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
