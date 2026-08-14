package io.github.chyuan_cuihongyuan.buzhou.observability.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityModule;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.AsyncObservabilityPipeline;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 认知 Span/Event 采集自装配（spec 03 / 09 / ticket 22；impl-46 生命周期收口）。
 *
 * <p>{@link ObservabilityConfig} 经 {@code @ConfigurationProperties} 正规绑定（此前 {@code Binder}
 * 直绑绕过元数据/校验）。异步管线暴露为 bean（{@code destroyMethod=close}）——
 * Spring context 关闭/刷新时管线随容器排空关闭，drain 线程不再泄漏
 * （JVM shutdown hook 仅作非装配路径兜底，close 时主动摘除）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.observability", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ObservabilityConfig.class)
public class BuzhouObservabilityAutoConfiguration {

    /**
     * impl-46：per-JVM 异步管线 bean 化。观察 {@code ObservabilityModule} 的单例语义
     * （per-JVM 一条 drain 线程）；{@code enabled=false} 时本装配整体不生效。
     * {@code destroyMethod=close} 显式声明（flush 排空 + 停 drain 线程 + 摘 shutdown hook）。
     */
    @Bean(destroyMethod = "close")
    public AsyncObservabilityPipeline observabilityPipeline(BuzhouStores stores, ObservabilityConfig config,
                                                            List<PipelineSink> sinks) {
        return new AsyncObservabilityPipeline(stores.observabilityStore(), config,
                new io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter(),
                sinks);
    }

    @Bean
    public RuntimeConfig observabilityRuntimeConfig(BuzhouStores stores, ObservabilityConfig config,
                                                    Environment env,
                                                    AsyncObservabilityPipeline pipeline) {
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        return ObservabilityModule.configure(stores, config, modelName, pipeline);
    }
}
