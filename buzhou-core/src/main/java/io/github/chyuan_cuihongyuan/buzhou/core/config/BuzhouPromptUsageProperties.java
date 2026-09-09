package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 提示词使用统计 yml 面（spec 424 / T740，Langfuse prompt analytics 借鉴）：
 * {@code buzhou.prompt.usage-tracking.enabled=true} 时 buzhouPromptRegistry
 * bean 包 {@code UsageTrackingPromptRegistry} 装饰器（resolve 命中记账）；
 * 默认关 = 原样 InMemory 零行为变化。
 */
@ConfigurationProperties(prefix = "buzhou.prompt.usage-tracking")
public record BuzhouPromptUsageProperties(Boolean enabled) {
}
