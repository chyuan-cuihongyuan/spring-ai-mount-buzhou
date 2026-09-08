package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 健康时间线 yml 面（spec 405 / T702，PagerDuty incident timeline 借鉴）：
 * {@code buzhou.health.timeline.{enabled=false, interval=15s, capacity=256,
 * export-path}}。export-path 可选（声明即逐变迁落盘 JSONL）。
 */
@ConfigurationProperties(prefix = "buzhou.health.timeline")
public record BuzhouHealthTimelineProperties(Boolean enabled, Duration interval,
        Integer capacity, String exportPath) {

    public BuzhouHealthTimelineProperties {
        interval = (interval == null || interval.isZero() || interval.isNegative())
                ? Duration.ofSeconds(15) : interval;
        capacity = (capacity == null || capacity <= 0) ? 256 : capacity;
        exportPath = exportPath == null || exportPath.isBlank() ? null : exportPath;
    }
}
