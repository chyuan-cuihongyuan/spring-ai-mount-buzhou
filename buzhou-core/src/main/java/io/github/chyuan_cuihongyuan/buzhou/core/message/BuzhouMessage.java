package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BuzhouMessage(
        String id,
        String sessionId,
        int turnSeq,
        int seqInTurn,
        Role role,
        String content,
        List<ToolCallRecord> toolCalls,
        String toolCallId,
        String reasoningContent,
        String reasoningSignature,
        Map<String, Object> metadata,
        Instant createdAt) {

    public BuzhouMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
