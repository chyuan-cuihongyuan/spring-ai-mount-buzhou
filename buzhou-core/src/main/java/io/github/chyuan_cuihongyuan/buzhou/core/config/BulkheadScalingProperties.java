package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 舱压伸缩建议装配属性（spec 319 / T630，前缀 {@code buzhou.bulkhead.scaling}，
 * Kubernetes HPA 借鉴）。未配置 scale-up-threshold = 不装配（零变化）；
 * 配置即要求舱开启（NOOP 舱拒绝恒 0，建议恒 1 无意义——不装配）。
 *
 * @param scaleUpThreshold 每多少个窗口拒绝 = +1 倍实例（≥1；未配置 = 不装配）
 * @param maxMultiplier    建议倍率上限（≥1；null = 默认 3）
 */
@ConfigurationProperties(prefix = "buzhou.bulkhead.scaling")
public record BulkheadScalingProperties(Long scaleUpThreshold, Integer maxMultiplier) {

    /** 默认倍率上限。 */
    public static final int DEFAULT_MAX_MULTIPLIER = 3;

    public BulkheadScalingProperties {
        if (scaleUpThreshold != null && scaleUpThreshold < 1) {
            throw new BuzhouConfigurationException(
                    "buzhou.bulkhead.scaling.scale-up-threshold（" + scaleUpThreshold + "）非法",
                    ">= 1（每多少个窗口拒绝 = +1 倍实例；未配置 = 不装配建议面）");
        }
        if (maxMultiplier != null && maxMultiplier < 1) {
            throw new BuzhouConfigurationException(
                    "buzhou.bulkhead.scaling.max-multiplier（" + maxMultiplier + "）非法",
                    ">= 1（建议倍率上限；未配置 = 默认 " + DEFAULT_MAX_MULTIPLIER + "）");
        }
    }
}
