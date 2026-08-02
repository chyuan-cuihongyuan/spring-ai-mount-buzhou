package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Map;

public record EventRecord(
        String eventId,
        String spanId,
        String sessionId,
        String type,
        Instant occurredAt,
        Map<String, Object> payload) {

    public EventRecord {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
