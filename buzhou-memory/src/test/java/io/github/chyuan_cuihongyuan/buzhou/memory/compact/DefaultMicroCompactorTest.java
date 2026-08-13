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
    void partialEvictRatioKeepsNewestCandidatesInline() {
        String sessionId = "ratio-" + UUID.randomUUID();
        List<BuzhouMessage> history = twentyTurnSession(sessionId, 3000);

        // 候选 = 完结 + 超过 maxAgeTurns(3) + 超 protectRecentTurns(1) 的工具消息 = turn 1..16 共 16 条；
        // ratio 0.7 → 逐出最旧 ceil(11.2)=12 条，最新 4 条（turn 13..16）原文内联续接（保连续）
        MicroCompactionResult partial = compactor.compact(history, 20,
                name -> MicroCompactionPolicy.defaults(), 1, 0.7d);
        assertThat(partial.compactedMessageIds()).hasSize(12);
        // 保留的是<b>最新的候选</b>（turn 13..16 原文内联；17..20 本就不是候选、天然保留）
        long keptNewest = partial.compactedView().stream()
                .filter(m -> m.role() == Role.TOOL && m.turnSeq() >= 13 && m.turnSeq() <= 16)
                .filter(m -> m.content() != null && !m.content().contains("evidence-id="))
                .count();
        assertThat(keptNewest).isEqualTo(4);
        // 逐出的是最旧候选（turn ≤ 12 的 tool 消息全部占位）
        long evictedOldest = partial.compactedView().stream()
                .filter(m -> m.role() == Role.TOOL && m.turnSeq() <= 12)
                .filter(m -> m.content() != null && m.content().contains("evidence-id="))
                .count();
        assertThat(evictedOldest).isEqualTo(12);

        // ratio 1.0：全量逐出，等价于既有 4 参重载（旧行为）
        MicroCompactionResult full = compactor.compact(history, 20,
                name -> MicroCompactionPolicy.defaults(), 1, 1.0d);
        assertThat(full.compactedMessageIds()).hasSize(16);
        assertThat(full.compactedMessageIds()).isEqualTo(compactor
                .compact(history, 20, name -> MicroCompactionPolicy.defaults(), 1)
                .compactedMessageIds());
    }

    @Test
    void ratioRoundsUpOnTies() {
        String sessionId = "ratio-half-" + UUID.randomUUID();
        List<BuzhouMessage> history = new ArrayList<>();
        for (int turn = 1; turn <= 5; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER, "q" + turn));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "query", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            history.add(toolResult(sessionId, turn, "tc-" + turn, "query", "x".repeat(3000)));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT, "a" + turn));
        }
        // 候选 5（current=10 距每轮均超 maxAgeTurns）× ratio 0.5 → ceil(2.5)=3 条逐出、2 条保留
        MicroCompactionResult result = compactor.compact(history, 10,
                name -> MicroCompactionPolicy.defaults(), 1, 0.5d);
        assertThat(result.compactedMessageIds()).hasSize(3);
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
