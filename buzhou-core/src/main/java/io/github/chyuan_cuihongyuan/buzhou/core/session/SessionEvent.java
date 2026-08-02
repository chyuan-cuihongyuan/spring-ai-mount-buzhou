package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Instant;
import java.util.Map;

public record SessionEvent(String type, Map<String, Object> payload, Instant occurredAt) {

    public SessionEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static SessionEvent of(String type) {
        return new SessionEvent(type, Map.of(), Instant.now());
    }
}
