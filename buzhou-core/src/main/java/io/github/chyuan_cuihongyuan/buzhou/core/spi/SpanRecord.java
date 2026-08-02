package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Map;

public record SpanRecord(
        String spanId,
        String parentSpanId,
        String sessionId,
        int turnSeq,
        String kind,
        String name,
        Instant startedAt,
        Instant endedAt,
        String status,
        Map<String, Object> attributes) {

    public SpanRecord {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
