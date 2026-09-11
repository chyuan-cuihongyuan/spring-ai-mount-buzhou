package io.github.chyuan_cuihongyuan.buzhou.resilience.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 请求幂等键 yml 面（spec 501 / T754，Stripe Idempotency-Key 借鉴）：
 * {@code buzhou.resilience.idempotency.{enabled, ttl, maxEntries}}。
 * enabled 默认 false（opt-in）；ttl 默认 24h（Stripe 同款键保留窗）；
 * maxEntries 默认 1024（进程内 LRU——跨实例共享为诚实边界外）。
 */
@ConfigurationProperties(prefix = "buzhou.resilience.idempotency")
public record BuzhouIdempotencyProperties(Boolean enabled, Duration ttl, Integer maxEntries) {

    /** 默认键保留窗（Stripe 24h 同款）。 */
    static final Duration DEFAULT_TTL = Duration.ofHours(24);
    /** 默认容量。 */
    static final int DEFAULT_MAX_ENTRIES = 1024;

    public BuzhouIdempotencyProperties {
        boolean on = enabled != null && enabled;
        ttl = ttl == null ? DEFAULT_TTL : ttl;
        maxEntries = maxEntries == null ? DEFAULT_MAX_ENTRIES : maxEntries;
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "idempotency.ttl 必须为正时长（当前 " + ttl + "）");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException(
                    "idempotency.max-entries 必须 >= 1（当前 " + maxEntries + "）");
        }
        // 归一化输出（消费方拿到的 enabled 恒非 null）
        enabled = on;
    }
}
