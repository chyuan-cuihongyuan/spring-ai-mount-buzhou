package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 模型韧性层自装配（spec「模型韧性层 M1」/ 09 / ticket 22）。
 *
 * <p>safe-by-default：{@code buzhou.resilience.enabled} 未配置时默认开（引入即生效、可一键关）。
 * 暴露一个 {@link RuntimeConfig} bean，由 core 收集并 merge——其内装配定制器把
 * {@code ResilienceAdvisor} + {@code RateLimitAdvisor}（如配置了限流）注入 advisor 链。
 *
 * <p>模型名取 {@code buzhou.model-name}（默认 {@code unknown}，与 observability 模块同口径）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.resilience", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ResilienceProperties.class)
public class BuzhouResilienceAutoConfiguration {

    @Bean
    public RuntimeConfig resilienceRuntimeConfig(ResilienceProperties properties, Environment env) {
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        return ResilienceModule.configure(properties, modelName);
    }
}
