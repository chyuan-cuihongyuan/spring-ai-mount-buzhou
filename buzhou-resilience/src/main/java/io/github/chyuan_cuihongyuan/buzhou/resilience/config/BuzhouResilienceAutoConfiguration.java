package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型韧性层自装配（spec 15 / 09；impl-44 补运维面、impl-57 补备模型降级链）。
 *
 * <p>safe-by-default：{@code buzhou.resilience.enabled} 未配置时默认开（引入即生效、可一键关）。
 * 暴露一个 {@link RuntimeConfig} bean，由 core 收集并 merge——其内装配定制器把
 * {@code ResilienceAdvisor} + {@code RateLimitAdvisor}（如配置了限流）注入 advisor 链。
 *
 * <p>模型名取 {@code buzhou.model-name}（默认 {@code unknown}，与 observability 模块同口径）。
 * 备模型降级链（impl-57）：{@code buzhou.resilience.fallback.models} 为 ChatModel bean 名列表，
 * 按名解析、未命中启动失败（fail-fast 防拼写错静默失效）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.resilience", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ResilienceProperties.class)
public class BuzhouResilienceAutoConfiguration {

    @Bean
    public ResilienceStats resilienceStats() {
        return new ResilienceStats();
    }

    @Bean
    public RuntimeConfig resilienceRuntimeConfig(ResilienceProperties properties, Environment env,
            ResilienceStats stats, Map<String, ChatModel> chatModels) {
        String modelName = env.getProperty("buzhou.model-name", "unknown");
        return ResilienceModule.configure(properties, modelName, stats,
                resolveFallbacks(properties, chatModels));
    }

    /** 按 bean 名解析备模型链：未命中名 fail-fast（拼写错不静默失效）。 */
    private static List<NamedFallbackModel> resolveFallbacks(ResilienceProperties properties,
            Map<String, ChatModel> chatModels) {
        ResilienceProperties.Fallback fallback = properties.fallback();
        if (fallback == null || !fallback.enabled()) {
            return null;
        }
        List<NamedFallbackModel> resolved = new ArrayList<>(fallback.models().size());
        for (String name : fallback.models()) {
            ChatModel model = chatModels.get(name);
            if (model == null) {
                throw new BuzhouConfigurationException(
                        "buzhou.resilience.fallback.models（" + name + "）未命中任何 ChatModel bean",
                        "检查 bean 名拼写；容器内可用 ChatModel bean：" + chatModels.keySet());
            }
            resolved.add(new NamedFallbackModel(name, model));
        }
        return resolved;
    }
}
