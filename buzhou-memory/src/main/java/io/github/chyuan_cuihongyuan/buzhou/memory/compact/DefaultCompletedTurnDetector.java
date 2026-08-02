package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultCompletedTurnDetector implements CompletedTurnDetector {

    @Override
    public List<TurnSpan> detectTurns(List<BuzhouMessage> history) {
        Map<Integer, List<Integer>> offsetsByTurn = new java.util.TreeMap<>();
        for (int i = 0; i < history.size(); i++) {
            offsetsByTurn.computeIfAbsent(history.get(i).turnSeq(), k -> new ArrayList<>()).add(i);
        }
        Set<String> respondedIds = history.stream()
                .filter(m -> m.role() == Role.TOOL)
                .map(BuzhouMessage::toolCallId)
                .collect(Collectors.toSet());

        List<TurnSpan> spans = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Integer, List<Integer>> entry : offsetsByTurn.entrySet()) {
            List<Integer> offsets = entry.getValue();
            boolean completed = true;
            boolean endsWithAssistantText = false;
            for (int offset : offsets) {
                BuzhouMessage message = history.get(offset);
                if (message.role() == Role.ASSISTANT) {
                    if (message.toolCalls().isEmpty()) {
                        endsWithAssistantText = true;
                    } else {
                        endsWithAssistantText = false;
                        for (ToolCallRecord call : message.toolCalls()) {
                            if (!respondedIds.contains(call.id())) {
                                completed = false;
                            }
                        }
                    }
                }
            }
            spans.add(new TurnSpan(index++, offsets.getFirst(),
                    offsets.getLast() + 1, completed && endsWithAssistantText));
        }
        return spans;
    }
}
