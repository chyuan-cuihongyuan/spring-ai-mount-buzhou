package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Map;

/**
 * 结构化摘要——记忆压缩产物的结构化载体（SummaryStore 存取）。
 */
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
