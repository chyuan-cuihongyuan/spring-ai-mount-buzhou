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
@EnableConfigurationProperties({BuzhouCoreProperties.class, BuzhouRetentionProperties.class})
public class BuzhouCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "memory", matchIfMissing = true)
    public BuzhouStores buzhouStores(BuzhouCoreProperties properties) {
        // impl-36 / spec 13 §growth-8：buzhou.store.in-memory.* 容量配额流入内存套件
        return Buzhou.inMemoryStores(properties.store().inMemory().toConfig());
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
                                           List<RuntimeConfig> moduleConfigs,
                                           List<BuzhouHook> hooks,
                                           List<ToolCallback> autoTools,
                                           List<SessionAssemblyCustomizer> assemblyCustomizers,
                                           List<SessionResourceCustomizer> resourceCustomizers,
                                           ObjectProvider<MemoryViewProcessor> viewProcessor) {
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
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(), merged,
                properties.leaseTtl(), properties.effectiveLeaseRenewInterval(),
                properties.lifecycle().timeoutPerShutdownPhase(),
                eventDispatch.isBuffered() ? eventDispatch : null);
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
