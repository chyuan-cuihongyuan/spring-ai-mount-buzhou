package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.List;
import java.util.Map;

/**
 * impl-35 / spec 13 §stores-6：一次 deleteSession 级联的结果报告——
 * 清理成功的目标名列表 + 失败的目标名到异常映射（失败聚合可见，不静默）。
 *
 * @param sessionId 被清理的会话
 * @param cleaned   清理成功的目标（按执行顺序；null 槽位不参与、不出现）
 * @param failures  清理失败的目标 → 异常（首个失败可上抛，其余 suppressed）
 */
public record SessionCleanupResult(
        String sessionId,
        List<String> cleaned,
        Map<String, RuntimeException> failures) {

    public SessionCleanupResult {
        cleaned = cleaned == null ? List.of() : List.copyOf(cleaned);
        failures = failures == null ? Map.of() : Map.copyOf(failures);
    }

    /** 全部目标清理成功（无失败）。 */
    public boolean fullyCleaned() {
        return failures.isEmpty();
    }
}
