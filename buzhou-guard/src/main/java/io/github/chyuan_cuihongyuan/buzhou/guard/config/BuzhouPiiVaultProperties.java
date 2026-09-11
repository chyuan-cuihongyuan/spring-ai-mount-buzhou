package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 可逆 PII 代管库 yml 面（spec 507 / T766，Presidio Vault）：
 * {@code buzhou.guard.pii.vault.{enabled, ttl, max-entries}}。
 * enabled 默认关（opt-in——代管库是敏感面）；ttl 默认 1h；
 * max-entries 默认 10_000。
 */
@ConfigurationProperties(prefix = "buzhou.guard.pii.vault")
public record BuzhouPiiVaultProperties(Boolean enabled, Duration ttl, Integer maxEntries) {

    public BuzhouPiiVaultProperties {
        boolean on = enabled != null && enabled;
        ttl = ttl == null ? Duration.ofHours(1) : ttl;
        maxEntries = maxEntries == null ? 10_000 : maxEntries;
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("pii.vault.ttl 须为正时长（当前 " + ttl + "）");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("pii.vault.max-entries >= 1（当前 " + maxEntries + "）");
        }
        enabled = on;
    }
}
