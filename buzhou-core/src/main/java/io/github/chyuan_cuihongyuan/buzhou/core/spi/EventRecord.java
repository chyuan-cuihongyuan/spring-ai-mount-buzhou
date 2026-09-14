package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Map;

/**
 * 观测事件持久化记录（ObservabilityStore 事件族载荷）。
 */
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
