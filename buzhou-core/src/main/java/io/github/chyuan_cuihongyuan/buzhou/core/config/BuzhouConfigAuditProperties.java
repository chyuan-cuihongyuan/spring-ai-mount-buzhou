package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 配置漂移审计 yml 面（spec 414 / T720，ArgoCD drift detection 借鉴）：
 * {@code buzhou.config-audit.{enabled=false, interval=30s}}。声明即装配；
 * 变更经 {@link org.springframework.context.ApplicationEventPublisher}? 否——
 * 经内置 listener 落日志 WARN（宿主可注入自定义 ConfigDriftAuditor 增强）。
 */
@ConfigurationProperties(prefix = "buzhou.config-audit")
public record BuzhouConfigAuditProperties(Boolean enabled, Duration interval) {

    public BuzhouConfigAuditProperties {
        interval = (interval == null || interval.isZero() || interval.isNegative())
                ? Duration.ofSeconds(30) : interval;
    }
}
