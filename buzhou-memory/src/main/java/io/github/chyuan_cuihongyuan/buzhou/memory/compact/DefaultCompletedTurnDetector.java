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

    // —— spec 1065 / impl 817：检测读面（上游闸门空结果率思想；静态面理由同 R46–R64
    // 先例）。口径诚实：spansDetected ≤ toolCallTurnsSeen 为弱校验非硬守恒（同一轮可被
    // 多次扫描反复计入分母）；检出率 = spansDetected / toolCallTurnsSeen。
    private static final java.util.concurrent.atomic.AtomicLong DETECT_CALLS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong SPANS_DETECTED =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong TOOL_CALL_TURNS_SEEN =
            new java.util.concurrent.atomic.AtomicLong();

    /** 完成轮检测分布快照（spec 1065）。 */
    public record CompletedTurnStats(long detectCalls, long spansDetected,
                                     long toolCallTurnsSeen) {
    }

    /** 只读快照（检出率 = spansDetected / toolCallTurnsSeen，弱校验口径）。 */
    public static CompletedTurnStats stats() {
        return new CompletedTurnStats(DETECT_CALLS.get(), SPANS_DETECTED.get(),
                TOOL_CALL_TURNS_SEEN.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        DETECT_CALLS.set(0);
        SPANS_DETECTED.set(0);
        TOOL_CALL_TURNS_SEEN.set(0);
    }

    @Override
    public List<TurnSpan> detectTurns(List<BuzhouMessage> history) {
        DETECT_CALLS.incrementAndGet();
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
                        TOOL_CALL_TURNS_SEEN.incrementAndGet();
                        for (ToolCallRecord call : message.toolCalls()) {
                            if (!respondedIds.contains(call.id())) {
                                completed = false;
                            }
                        }
                    }
                }
            }
            if (completed && endsWithAssistantText) {
                SPANS_DETECTED.incrementAndGet();
            }
            spans.add(new TurnSpan(index++, offsets.getFirst(),
                    offsets.getLast() + 1, completed && endsWithAssistantText));
        }
        return spans;
    }
}
