package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Todo 陈旧度审计读数（spec 716 / T1032，agent todo 纪律面板思想）：
 * in_progress 挂 N turn 不收敛是任务失控第一信号——per-item 轮次年龄 +
 * 滞留清单 + 一行人话提示（promptHint，供提示词注入面/健康面板拼装）。
 *
 * <p>纯读数不自动清理（动作归 agent/宿主）。轮次口径（TodoItem 只有
 * createdTurn——墙钟语义归 store 层）。staleAfterTurns ≤ 0 = 不判滞留。
 */
public final class TodoStalenessAudit {

    /** 单项年龄行（age = currentTurn − createdTurn）。 */
    public record Row(String id, String status, int age) {
    }

    /** 不可变报告（rows 按 age 降序——最老滞留排首）。 */
    public record Report(List<Row> rows, Map<String, Integer> byStatus,
                         List<Row> stale, int oldestOpenAge) {
    }

    private TodoStalenessAudit() {
    }

    /** 审计（null items fail-fast；staleAfterTurns ≤ 0 = 不判滞留）。 */
    public static Report analyze(List<TodoItem> items, int currentTurn, int staleAfterTurns) {
        Objects.requireNonNull(items, "items");
        List<Row> rows = new ArrayList<>(items.size());
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        int oldestOpenAge = -1;
        for (TodoItem item : items) {
            if (item == null) {
                continue;
            }
            int age = Math.max(0, currentTurn - item.createdTurn());
            rows.add(new Row(item.id(), item.status(), age));
            byStatus.merge(item.status(), 1, Integer::sum);
            if (!TodoItem.COMPLETED.equals(item.status())) {
                oldestOpenAge = Math.max(oldestOpenAge, age);
            }
        }
        rows.sort(Comparator.comparingInt(Row::age).reversed()
                .thenComparing(Row::id));
        List<Row> stale = new ArrayList<>();
        if (staleAfterTurns > 0) {
            for (Row row : rows) {
                if (!TodoItem.COMPLETED.equals(row.status()) && row.age() > staleAfterTurns) {
                    stale.add(row);
                }
            }
        }
        return new Report(List.copyOf(rows), Map.copyOf(byStatus), List.copyOf(stale), oldestOpenAge);
    }

    /** 一行人话提示（无滞留 = 空串——注入面按空跳过）。 */
    public static String promptHint(Report report) {
        Objects.requireNonNull(report, "report");
        if (report.stale().isEmpty()) {
            return "";
        }
        String ids = String.join(", ",
                report.stale().stream().map(Row::id).toList());
        return report.stale().size() + " 项滞留（最老 " + report.oldestOpenAge()
                + " turn）：" + ids;
    }
}
