package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1425 / T2152：会话历史形态审计——角色直方降序、连续同角色（非 TOOL）
 * 计数、空内容显形（带 toolCalls 的空 content 是正常形态）、turnGap 跳变、
 * 空输入哨兵。
 */
class ConversationShapeAuditTest {

    private static BuzhouMessage msg(int turnSeq, Role role, String content) {
        return new BuzhouMessage("id-" + turnSeq + role + content, "s", turnSeq,
                0, role, content, List.of(), null, null, null,
                java.util.Map.of(), null);
    }

    @Test
    void emptyInputYieldsZeroShape() {
        var r = ConversationShapeAudit.analyze(List.of());
        assertThat(r.totalMessages()).isZero();
        assertThat(r.roleHistogram()).isEmpty();
        assertThat(r.maxTurnGap()).isZero();
    }

    @Test
    void healthyConversationHasNoAnomalies() {
        var r = ConversationShapeAudit.analyze(List.of(
                msg(1, Role.USER, "问题"),
                msg(1, Role.ASSISTANT, "回答")));
        assertThat(r.consecutiveSameRole()).isZero();
        assertThat(r.emptyContent()).isZero();
        assertThat(r.roleHistogram()).containsEntry("USER", 1)
                .containsEntry("ASSISTANT", 1);
    }

    @Test
    void consecutiveUserMessagesAreAnomalous() {
        // 连续两条 USER（非 TOOL）：history 写坏信号
        var r = ConversationShapeAudit.analyze(List.of(
                msg(1, Role.USER, "问一"),
                msg(1, Role.USER, "问一补充"),
                msg(2, Role.ASSISTANT, "答")));
        assertThat(r.consecutiveSameRole()).isEqualTo(1);
    }

    @Test
    void consecutiveToolMessagesAreNormal() {
        // 并行工具调用：连续 TOOL 是正常形态，不计异常
        var r = ConversationShapeAudit.analyze(List.of(
                msg(1, Role.TOOL, "结果一"),
                msg(1, Role.TOOL, "结果二"),
                msg(1, Role.ASSISTANT, "汇总")));
        assertThat(r.consecutiveSameRole()).isZero();
    }

    @Test
    void emptyContentWithoutToolCallsIsFlagged() {
        var r = ConversationShapeAudit.analyze(List.of(
                msg(1, Role.USER, "问题"),
                msg(1, Role.ASSISTANT, ""), // 空回答且无工具调用 → 异常
                msg(1, Role.ASSISTANT, "  ")));
        assertThat(r.emptyContent()).isEqualTo(2);
    }

    @Test
    void turnGapDetectsReplayOrOutOfWorkOrderWrite() {
        var r = ConversationShapeAudit.analyze(List.of(
                msg(1, Role.USER, "一"),
                msg(2, Role.ASSISTANT, "二"),
                msg(7, Role.USER, "七"))); // 跳变 5
        assertThat(r.maxTurnGap()).isEqualTo(5);
    }

    @Test
    void histogramSortedByCountDescThenName() {
        var r = ConversationShapeAudit.analyze(List.of(
                msg(1, Role.USER, "a"),
                msg(1, Role.ASSISTANT, "b"),
                msg(2, Role.ASSISTANT, "c"),
                msg(3, Role.ASSISTANT, "d")));
        // ASSISTANT 3 条在前；USER/SYSTEM？只有 USER 1——降序
        assertThat(r.roleHistogram().keySet().stream().toList())
                .containsExactly("ASSISTANT", "USER");
    }
}
