package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 模型能力注册表 yml 面（spec 502 / T755）：{@code buzhou.resilience.
 * model-capabilities.<model>.{vision, tools, context-window}}。map 空 =
 * 不装配（门零行为——声明是渐进的，未注册模型不受影响）。
 */
@ConfigurationProperties(prefix = "buzhou.resilience.model-capabilities")
public record BuzhouModelCapabilityProperties(Map<String, ModelCapabilities> models) {

    public BuzhouModelCapabilityProperties {
        models = models == null ? Map.of() : Map.copyOf(models);
    }
}
