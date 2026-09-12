package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * 多模型加权路由装配属性（spec 339 / T670，前缀 {@code buzhou.routing}）。
 * weights 以 ChatModel bean 名为键、正整数为权重；&lt;2 项 = 不装配
 * （零变化）。
 *
 * @param weights   beanName → 正整数权重（如 buzhou.routing.weights.cheap-model=7）
 * @param slowStart 慢启动爬坡窗（spec 725 / T1001——权重上调先落 floor 分步爬坡；
 *                  null = 关——默认零变化）
 */
@ConfigurationProperties(prefix = "buzhou.routing")
public record BuzhouRoutingProperties(Map<String, Integer> weights, Duration slowStart) {

    public BuzhouRoutingProperties(Map<String, Integer> weights) {
        this(weights, null);
    }

    /** 多构造绑定坑（R39 同法，见 BuzhouMcpProperties）：canonical 显式标注供 yml 绑定。 */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
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
        if (slowStart != null && slowStart.isNegative()) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.config
                    .BuzhouConfigurationException(
                    "buzhou.routing.slow-start（" + slowStart + "）非法",
                    "非负时长或不声明（关）");
        }
    }

    /** 是否声明了路由面（≥2 路）。 */
    public boolean routingConfigured() {
        return weights.size() >= 2;
    }
}
