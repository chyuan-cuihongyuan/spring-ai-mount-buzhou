package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.AgentRuntimeLifecycle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.HarnessAssembler;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 内核自装配（spec 09 / ticket 22）。
 *
 * <p>两件套：
 * <ol>
 *   <li>按 {@code buzhou.store.type}（默认 {@code memory}）装配 {@link BuzhouStores}；
 *       jdbc/redis 实现由各自模块按 store.type 条件装配，本类只提供内存默认。</li>
 *   <li>收集容器内全部 {@link RuntimeConfig}（机制模块产出）与扩展组件 bean
 *       （{@link BuzhouHook} / {@link ToolCallback} / {@link SessionAssemblyCustomizer} /
 *       {@link SessionResourceCustomizer} / {@link MemoryViewProcessor}，供用户自定义扩展），
 *       经 {@link RuntimeConfig#merge} 合成单一 {@link AgentRuntime}（依赖 {@link ChatModel}）。</li>
 * </ol>
 *
 * <p>合并后的 {@link RuntimeConfig} 是 {@link #buzhouAgentRuntime} 方法内的局部变量，
 * <b>不</b>暴露为 bean，避免被 {@code List<RuntimeConfig>} 自收集（无环）。
 */
@AutoConfiguration
@EnableConfigurationProperties({BuzhouCoreProperties.class, BuzhouRetentionProperties.class,
        BuzhouRunawayProperties.class, BuzhouBackpressureProperties.class,
        BuzhouTokenBudgetProperties.class,
        io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties.class,
        BuzhouToolsProperties.class})
public class BuzhouCoreAutoConfiguration {

    /**
     * 事件外发 webhook（spec 20 / T89；outbox 持久化 spec 24 / T103 / impl-78）：配置
     * {@code buzhou.webhook.url} 才装配（默认关、零开销）。事件经持久化 outbox 投递
     * （stateStore 合成会话，重启恢复）；forwarder 经全局监听挂点挂全部会话（见 buzhouAgentRuntime）。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "buzhou.webhook", name = "url")
    public io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder webhookEventForwarder(
            io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties props,
            org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores> storesProvider) {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores = storesProvider.getIfAvailable();
        return new io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder(props,
                stores != null ? stores.sessionStateStore()
                        : new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore());
    }

    /**
     * 工具结果限幅器全局默认（spec 31 / T110 / impl-85）：启动期据配置设定 Holder；
     * 会话装配时 toolManager 从 Holder 取初值（可经 toolManager() per-session 覆盖）。
     */
    @Bean
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiter buzhouToolResultLimiter(
            BuzhouToolsProperties props) {
        java.util.Map<String, Integer> overrides = new java.util.LinkedHashMap<>();
        overrides.put("read_range", -1); // spill 自治理豁免（默认档）
        if (props.resultLimitOverrides() != null) {
            overrides.putAll(props.resultLimitOverrides());
        }
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiter limiter =
                new io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiter(
                        props.resultLimitChars(), overrides);
        io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultLimiterHolder.set(limiter);
        return limiter;
    }

    // ---- 失控检测与容量闸（impl-45 / spec 14 §A，自分支增量移植）----

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters runawayCounters() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters();
    }

    /**
     * 失控检测 Hook（{@code buzhou.runaway.enabled} 默认开；阈值默认 null = 不限，
     * safe-by-default）。注入 {@link BuzhouStores} 的 observabilityStore 使
     * {@code runaway.*} 事件双重写入（SessionEvent + EventRecord），dashboard 可查。
     * store 经 {@code ObjectProvider} 惰性取用——store.type 校验失败路径上无 store bean 时
     * 不抢跑（该路径由 buzhouStoreTypeGuard 以 BuzhouConfigurationException 失败）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "runawayHook")
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook runawayHook(
            BuzhouRunawayProperties runawayProperties,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters> counters,
            ObjectProvider<BuzhouStores> stores) {
        BuzhouStores available = stores.getIfAvailable();
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayHook(
                runawayProperties, counters.getIfAvailable(
                        io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters::new),
                available == null ? null : available.observabilityStore());
    }

    /**
     * Token/成本预算 Hook（spec 16 / T83 / impl-58；{@code buzhou.token-budget.enabled} 默认开、
     * 阈值 null = 不限，safe-by-default）。模型名回退键取 {@code buzhou.model-name}（默认 unknown，
     * 与 resilience/observability 同口径）；{@code budget.*} 事件双写（SessionEvent + EventRecord）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "tokenBudgetHook")
    @ConditionalOnProperty(prefix = "buzhou.token-budget", name = "enabled", matchIfMissing = true)
    public BuzhouHook tokenBudgetHook(
            BuzhouTokenBudgetProperties tokenBudgetProperties,
            ObjectProvider<BuzhouStores> stores,
            org.springframework.core.env.Environment env) {
        BuzhouStores available = stores.getIfAvailable();
        return new io.github.chyuan_cuihongyuan.buzhou.core.budget.TokenBudgetHook(
                tokenBudgetProperties, env.getProperty("buzhou.model-name", "unknown"),
                available == null ? null : available.observabilityStore());
    }

    /**
     * 软退出提醒渲染器（达软阈值时经既有 Attachment 通道注入「剩余步数预算」信号）。
     * 被 {@code BuzhouMemoryAutoConfiguration} 自动组合进 CompositeAttachmentRenderer。
     * 无 {@code per-turn.max-steps} 时不注入（合法长任务不受影响）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "runawayBudgetRenderer")
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer runawayBudgetRenderer(
            BuzhouRunawayProperties runawayProperties,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters> counters) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayBudgetRenderer(
                runawayProperties, counters.getIfAvailable(
                        io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters::new));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "memory", matchIfMissing = true)
    public BuzhouStores buzhouStores(BuzhouCoreProperties properties) {
        // impl-36 / spec 13 §growth-8：buzhou.store.in-memory.* 容量配额流入内存套件
        return Buzhou.inMemoryStores(properties.store().inMemory().toConfig());
    }

    /**
     * impl-42 / spec 13 §T68：{@code buzhou.store.type} 封闭枚举 fail-fast——拼错值
     * （如 {@code jbdc}）此前会静默落进「无任何 store 装配」的深水区运行时失败；现在
     * 启动即失败并给出可选值与已装模块指引（经 {@link BuzhouStoreFailureAnalyzer} 翻译）。
     * 依赖顺序：本 bean 须在无 store 可用时不抢跑——用 {@code @ConditionalOnMissingBean}
     * + 显式类型校验双管（memory 默认路径自证；jdbc/redis 模块在场时由其 store bean 存在）。
     */
    @Bean
    public Object buzhouStoreTypeGuard(org.springframework.core.env.Environment env,
            ObjectProvider<BuzhouStores> stores) {
        String type = env.getProperty("buzhou.store.type", "memory").trim().toLowerCase();
        if (!java.util.Set.of("memory", "jdbc", "redis").contains(type)) {
            throw new BuzhouConfigurationException(
                    "buzhou.store.type=\"" + type + "\" 不是有效存储形态",
                    "修正为 memory（默认，进程内）/ jdbc（MySQL/PostgreSQL/H2，需 buzhou-store-jdbc）"
                            + "/ redis（需 buzhou-store-redis）之一；注意大小写与拼写",
                    null);
        }
        if (stores.getIfAvailable() == null) {
            throw new BuzhouConfigurationException(
                    "buzhou.store.type=" + type + " 但对应 store 实现未装配",
                    type.equals("jdbc") ? "引入 buzhou-store-jdbc 依赖（并确认 DataSource bean 存在）"
                            : type.equals("redis") ? "引入 buzhou-store-redis 依赖（并确认 uri 配置）"
                            : "检查 buzhou.store.* 配置",
                    null);
        }
        return new Object();
    }

    /**
     * impl-41 / spec 13 §T66：泄漏检测器（{@code buzhou.leak.level}=
     * DISABLED|SIMPLE|ADVANCED|PARANOID，默认 SIMPLE 1/128 采样；
     * {@code buzhou.leak.lease-age-threshold} 默认 PT5M；LeakListener bean 可注入）。
     * 安装即全局生效（会话/租约/spill 句柄三挂点经 LeakDetectorHolder 取用）。
     */
    @Bean
    @ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector resourceLeakDetector(
            org.springframework.core.env.Environment env,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakListener> listener) {
        String levelText = env.getProperty("buzhou.leak.level",
                io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakLevel.SIMPLE.name());
        io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakLevel level;
        try {
            level = io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector.LeakLevel
                    .valueOf(levelText.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("buzhou.leak.level 非法：\"" + levelText
                    + "\"（须 DISABLED/SIMPLE/ADVANCED/PARANOID）", e);
        }
        java.time.Duration threshold = java.time.Duration.parse(env.getProperty(
                "buzhou.leak.lease-age-threshold", "PT5M"));
        io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector detector =
                new io.github.chyuan_cuihongyuan.buzhou.core.leak.ResourceLeakDetector(
                        level, threshold, listener.getIfAvailable());
        io.github.chyuan_cuihongyuan.buzhou.core.leak.LeakDetectorHolder.install(detector);
        return detector;
    }

    /**
     * impl-41 / spec 13 §T66：有 micrometer（MeterRegistry bean）时——安装
     * MicrometerBuzhouMetrics 到全局 holder + 预注册标准指标集。未装 micrometer：
     * holder 保持 no-op（零开销）。
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
            io.micrometer.core.instrument.MeterRegistry.class)
    static class BuzhouMetricsConfiguration {

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
                io.micrometer.core.instrument.MeterRegistry.class)
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsBinder buzhouMetricsBinder(
                io.micrometer.core.instrument.MeterRegistry registry) {
            return new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsBinder();
        }

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
                io.micrometer.core.instrument.MeterRegistry.class)
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolderInstaller
                buzhouMetricsHolderInstaller(io.micrometer.core.instrument.MeterRegistry registry) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(
                    new io.github.chyuan_cuihongyuan.buzhou.core.metrics.MicrometerBuzhouMetrics(
                            registry));
            return new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolderInstaller();
        }
    }

    /**
     * impl-41 / spec 13 §T66：只读快照端点 {@code /actuator/buzhou}（有 actuator 才装配；
     * 聚合全部 BuzhouHealth 机制贡献）。
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
            name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    static class BuzhouEndpointConfiguration {

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthEndpoint buzhouHealthEndpoint(
                ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth> contributors) {
            return new io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthEndpoint(
                    contributors.orderedStream().toList());
        }
    }

    /**
     * impl-30 / spec 13 §core-1：显式 {@code destroyMethod = "close"}——不靠推断，且与
     * {@link AgentRuntimeLifecycle#stop} 的双触发由 runtime 停机状态机（幂等）吸收：
     * stop 已跑完则 close 为 no-op；容器未调 stop 直接 destroy 时 close 兜底硬截断收尾。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnBean(ChatModel.class)
    @ConditionalOnMissingBean(AgentRuntime.class)
    public DefaultAgentRuntime buzhouAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                                           BuzhouCoreProperties properties,
                                           BuzhouBackpressureProperties backpressureProperties,
                                           List<RuntimeConfig> moduleConfigs,
                                           List<BuzhouHook> hooks,
                                           List<ToolCallback> autoTools,
                                           List<SessionAssemblyCustomizer> assemblyCustomizers,
                                           List<SessionResourceCustomizer> resourceCustomizers,
                                           ObjectProvider<MemoryViewProcessor> viewProcessor,
                                           ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener>
                                                   globalEventListeners,
                                           org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore>
                                                   indexStoreProvider,
                                           org.springframework.beans.factory.ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExportExtension>
                                                   exportExtensionsProvider) {
        List<RuntimeConfig> all = new ArrayList<>(moduleConfigs);
        // 用户自定义扩展 bean（按组件类型包成单维度 RC 后并入 merge；模块产出已在 moduleConfigs 内）
        if (!hooks.isEmpty()) {
            all.add(RuntimeConfig.hooks(hooks));
        }
        if (!autoTools.isEmpty()) {
            all.add(RuntimeConfig.autoTools(autoTools));
        }
        if (!assemblyCustomizers.isEmpty()) {
            all.add(RuntimeConfig.assemblyCustomizers(assemblyCustomizers));
        }
        // spec 30 / T109 / impl-84：会话索引接线（store 模块提供 SessionIndexStore bean 才启用；
        // 无 bean = 无枚举能力，会话功能零影响——最终一致索引）
        io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore indexStore =
                indexStoreProvider.getIfAvailable();
        if (indexStore != null) {
            all.add(RuntimeConfig.assemblyCustomizers(java.util.List.of(
                    io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionIndexObserver
                            .wiring(indexStore))));
            // spec 33 §B / T113：会话删除级联置 DELETED（审计留存；物理删由运维按保留策略）
            all.add(RuntimeConfig.cleanupContributors(java.util.List.of(
                    io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor.of(
                            "session-index",
                            sessionId -> indexStore.get(sessionId).ifPresent(info ->
                                    indexStore.upsert(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo(
                                            info.sessionId(), info.appId(), info.agentName(),
                                            io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo.STATUS_DELETED,
                                            info.createdAtEpochMs(), info.lastActiveAtEpochMs(),
                                            info.turnCount(), info.tags())))))));
        }
        if (!resourceCustomizers.isEmpty()) {
            all.add(RuntimeConfig.sessionCustomizers(resourceCustomizers));
        }
        MemoryViewProcessor mvp = viewProcessor.getIfAvailable();
        if (mvp != null) {
            all.add(RuntimeConfig.viewProcessor(mvp));
        }
        RuntimeConfig merged = RuntimeConfig.merge(all.toArray(new RuntimeConfig[0]));
        // impl-33 / spec 13 §core-3：租约参数（buzhou.lease-ttl / buzhou.lease-renew-interval）流入运行时；
        // impl-30：停机排空预算（buzhou.lifecycle.timeout-per-shutdown-phase）流入运行时；
        // impl-34 / spec 13 §core-4：事件分发模式（buzhou.core.event-dispatch.*）流入运行时
        io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatch =
                properties.core().eventDispatch().toConfig();
        // impl-45 / spec 14 §A：spawn 容量闸（buzhou.backpressure.max-concurrent-sessions 配置且
        // 机制启用时构建；未配置 / 关闭 = null 不限，既有行为不变）
        BuzhouBackpressureProperties bp = backpressureProperties;
        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnGate spawnGate =
                bp != null && bp.enabled() && bp.maxConcurrentSessions() != null
                        && bp.maxConcurrentSessions() > 0
                        ? new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnGate(
                                bp.maxConcurrentSessions(), bp.effectiveSpawnQueueTimeout(),
                                bp.effectiveSpawnOverloadPolicy(), event -> {
                                })
                        : null;
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(chatModel, stores,
                new HarnessAssembler().withToolTimeout(properties.core().toolTimeout()), merged,
                properties.leaseTtl(), properties.effectiveLeaseRenewInterval(),
                properties.lifecycle().timeoutPerShutdownPhase(),
                eventDispatch.isBuffered() ? eventDispatch : null,
                spawnGate);
        // spec 20 / T89 / impl-64：全局事件监听 bean（如 WebhookEventForwarder）挂全部会话
        globalEventListeners.stream()
                .forEach(runtime::addGlobalEventListener);
        // spec 36 §A / T121：导出扩展 bean（模块自有段进 SessionExport.extensions）
        runtime.setExportExtensions(exportExtensionsProvider.orderedStream().toList());
        return runtime;
    }

    /**
     * impl-30 / spec 13 §core-1：core 优雅停机 lifecycle（phase =
     * {@link BuzhouLifecyclePhases#CORE}，最先 stop——拒绝新 Turn → 在途 AFTER_CURRENT_TURN
     * 取消 → 排空 → 超时硬截断）。
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    public AgentRuntimeLifecycle buzhouAgentRuntimeLifecycle(DefaultAgentRuntime runtime) {
        return new AgentRuntimeLifecycle(runtime, null);
    }

    /**
     * impl-37 / spec 13 §stores-6：保留策略族后台执行器（{@code buzhou.retention.*}）。
     * bean 恒在（{@code enabled=false} 只关自启动调度——各策略仍可手动
     * {@code RetentionSweeper#sweepOnce()} 触发）。恢复设施（ToolCallLog/RunRegistry）
     * 作为 bean 声明时并入级联清理与窗口批删；未声明时仅五槽 store 参与保留兑现。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper buzhouRetentionSweeper(
            BuzhouStores stores,
            BuzhouRetentionProperties retention,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog> toolCallLog,
            org.springframework.beans.factory.ObjectProvider<
                    io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry> runRegistry) {
        io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog tcl = toolCallLog.getIfAvailable();
        io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry registry = runRegistry.getIfAvailable();
        return new io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper(
                new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner(stores, registry, tcl),
                stores.observabilityStore(),
                stores.summaryStore(),
                tcl,
                registry,
                new io.github.chyuan_cuihongyuan.buzhou.core.retention.SessionHistoryPolicy(
                        retention.sessionRetention(), retention.sessionNotBefore()),
                new io.github.chyuan_cuihongyuan.buzhou.core.retention.ObservabilityTtl(
                        retention.observabilityTtl(), retention.observabilityBatchSize()),
                retention.summaryKeepVersions(),
                retention.toolCallLogRetention(),
                retention.runCompletedRetention(),
                new io.github.chyuan_cuihongyuan.buzhou.core.retention.MaintenanceTrigger(
                        retention.trigger().base(), retention.trigger().scaleFactor(),
                        retention.trigger().cap(), retention.trigger().hardFloor()),
                retention.sweepInterval(),
                null,
                retention.enabled());
    }
}
