package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * impl-657 / spec 904：会话导出文档导入审计 + 严格模式（pg_restore --exit-on-error /
 * protobuf unknown fields 处置思想）。宽松反解（{@link SessionExport#fromJson}，
 * 未知顶层字段静默丢弃）之上提供只读审计与 opt-in 严格入口——「导入成功 ≠ 文档无损」
 * 显形化：上游新版本多出的字段不再无声消失。
 *
 * <p>纯函数不落盘；审计读树不改动宽松路径行为（向后兼容保留）。
 */
public final class SessionExportAudit {

    /** 已知顶层字段全集（与 {@link SessionExport} record 组件一致）。 */
    private static final Set<String> KNOWN_FIELDS = Set.of(
            "format", "version", "sessionId", "appId", "agentName",
            "exportedAtEpochMs", "messages", "summary", "state", "extensions");

    /** 推荐字段（缺失即不可追溯或不可用——入审计报告）。 */
    private static final Set<String> RECOMMENDED_FIELDS =
            Set.of("sessionId", "exportedAtEpochMs", "messages");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 审计报告：{@code strictCompatible=false} 当且仅当存在未知顶层字段或缺失
     * 推荐字段（即 {@link SessionExportAudit#fromJsonStrict} 会拒绝）。
     */
    public record AuditReport(List<String> unknownTopLevelFields,
                              List<String> missingRecommendedFields,
                              boolean strictCompatible) {
    }

    private SessionExportAudit() {
    }

    /**
     * 只读审计：读树对比已知顶层组件。损坏 JSON（非对象/非法 JSON）向上抛
     * {@code IllegalArgumentException}（与 {@link SessionExport#fromJson} 同口径）。
     */
    public static AuditReport audit(String exportJson) {
        JsonNode root;
        try {
            root = MAPPER.readTree(exportJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("非法导出 JSON（审计只读，未改动任何状态）", e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("导出文档须为 JSON 对象");
        }
        List<String> unknown = new ArrayList<>();
        root.fieldNames().forEachRemaining(name -> {
            if (!KNOWN_FIELDS.contains(name)) {
                unknown.add(name);
            }
        });
        List<String> missing = new ArrayList<>();
        for (String required : RECOMMENDED_FIELDS) {
            JsonNode node = root.get(required);
            // 空数组不算缺失（空会话导出是合法状态——SessionExport.of 允许 0 消息）
            if (node == null || node.isNull()) {
                missing.add(required);
            }
        }
        boolean compatible = unknown.isEmpty() && missing.isEmpty();
        return new AuditReport(List.copyOf(new LinkedHashSet<>(unknown)),
                List.copyOf(new LinkedHashSet<>(missing)), compatible);
    }

    /**
     * 严格导入：审计不过（未知顶层字段或缺失推荐字段）抛 {@link SessionImportException}
     * （明细入消息）；通过则等价 {@link SessionExport#fromJson}。
     */
    public static SessionExport fromJsonStrict(String exportJson) {
        AuditReport report = audit(exportJson);
        if (!report.strictCompatible()) {
            throw new SessionImportException(
                    "严格导入拒绝：未知顶层字段=" + report.unknownTopLevelFields()
                            + "，缺失推荐字段=" + report.missingRecommendedFields());
        }
        return SessionExport.fromJson(exportJson);
    }
}
