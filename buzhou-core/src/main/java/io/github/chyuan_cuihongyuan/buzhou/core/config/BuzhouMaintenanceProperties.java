package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.Instant;

/**
 * 维护窗装配属性（spec 342 / T676，前缀 {@code buzhou.maintenance}）。
 * bean 恒在（325 纪律——运行时 cordon 按钮必须预先在场）；窗未声明 =
 * 仅按钮可用（零行为变化）；<b>过期窗启动即 no-op 不追溯</b>。
 *
 * @param from    窗起点（ISO 时刻；null = 无声明窗）
 * @param until   窗终点（ISO 时刻；须晚于 from）
 * @param reason  备注（观测面）
 * @param pollInterval 轮询周期（默认 15s）
 */
@ConfigurationProperties(prefix = "buzhou.maintenance")
public record BuzhouMaintenanceProperties(
        Instant from,
        Instant until,
        String reason,
        Duration pollInterval) {

    public BuzhouMaintenanceProperties {
        reason = reason == null ? "" : reason;
        pollInterval = pollInterval == null ? Duration.ofSeconds(15) : pollInterval;
        if (pollInterval.isZero() || pollInterval.isNegative()) {
            throw new BuzhouConfigurationException(
                    "buzhou.maintenance.poll-interval（" + pollInterval + "）非法", "正时长，如 15s");
        }
        if (from != null && until != null && !from.isBefore(until)) {
            throw new BuzhouConfigurationException(
                    "buzhou.maintenance.from 须早于 until",
                    "如 from=2026-09-05T02:00:00Z / until=2026-09-05T03:00:00Z");
        }
    }
}
