package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 716 / T1032–T1033：Todo 陈旧度审计——滞留判定/降序/完成项豁免/
 * 阈值关闭/promptHint/null fail-fast。
 */
class TodoStalenessAuditTest {

    private static TodoItem item(String id, String status, int createdTurn) {
        return new TodoItem(id, "任务 " + id, status, createdTurn);
    }

    @Test
    void staleDetectionSortedByAgeDescending() {
        TodoStalenessAudit.Report report = TodoStalenessAudit.analyze(List.of(
                item("t1", TodoItem.PENDING, 3),
                item("t2", TodoItem.IN_PROGRESS, 1),
                item("t3", TodoItem.COMPLETED, 0)), 12, 5);
        assertThat(report.rows()).extracting(TodoStalenessAudit.Row::id)
                .containsExactly("t3", "t2", "t1"); // age 12/11/9 降序
        assertThat(report.byStatus()).containsEntry("pending", 1)
                .containsEntry("in_progress", 1).containsEntry("completed", 1);
        // 滞留：未完成且 age>5 → t1(9)、t2(11)；completed 的 t3(12) 豁免
        assertThat(report.stale()).extracting(TodoStalenessAudit.Row::id)
                .containsExactly("t2", "t1");
        assertThat(report.oldestOpenAge()).isEqualTo(11);
    }

    @Test
    void completedItemsNeverStaleAndZeroThresholdDisables() {
        TodoStalenessAudit.Report completedOnly = TodoStalenessAudit.analyze(
                List.of(item("t9", TodoItem.COMPLETED, 0)), 30, 5);
        assertThat(completedOnly.stale()).isEmpty();

        TodoStalenessAudit.Report disabled = TodoStalenessAudit.analyze(
                List.of(item("t1", TodoItem.IN_PROGRESS, 0)), 30, 0);
        assertThat(disabled.stale()).isEmpty(); // 阈值 0 = 只报统计
        assertThat(disabled.rows()).hasSize(1);
    }

    @Test
    void promptHintShapesAndNullFailFast() {
        TodoStalenessAudit.Report withStale = TodoStalenessAudit.analyze(
                List.of(item("t3", TodoItem.IN_PROGRESS, 0),
                        item("t7", TodoItem.PENDING, 4)), 12, 5);
        assertThat(TodoStalenessAudit.promptHint(withStale))
                .isEqualTo("2 项滞留（最老 12 turn）：t3, t7");

        TodoStalenessAudit.Report clean = TodoStalenessAudit.analyze(
                List.of(item("t1", TodoItem.COMPLETED, 0)), 5, 5);
        assertThat(TodoStalenessAudit.promptHint(clean)).isEmpty();

        assertThatThrownBy(() -> TodoStalenessAudit.analyze(null, 1, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TodoStalenessAudit.promptHint(null))
                .isInstanceOf(NullPointerException.class);
    }
}
