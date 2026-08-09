package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis store 装配属性（spec 08 / 09 / ticket 22，前缀 {@code buzhou.store.redis}）。
 *
 * @param uri         Redis 连接 URI（默认 {@code redis://localhost:6379}）
 * @param keyPrefix   key 前缀（默认 {@code buzhou:}）
 * @param snapshotTtl 注入快照 TTL；{@link Duration#ZERO} 不过期（默认）
 */
@ConfigurationProperties(prefix = "buzhou.store.redis")
public record RedisStoreProperties(String uri, String keyPrefix, Duration snapshotTtl) {

    public RedisStoreProperties {
        uri = (uri == null || uri.isBlank()) ? "redis://localhost:6379" : uri;
        keyPrefix = (keyPrefix == null || keyPrefix.isBlank()) ? "buzhou:" : keyPrefix;
        snapshotTtl = snapshotTtl == null ? Duration.ZERO : snapshotTtl;
    }
}
