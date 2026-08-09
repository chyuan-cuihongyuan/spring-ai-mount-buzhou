package io.github.chyuan_cuihongyuan.buzhou.observability.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityModule;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 认知 Span/Event 采集自装配（spec 03 / 09 / ticket 22）。
 *
 * <p>把 {@link ObservabilityModule#configure} 产出（含 {@code ObservabilityAssemblyCustomizer}）注册为
 * {@link RuntimeConfig} bean，供 core 装配收集合并。配置经 {@link Binder} 直绑
 * {@code buzhou.observability → ObservabilityConfig}（既有 record，缺省 {@link ObservabilityConfig#defaults()}）。
 *
 * <p>{@link MeterRegistry}（Micrometer 双写）与 {@link PipelineSink}（OTel 导出桥等旁路 sink）经容器注入，
 * 缺失时降级（无指标 / 无旁路）。模型名取 {@code buzhou.model-name}（默认 {@code unknown}）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.observability", name = "enabled", matchIfMissing = true)
public class BuzhouObservabilityAutoConfiguration {

    @Bean
    public RuntimeConfig observabilityRuntimeConfig(BuzhouStores stores, Environment env,
                                                    ObjectProvider<MeterRegistry> meterRegistry,
                                                    List<PipelineSink> sinks) {
        ObservabilityConfig config = Binder.get(env)
                .bind("buzhou.observability", ObservabilityConfig.class)
                .orElse(ObservabilityConfig.defaults());
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        return ObservabilityModule.configure(stores, config, meterRegistry.getIfAvailable(), modelName, sinks);
    }
}
