package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 成本尖峰检测 yml 面（spec 508 / T768，Prometheus/Istio 异常检测）：
 * {@code buzhou.budget.spike.{enabled, baseline-buckets, min-samples,
 * z-threshold, floor-micro-usd, cooldown}}。enabled 默认关（opt-in——
 * 403 同族）。
 */
@ConfigurationProperties(prefix = "buzhou.budget.spike")
public record BuzhouCostSpikeProperties(Boolean enabled, Integer baselineBuckets,
        Integer minSamples, Double zThreshold, Long floorMicroUsd, Duration cooldown) {

    public BuzhouCostSpikeProperties {
        enabled = enabled != null && enabled;
        baselineBuckets = baselineBuckets == null ? 30 : baselineBuckets;
        minSamples = minSamples == null ? 10 : minSamples;
        zThreshold = zThreshold == null ? 3.0 : zThreshold;
        floorMicroUsd = floorMicroUsd == null ? 10_000L : floorMicroUsd;
        cooldown = cooldown == null ? Duration.ofMinutes(5) : cooldown;
    }
}
