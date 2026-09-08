package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import java.time.Duration;
import java.time.Instant;

/**
 * 共享事实（spec 410 / T711，mem0 共享记忆借鉴）：跨会话/跨 agent 的
 * 键值事实——key 即所有权（非 owner 发布已存在键 fail-fast）；可选 ttl
 * （null = 永久；读时按 Clock 判过期）。
 */
public record SharedFact(String key, Object value, String owner,
        Instant createdAt, Duration ttl) {

    public SharedFact {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key 非空");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner 非空");
        }
        createdAt = createdAt == null ? Instant.EPOCH : createdAt;
    }
}
