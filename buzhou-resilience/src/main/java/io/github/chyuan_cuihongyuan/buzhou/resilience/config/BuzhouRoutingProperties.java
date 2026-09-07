package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 多模型加权路由装配属性（spec 339 / T670，前缀 {@code buzhou.routing}）。
 * weights 以 ChatModel bean 名为键、正整数为权重；&lt;2 项 = 不装配
 * （零变化）。
 *
 * @param weights beanName → 正整数权重（如 buzhou.routing.weights.cheap-model=7）
 */
@ConfigurationProperties(prefix = "buzhou.routing")
public record BuzhouRoutingProperties(Map<String, Integer> weights) {

    public BuzhouRoutingProperties {
        weights = weights == null ? Map.of() : Map.copyOf(weights);
        weights.forEach((name, weight) -> {
            if (weight == null || weight < 1) {
                throw new io.github.chyuan_cuihongyuan.buzhou.core.config
                        .BuzhouConfigurationException(
                        "buzhou.routing.weights." + name + "（" + weight + "）非法",
                        "每路正整数 ≥1");
            }
        });
    }

    /** 是否声明了路由面（≥2 路）。 */
    public boolean routingConfigured() {
        return weights.size() >= 2;
    }
}
