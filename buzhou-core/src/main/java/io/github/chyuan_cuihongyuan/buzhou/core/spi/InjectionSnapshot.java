package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InjectionSnapshot(
        String sessionId,
        int turnSeq,
        List<String> messageIds,
        Map<String, Object> budgetBreakdown,
        Instant createdAt) {

    public InjectionSnapshot {
        messageIds = messageIds == null ? List.of() : List.copyOf(messageIds);
        budgetBreakdown = budgetBreakdown == null ? Map.of() : Map.copyOf(budgetBreakdown);
    }
}
