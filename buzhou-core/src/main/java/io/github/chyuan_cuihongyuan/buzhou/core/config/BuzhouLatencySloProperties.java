package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 时延 SLO 燃尽 yml 面（spec 509 / T770，Google SRE workbook——321 时延
 * 维度扩散）：{@code buzhou.latency-slo.{enabled, threshold-millis,
 * slo-percent, burn-rate-threshold, window, min-samples}}。
 * enabled 默认关（opt-in——完全零钩子零开销）。
 */
@ConfigurationProperties(prefix = "buzhou.latency-slo")
public record BuzhouLatencySloProperties(Boolean enabled, Long thresholdMillis,
        Double sloPercent, Double burnRateThreshold, Duration window, Integer minSamples) {

    public BuzhouLatencySloProperties {
        enabled = enabled != null && enabled;
        thresholdMillis = thresholdMillis == null ? 3_000L : thresholdMillis;
        sloPercent = sloPercent == null ? 99.0 : sloPercent;
        burnRateThreshold = burnRateThreshold == null ? 2.0 : burnRateThreshold;
        window = window == null ? Duration.ofMinutes(10) : window;
        minSamples = minSamples == null ? 20 : minSamples;
    }
}
