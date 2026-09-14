package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1065 / impl 817：完成轮检测器读面——检出（spansDetected）、
 * 分母（toolCallTurnsSeen）、悬挂轮全量失能信号、resetForTest 归零。
 */
class CompletedTurnStatsTest {

    private static BuzhouMessage user(int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static BuzhouMessage assistantWithCall(int turn, String callId) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 1, Role.ASSISTANT,
                null, List.of(new ToolCallRecord(callId, "write_file", "{}")),
                null, null, null, Map.of(), Instant.now());
    }

    private static BuzhouMessage toolResult(int turn, String callId, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 2, Role.TOOL,
                content, List.of(), callId, null, null, Map.of(), Instant.now());
    }

    private static BuzhouMessage assistantText(int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 3, Role.ASSISTANT,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @BeforeEach
    void reset() {
        DefaultCompletedTurnDetector.resetForTest();
    }

    @Test
    void completedTurnCountsDetected() {
        DefaultCompletedTurnDetector detector = new DefaultCompletedTurnDetector();
        List<TurnSpan> spans = detector.detectTurns(List.of(
                user(1, "做任务"),
                assistantWithCall(1, "call-1"),
                toolResult(1, "call-1", "ok"),
                assistantText(1, "完成")));
        assertThat(spans.getLast().completed()).isTrue();

        DefaultCompletedTurnDetector.CompletedTurnStats stats =
                DefaultCompletedTurnDetector.stats();
        assertThat(stats.detectCalls()).isEqualTo(1);
        assertThat(stats.spansDetected()).isEqualTo(1);
        assertThat(stats.toolCallTurnsSeen()).isEqualTo(1);
    }

    @Test
    void hangingTurnsYieldZeroDetectionSignal() {
        DefaultCompletedTurnDetector detector = new DefaultCompletedTurnDetector();
        // 全悬挂轮：工具调用无响应 → 检出 0（压缩管线失能信号可见）
        List<TurnSpan> spans = detector.detectTurns(List.of(
                user(1, "做任务"),
                assistantWithCall(1, "call-hang")));
        assertThat(spans).allSatisfy(span -> assertThat(span.completed()).isFalse());

        DefaultCompletedTurnDetector.CompletedTurnStats stats =
                DefaultCompletedTurnDetector.stats();
        assertThat(stats.spansDetected()).isZero();
        assertThat(stats.toolCallTurnsSeen()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        DefaultCompletedTurnDetector detector = new DefaultCompletedTurnDetector();
        detector.detectTurns(List.of(user(1, "x"), assistantText(1, "done")));
        assertThat(DefaultCompletedTurnDetector.stats().detectCalls()).isEqualTo(1);

        DefaultCompletedTurnDetector.resetForTest();

        DefaultCompletedTurnDetector.CompletedTurnStats stats =
                DefaultCompletedTurnDetector.stats();
        assertThat(stats.detectCalls()).isZero();
        assertThat(stats.spansDetected()).isZero();
        assertThat(stats.toolCallTurnsSeen()).isZero();
    }
}
