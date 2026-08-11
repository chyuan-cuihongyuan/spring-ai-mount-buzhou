package io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 「被中断轮次」判定表（纯函数，spec「崩溃中轮次恢复」）。 */
class InterruptedTurnDetectorTest {

    private static BuzhouMessage msg(Role role, List<ToolCallRecord> toolCalls, String toolCallId) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s", 1, 0, role, "c",
                toolCalls, toolCallId, null, null, Map.of(), Instant.now());
    }

    @Test
    void shouldDetectInterrupted_whenHistoryEndsWithToolResult() {
        // 窗口 B：工具结果已落库，无最终助手回复 → 中断
        List<BuzhouMessage> stored = List.of(
                msg(Role.USER, List.of(), null),
                msg(Role.ASSISTANT, List.of(new ToolCallRecord("tc-1", "charge", "{}")), null),
                msg(Role.TOOL, List.of(), "tc-1"));
        assertThat(InterruptedTurnDetector.wasInterrupted(stored)).isTrue();
    }

    @Test
    void shouldDetectInterrupted_whenHistoryEndsWithPendingToolCalls() {
        // 窗口 A（修复前形态）：助手发了工具调用、响应未落库 → 中断
        List<BuzhouMessage> stored = List.of(
                msg(Role.USER, List.of(), null),
                msg(Role.ASSISTANT, List.of(new ToolCallRecord("tc-1", "charge", "{}")), null));
        assertThat(InterruptedTurnDetector.wasInterrupted(stored)).isTrue();
    }

    @Test
    void shouldNotDetectInterrupted_whenHistoryEndsWithTerminalAssistantReply() {
        // 完结轮次：已有终结性助手回复（无工具调用）→ 不中断
        List<BuzhouMessage> stored = List.of(
                msg(Role.USER, List.of(), null),
                msg(Role.ASSISTANT, List.of(new ToolCallRecord("tc-1", "charge", "{}")), null),
                msg(Role.TOOL, List.of(), "tc-1"),
                msg(Role.ASSISTANT, List.of(), null));
        assertThat(InterruptedTurnDetector.wasInterrupted(stored)).isFalse();
    }

    @Test
    void shouldNotDetectInterrupted_whenHistoryEmpty() {
        assertThat(InterruptedTurnDetector.wasInterrupted(List.of())).isFalse();
        assertThat(InterruptedTurnDetector.wasInterrupted(null)).isFalse();
    }
}
