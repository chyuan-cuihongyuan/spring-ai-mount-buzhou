package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 选主装配属性（spec 331 / T654，前缀 {@code buzhou.leader-election}）。
 * enabled ≠ true 或非 redis store = 不供 elector bean（sweeper 无门零变化）。
 *
 * @param enabled  开关（默认 false——单实例/无共识部署不需要）
 * @param ttl      租约 TTL（须大于消费方执行周期，建议 2×；故障转移时延 ≤ TTL+一周期）
 * @param holderId 本实例身份（缺省自动生成）
 */
@ConfigurationProperties(prefix = "buzhou.leader-election")
public record LeaderElectionProperties(
        Boolean enabled,
        Duration ttl,
        String holderId) {

    public LeaderElectionProperties {
        enabled = enabled == null ? Boolean.FALSE : enabled;
        ttl = ttl == null ? Duration.ofHours(2) : ttl;
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "buzhou.leader-election.ttl（" + ttl + "）非法——正时长，如 2h（须大于 sweep 间隔）");
        }
    }
}
