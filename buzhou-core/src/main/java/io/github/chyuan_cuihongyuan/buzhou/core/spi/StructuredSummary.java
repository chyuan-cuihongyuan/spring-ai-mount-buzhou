package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Map;

public record StructuredSummary(
        String sessionId,
        long version,
        Map<String, String> sections,
        int tokenEstimate,
        Instant createdAt) {

    public StructuredSummary {
        sections = sections == null ? Map.of() : Map.copyOf(sections);
    }
}
