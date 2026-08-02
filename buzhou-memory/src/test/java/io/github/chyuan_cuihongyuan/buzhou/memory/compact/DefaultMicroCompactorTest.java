package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMicroCompactorTest {

    private final DefaultMicroCompactor compactor =
            new DefaultMicroCompactor(new DefaultCompletedTurnDetector());

    private BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private BuzhouMessage toolResult(String sessionId, int turn, String toolCallId,
                                     String toolName, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2, Role.TOOL,
                content, List.of(), toolCallId, null, null,
                Map.of("toolName", toolName), Instant.now());
    }

    private List<BuzhouMessage> twentyTurnSession(String sessionId, int resultChars) {
        List<BuzhouMessage> history = new ArrayList<>();
        String big = "x".repeat(resultChars);
        for (int turn = 1; turn <= 20; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER, "q" + turn));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "query", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            history.add(toolResult(sessionId, turn, "tc-" + turn, "query", big));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT, "回答 " + turn));
        }
        return history;
    }

    @Test
    void oldLargeToolResultsAreCompactedWithEvidencePointer() {
        String sessionId = "compact-" + UUID.randomUUID();
        List<BuzhouMessage> history = twentyTurnSession(sessionId, 3000);

        MicroCompactionResult result = compactor.compact(history, 20,
                name -> MicroCompactionPolicy.defaults(), 1);

        assertThat(result.compactedMessageIds()).isNotEmpty();
        assertThat(result.reclaimedChars()).isGreaterThan(30000);

        List<BuzhouMessage> compacted = result.compactedView().stream()
                .filter(m -> m.content() != null && m.content().contains("evidence-id="))
                .toList();
        assertThat(compacted).isNotEmpty();
        BuzhouMessage first = compacted.getFirst();
        assertThat(first.content()).contains("evidence-id=" + first.id());
    }

    @Test
    void recentTurnsAndSmallResultsAreProtected() {
        String sessionId = "protect-" + UUID.randomUUID();
        List<BuzhouMessage> history = twentyTurnSession(sessionId, 3000);
        history.add(msg(sessionId, 21, 0, Role.USER, "q21"));
        history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 21, 1,
                Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-21", "query", "{}")),
                null, null, null, Map.of(), Instant.now()));
        history.add(toolResult(sessionId, 21, "tc-21", "query", "x".repeat(3000)));

        MicroCompactionResult result = compactor.compact(history, 21,
                name -> MicroCompactionPolicy.defaults(), 1);

        long compactedFromRecent = result.compactedView().stream()
                .filter(m -> m.turnSeq() >= 20 && m.content() != null
                        && m.content().contains("evidence-id="))
                .count();
        assertThat(compactedFromRecent).isZero();
    }

    @Test
    void neverCompressPolicyProtectsTool() {
        String sessionId = "never-" + UUID.randomUUID();
        List<BuzhouMessage> history = twentyTurnSession(sessionId, 3000);

        MicroCompactionResult result = compactor.compact(history, 20,
                name -> new MicroCompactionPolicy(true, 3, 200), 1);

        assertThat(result.compactedMessageIds()).isEmpty();
    }

    @Test
    void unfinishedTurnIsNeverTouched() {
        String sessionId = "unfinished-" + UUID.randomUUID();
        List<BuzhouMessage> history = new ArrayList<>();
        for (int turn = 1; turn <= 9; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER, "q" + turn));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "query", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            history.add(toolResult(sessionId, turn, "tc-" + turn, "query", "x".repeat(3000)));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT, "a" + turn));
        }
        history.add(msg(sessionId, 10, 0, Role.USER, "q10"));
        history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 10, 1,
                Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-10", "query", "{}")),
                null, null, null, Map.of(), Instant.now()));
        history.add(toolResult(sessionId, 10, "tc-10", "query", "x".repeat(3000)));

        MicroCompactionResult result = compactor.compact(history, 20,
                name -> MicroCompactionPolicy.defaults(), 0);

        assertThat(result.compactedMessageIds()).isNotEmpty();
        List<BuzhouMessage> turnTen = result.compactedView().stream()
                .filter(m -> m.turnSeq() == 10 && m.role() == Role.TOOL)
                .toList();
        assertThat(turnTen.getFirst().content()).doesNotContain("evidence-id=");
    }

    @Test
    void turnDetectionMarksCompleteOnlyWithResponsesAndClosingText() {
        String sessionId = "detect-" + UUID.randomUUID();
        List<BuzhouMessage> history = List.of(
                msg(sessionId, 1, 0, Role.USER, "q"),
                new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, 1, Role.ASSISTANT,
                        "", List.of(new ToolCallRecord("tc-1", "query", "{}")), null, null, null,
                        Map.of(), Instant.now()),
                toolResult(sessionId, 1, "tc-1", "query", "ok"),
                msg(sessionId, 1, 3, Role.ASSISTANT, "a"),
                msg(sessionId, 2, 0, Role.USER, "q2"));

        List<TurnSpan> spans = new DefaultCompletedTurnDetector().detectTurns(history);

        assertThat(spans).hasSize(2);
        assertThat(spans.get(0).completed()).isTrue();
        assertThat(spans.get(1).completed()).isFalse();
    }
}
