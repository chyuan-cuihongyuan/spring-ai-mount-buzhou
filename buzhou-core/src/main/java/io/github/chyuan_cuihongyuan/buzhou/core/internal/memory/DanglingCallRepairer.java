package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import org.springframework.ai.tool.ToolCallback;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class DanglingCallRepairer {

    public static final String INTERRUPTED_RESULT = "执行被中断，结果未知";

    public record RepairEvent(String messageId, List<String> danglingToolCalls, String action) {
    }

    private final Map<String, ToolCallback> toolsByName;
    private final Set<String> idempotentToolNames;
    private final BiConsumer<String, RepairEvent> repairListener;

    public DanglingCallRepairer(Map<String, ToolCallback> toolsByName,
                                Set<String> idempotentToolNames,
                                BiConsumer<String, RepairEvent> repairListener) {
        this.toolsByName = toolsByName;
        this.idempotentToolNames = idempotentToolNames;
        this.repairListener = repairListener;
    }

    public List<BuzhouMessage> repair(String sessionId, List<BuzhouMessage> stored) {
        Set<String> respondedIds = stored.stream()
                .filter(m -> m.role() == Role.TOOL)
                .map(BuzhouMessage::toolCallId)
                .collect(Collectors.toSet());

        List<BuzhouMessage> result = new ArrayList<>();
        for (BuzhouMessage message : stored) {
            if (message.role() != Role.ASSISTANT || message.toolCalls().isEmpty()) {
                result.add(message);
                continue;
            }
            List<ToolCallRecord> missing = message.toolCalls().stream()
                    .filter(tc -> !respondedIds.contains(tc.id()))
                    .toList();
            if (missing.isEmpty()) {
                result.add(message);
                continue;
            }

            List<BuzhouMessage> synthesized = new ArrayList<>();
            List<String> stillDangling = new ArrayList<>();
            for (ToolCallRecord call : missing) {
                BuzhouMessage replayed = tryReplay(sessionId, message, call);
                if (replayed != null) {
                    synthesized.add(replayed);
                } else {
                    synthesized.add(syntheticInterrupted(sessionId, message, call));
                    stillDangling.add(call.name());
                }
            }

            if (missing.size() == message.toolCalls().size()
                    && synthesized.stream().allMatch(this::isInterrupted)) {
                if (message.content() == null || message.content().isBlank()) {
                    notifyRepair(sessionId, new RepairEvent(message.id(), stillDangling, "dropped"));
                } else {
                    result.add(demoteToPlainText(message));
                    notifyRepair(sessionId, new RepairEvent(message.id(), stillDangling, "demoted"));
                }
                continue;
            }
            result.add(message);
            result.addAll(synthesized);
            if (!stillDangling.isEmpty()) {
                notifyRepair(sessionId, new RepairEvent(message.id(), stillDangling, "synthesized"));
            }
        }
        return result;
    }

    private boolean isInterrupted(BuzhouMessage message) {
        return INTERRUPTED_RESULT.equals(message.content());
    }

    private BuzhouMessage tryReplay(String sessionId, BuzhouMessage parent, ToolCallRecord call) {
        ToolCallback tool = toolsByName.get(call.name());
        if (tool == null || !idempotentToolNames.contains(call.name())) {
            return null;
        }
        try {
            String result = tool.call(call.arguments());
            notifyRepair(sessionId, new RepairEvent(parent.id(), List.of(call.name()), "replayed"));
            return new BuzhouMessage(UUID.randomUUID().toString(), sessionId,
                    parent.turnSeq(), parent.seqInTurn() + 1, Role.TOOL, result, List.of(),
                    call.id(), null, null, Map.of("toolName", call.name(), "replayed", true),
                    Instant.now());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private BuzhouMessage syntheticInterrupted(String sessionId, BuzhouMessage parent,
                                               ToolCallRecord call) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId,
                parent.turnSeq(), parent.seqInTurn() + 1, Role.TOOL, INTERRUPTED_RESULT, List.of(),
                call.id(), null, null,
                Map.of("toolName", call.name(), "synthetic", true), Instant.now());
    }

    private BuzhouMessage demoteToPlainText(BuzhouMessage message) {
        return new BuzhouMessage(message.id(), message.sessionId(), message.turnSeq(),
                message.seqInTurn(), Role.ASSISTANT, message.content(), List.of(), null,
                message.reasoningContent(), message.reasoningSignature(), message.metadata(),
                message.createdAt());
    }

    private void notifyRepair(String sessionId, RepairEvent event) {
        if (repairListener != null) {
            repairListener.accept(sessionId, event);
        }
    }
}
