package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Redis store 装配属性（spec 08 / 09 / ticket 22 + spec 13 §stores-7 ticket 32，
 * 前缀 {@code buzhou.store.redis}）。
 *
 * @param uri         Redis 连接 URI（默认 {@code redis://localhost:6379}）
 * @param keyPrefix   key 前缀（默认 {@code buzhou:}）
 * @param snapshotTtl 注入快照 TTL（impl-42 迁移：默认 PT168H——快照不再永生堆积；
 *                    显式配 ZERO 仍表示永不过期）
 * @param poolMaxSize UoW 事务连接池上限（默认 8；spec 13 §stores-7 Redis UoW 连接池化）
 */
@Validated
@ConfigurationProperties(prefix = "buzhou.store.redis")
public record RedisStoreProperties(String uri, String keyPrefix, Duration snapshotTtl, Integer poolMaxSize) {

    /** 连接池上限缺省值（与 {@code RedisBuzhouStores#DEFAULT_POOL_MAX_SIZE} 一致）。 */
    private static final int DEFAULT_POOL_MAX_SIZE = 8;

    public RedisStoreProperties {
        uri = (uri == null || uri.isBlank()) ? "redis://localhost:6379" : uri;
        keyPrefix = (keyPrefix == null || keyPrefix.isBlank()) ? "buzhou:" : keyPrefix;
        // impl-42 / spec 13 §T68 默认值安全化：快照 7 天 TTL（显式 ZERO = 永不过期，兼容既有语义）
        snapshotTtl = snapshotTtl == null ? Duration.ofHours(168) : snapshotTtl;
        if (poolMaxSize != null && poolMaxSize <= 0) {
            // impl-42 / spec 13 §T68：越界值启动即拒（负池大小此前被静默归一）
            throw new IllegalArgumentException(
                    "buzhou.store.redis.pool-max-size 必须为正整数（收到 " + poolMaxSize + "）");
        }
        poolMaxSize = poolMaxSize == null ? DEFAULT_POOL_MAX_SIZE : poolMaxSize;
    }
}
