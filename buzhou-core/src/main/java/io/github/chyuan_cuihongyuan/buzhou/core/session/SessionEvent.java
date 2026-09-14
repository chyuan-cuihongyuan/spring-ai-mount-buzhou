package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Instant;
import java.util.Map;

/**
 * 会话事件载体——type（受控词表）+ payload（不可变快照）+ occurredAt（发生时刻）；
 * {@code of} 静态工厂默认当前时刻。
 */
public record SessionEvent(String type, Map<String, Object> payload, Instant occurredAt) {

    public SessionEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static SessionEvent of(String type) {
        return new SessionEvent(type, Map.of(), Instant.now());
    }

    public static SessionEvent of(String type, Map<String, Object> payload) {
        return new SessionEvent(type, payload, Instant.now());
    }
}
