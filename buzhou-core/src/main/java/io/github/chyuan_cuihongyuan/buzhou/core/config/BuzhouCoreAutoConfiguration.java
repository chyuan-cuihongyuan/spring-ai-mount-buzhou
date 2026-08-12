package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.HarnessAssembler;
import io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayBudgetRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters;
import io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
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
@EnableConfigurationProperties({BuzhouCoreProperties.class, BuzhouRecoveryProperties.class,
        BuzhouShutdownProperties.class, BuzhouBackpressureProperties.class, BuzhouRunawayProperties.class})
public class BuzhouCoreAutoConfiguration {

    /** Boot 4 {@code spring.lifecycle.timeout-per-shutdown-phase} 的规范默认（drain 超时派生兜底）。 */
    private static final String DEFAULT_SHUTDOWN_PHASE_TIMEOUT = "30s";

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "memory", matchIfMissing = true)
    public BuzhouStores buzhouStores() {
        return Buzhou.inMemoryStores();
    }

    @Bean
    @ConditionalOnBean(ChatModel.class)
    @ConditionalOnMissingBean
    public AgentRuntime buzhouAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                                           List<RuntimeConfig> moduleConfigs,
                                           List<BuzhouHook> hooks,
                                           List<ToolCallback> autoTools,
                                           List<SessionAssemblyCustomizer> assemblyCustomizers,
                                           List<SessionResourceCustomizer> resourceCustomizers,
                                           ObjectProvider<MemoryViewProcessor> viewProcessor,
                                           BuzhouRecoveryProperties recoveryProperties,
                                           BuzhouBackpressureProperties backpressureProperties) {
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
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(), merged,
                recoveryProperties.toRecoveryConfig(), backpressureProperties);
    }

    // ---- 死循环与失控检测（spec「死循环与失控检测」）----

    /**
     * 失控检测内存计数器（单例，hook 与 renderer 共享）。
     *
     * <p>safe-by-default：机制默认装配，各阈值默认 null=不限（显式配置才生效）。
     */
    @Bean
    @ConditionalOnMissingBean
    public RunawayCounters runawayCounters() {
        return new RunawayCounters();
    }

    /**
     * 失控检测 Hook（{@code buzhou.runaway.enabled} 默认开）。
     *
     * <p>注入 {@link BuzhouStores} 的 {@code observabilityStore} 使 {@code runaway.*} 事件双重写入
     * （SessionEvent + EventRecord），在 dashboard 可查（参照 {@code GuardAuthApi.emitAudit} 先例）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "runawayHook")
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public BuzhouHook runawayHook(BuzhouRunawayProperties runawayProperties, RunawayCounters counters,
                                   BuzhouStores stores) {
        return new RunawayHook(runawayProperties, counters, stores.observabilityStore());
    }

    /**
     * 软退出提醒渲染器（达软阈值时经既有 Attachment 通道注入「剩余步数预算」信号）。
     *
     * <p>被 {@code BuzhouMemoryAutoConfiguration} 自动组合进 {@code CompositeAttachmentRenderer}。
     * 无 {@code per-turn.max-steps} 时不注入（合法长任务不受影响）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "runawayBudgetRenderer")
    @ConditionalOnProperty(prefix = "buzhou.runaway", name = "enabled", matchIfMissing = true)
    public AttachmentRenderer runawayBudgetRenderer(BuzhouRunawayProperties runawayProperties,
                                                     RunawayCounters counters) {
        return new RunawayBudgetRenderer(runawayProperties, counters);
    }

    /**
     * drain 生命周期 bean（spec「06 优雅停机 · SmartLifecycle 装配」）。
     *
     * <p>safe-by-default：{@code buzhou.shutdown.enabled} 默认开；用户可经 {@code @ConditionalOnMissingBean}
     * 覆盖。Spring 停机时触发与编程式入口同一 drain 编排（Spring 只是触发器）。
     *
     * <p>超时派生优先级：{@code buzhou.shutdown.drain-timeout}（显式配置）>
     * {@code spring.lifecycle.timeout-per-shutdown-phase}（Boot 4 内建属性，默认 30s）。
     * 不裸读 {@code Environment}——经 {@link BuzhouShutdownProperties} + {@link Value} 声明式注入。
     */
    @Bean
    @ConditionalOnBean(AgentRuntime.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.shutdown", name = "enabled", matchIfMissing = true)
    public BuzhouDrainLifecycle buzhouDrainLifecycle(AgentRuntime runtime,
                                                     BuzhouShutdownProperties shutdownProperties,
                                                     @Value("${spring.lifecycle.timeout-per-shutdown-phase:"
                                                             + DEFAULT_SHUTDOWN_PHASE_TIMEOUT + "}")
                                                     String springPhaseTimeoutStr) {
        // @Value 不经 ApplicationConversionService（ApplicationContextRunner 无 Duration 转换器），
        // 故注入 String 后用 DurationStyle 解析（与 @ConfigurationProperties 的 Duration 绑定同口径）
        Duration springPhaseTimeout = DurationStyle.detect(springPhaseTimeoutStr).parse(springPhaseTimeoutStr);
        Duration effective = shutdownProperties.drainTimeout() != null
                ? shutdownProperties.drainTimeout() : springPhaseTimeout;
        return new BuzhouDrainLifecycle(runtime, effective);
    }
}
