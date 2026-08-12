package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 优雅停机（drain）装配属性（spec「06 优雅停机」，前缀 {@code buzhou.shutdown}）。
 *
 * <p>字段统一用 boxed 类型，null = 「未配置」→ 装配层派生（对齐 {@link BuzhouRecoveryProperties} 模板）。
 *
 * @param enabled     机制总开关（默认开，safe-by-default；关则不装配 drain 生命周期 bean）
 * @param drainTimeout drain 总预算；null = 未配置 → 派生自 {@code spring.lifecycle.timeout-per-shutdown-phase}
 *                     （Boot 4 内建属性，默认 30s），两者皆无取保守默认
 */
@ConfigurationProperties(prefix = "buzhou.shutdown")
public record BuzhouShutdownProperties(Boolean enabled, Duration drainTimeout) {

    public BuzhouShutdownProperties {
        enabled = enabled == null || enabled;
        // drainTimeout 保持 null = 未配置，由装配层派生
    }

    /** 全默认（装配测试 / 兜底用）。 */
    public static BuzhouShutdownProperties defaults() {
        return new BuzhouShutdownProperties(null, null);
    }
}
