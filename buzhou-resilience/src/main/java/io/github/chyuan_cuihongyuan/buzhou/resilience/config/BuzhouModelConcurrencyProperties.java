package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * 模型并发舱 yml 面（spec 426 / T744，Resilience4j SemaphoreBulkhead /
 * Uber concurrency-limits 借鉴）：{@code buzhou.resilience.model-concurrency.
 * {limits.<model>, acquire-timeout}}。limits 非空才装配（模型名→并发上限，
 * 未列模型 NOOP 不限）；acquire-timeout 默认 0 = fail-fast。
 */
@ConfigurationProperties(prefix = "buzhou.resilience.model-concurrency")
public record BuzhouModelConcurrencyProperties(Map<String, Integer> limits, Duration acquireTimeout) {

    public BuzhouModelConcurrencyProperties {
        limits = limits == null ? Map.of() : Map.copyOf(limits);
        acquireTimeout = acquireTimeout == null ? Duration.ZERO : acquireTimeout;
        if (acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("acquire-timeout 非负（当前 " + acquireTimeout + "）");
        }
    }
}
