package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class DefaultMicroCompactor implements MicroCompactor {

    private final CompletedTurnDetector detector;

    public DefaultMicroCompactor(CompletedTurnDetector detector) {
        this.detector = detector;
    }

    @Override
    public MicroCompactionResult compact(List<BuzhouMessage> history,
                                         int currentTurnIndex,
                                         Function<String, MicroCompactionPolicy> policyByToolName,
                                         int protectRecentTurns) {
        Set<Integer> completedTurns = new HashSet<>();
        for (TurnSpan span : detector.detectTurns(history)) {
            if (span.completed()) {
                completedTurns.add(history.get(span.startMessageOffset()).turnSeq());
            }
        }

        List<BuzhouMessage> view = new ArrayList<>(history.size());
        List<String> compactedIds = new ArrayList<>();
        int reclaimed = 0;
        for (BuzhouMessage message : history) {
            if (isReclaimable(message, currentTurnIndex, completedTurns,
                    policyByToolName, protectRecentTurns)) {
                reclaimed += message.content() == null ? 0 : message.content().length();
                compactedIds.add(message.id());
                view.add(placeholder(message));
            } else {
                view.add(message);
            }
        }
        return new MicroCompactionResult(view, compactedIds, reclaimed);
    }

    private boolean isReclaimable(BuzhouMessage message,
                                  int currentTurnIndex,
                                  Set<Integer> completedTurns,
                                  Function<String, MicroCompactionPolicy> policyByToolName,
                                  int protectRecentTurns) {
        if (message.role() != Role.TOOL) {
            return false;
        }
        if (!completedTurns.contains(message.turnSeq())) {
            return false;
        }
        if (message.turnSeq() > currentTurnIndex - protectRecentTurns) {
            return false;
        }
        String toolName = (String) message.metadata().getOrDefault("toolName", "");
        MicroCompactionPolicy policy = policyByToolName.apply(toolName);
        if (policy.neverCompress()) {
            return false;
        }
        if (currentTurnIndex - message.turnSeq() <= policy.maxAgeTurns()) {
            return false;
        }
        return message.content() != null && message.content().length() >= policy.minSizeChars();
    }

    private BuzhouMessage placeholder(BuzhouMessage original) {
        String text = "[旧工具结果已清理，可按 evidence-id=" + original.id() + " 回查]";
        return new BuzhouMessage(original.id(), original.sessionId(), original.turnSeq(),
                original.seqInTurn(), original.role(), text, original.toolCalls(),
                original.toolCallId(), original.reasoningContent(), original.reasoningSignature(),
                original.metadata(), original.createdAt());
    }
}
