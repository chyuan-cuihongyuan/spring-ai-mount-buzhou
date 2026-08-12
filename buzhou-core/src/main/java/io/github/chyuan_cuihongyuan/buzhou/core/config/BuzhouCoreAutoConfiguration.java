package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
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
        BuzhouShutdownProperties.class})
public class BuzhouCoreAutoConfiguration {

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
                                           BuzhouRecoveryProperties recoveryProperties) {
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
                recoveryProperties.toRecoveryConfig());
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
                                                     @Value("${spring.lifecycle.timeout-per-shutdown-phase:30s}")
                                                     String springPhaseTimeoutStr) {
        // @Value 不经 ApplicationConversionService（ApplicationContextRunner 无 Duration 转换器），
        // 故注入 String 后用 DurationStyle 解析（与 @ConfigurationProperties 的 Duration 绑定同口径）
        Duration springPhaseTimeout = DurationStyle.detect(springPhaseTimeoutStr).parse(springPhaseTimeoutStr);
        Duration effective = shutdownProperties.drainTimeout() != null
                ? shutdownProperties.drainTimeout() : springPhaseTimeout;
        return new BuzhouDrainLifecycle(runtime, effective);
    }
}
