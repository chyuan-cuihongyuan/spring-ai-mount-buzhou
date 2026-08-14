package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import java.time.Instant;
import java.util.List;

/**
 * impl-37 / spec 13 §stores-6：一次保留清理周期的报告（清理动作可观测——监听者收到本报告；
 * 失败逐项收集、不中断周期）。
 *
 * @param sweptAt               周期时刻
 * @param sessionsDeleted       会话保留策略级联删除的封闭会话数
 * @param observabilityPruned   观测 TTL 批删条数（events/spans/snapshots）
 * @param summaryVersionsPruned 摘要旧版本修剪条数
 * @param toolCallLogPruned     工具调用日志窗口外删除条数
 * @param completedRunsPruned   COMPLETED run 窗口外删除条数
 * @param failures              失败步骤描述（WARN 已记，不中断周期）
 */
public record RetentionSweepReport(
        Instant sweptAt,
        int sessionsDeleted,
        int observabilityPruned,
        int summaryVersionsPruned,
        int toolCallLogPruned,
        int completedRunsPruned,
        List<String> failures) {

    public RetentionSweepReport {
        failures = failures == null ? List.of() : List.copyOf(failures);
    }

    public boolean fullySucceeded() {
        return failures.isEmpty();
    }
}
