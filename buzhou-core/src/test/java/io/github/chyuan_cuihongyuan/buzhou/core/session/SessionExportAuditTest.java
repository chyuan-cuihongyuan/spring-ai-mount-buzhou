package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-657 / spec 904：导入审计与严格模式——未知顶层字段报告 + strict 抛、
 * 干净文档 strict 等价宽松解析、缺失推荐字段入报告、非法 JSON 同口径 fail-fast、
 * 宽松路径零行为回归。
 */
class SessionExportAuditTest {

    private static SessionExport sampleExport() {
        return SessionExport.of("sess-a", "app-1", "agent-1",
                List.of(), null, Map.of());
    }

    @Test
    void cleanDocumentAuditsAndImportsStrictly() {
        String json = sampleExport().toJson();

        SessionExportAudit.AuditReport report = SessionExportAudit.audit(json);
        assertThat(report.unknownTopLevelFields()).isEmpty();
        assertThat(report.missingRecommendedFields()).isEmpty();
        assertThat(report.strictCompatible()).isTrue();

        // strict 通过且与宽松解析等价（同 sessionId）
        assertThat(SessionExportAudit.fromJsonStrict(json).sessionId())
                .isEqualTo(SessionExport.fromJson(json).sessionId());
    }

    @Test
    void unknownTopLevelFieldReportedAndStrictRefuses() {
        String json = sampleExport().toJson();
        String withUnknown = json.replaceFirst("\\{", "{\"futureField\":{\"a\":1},");

        SessionExportAudit.AuditReport report = SessionExportAudit.audit(withUnknown);
        assertThat(report.unknownTopLevelFields()).containsExactly("futureField");
        assertThat(report.strictCompatible()).isFalse();

        // 宽松路径仍成功（向后兼容零变化——静默忽略）
        assertThat(SessionExport.fromJson(withUnknown).sessionId()).isEqualTo("sess-a");
        // 严格路径拒绝且明细可读
        assertThatThrownBy(() -> SessionExportAudit.fromJsonStrict(withUnknown))
                .isInstanceOf(SessionImportException.class)
                .hasMessageContaining("futureField");
    }

    @Test
    void missingRecommendedFieldsReported() {
        // 无 sessionId、messages 为空数组（构造器要求 format/version 合法——手工拼树）
        String json = "{\"format\":\"buzhou.session-export\",\"version\":1,"
                + "\"exportedAtEpochMs\":1,\"messages\":[],"
                + "\"state\":[],\"extensions\":{}}";

        SessionExportAudit.AuditReport report = SessionExportAudit.audit(json);
        assertThat(report.missingRecommendedFields()).containsExactly("sessionId");
        assertThat(report.strictCompatible()).isFalse();
    }

    @Test
    void emptyMessagesArrayIsLegalNotMissing() {
        // 空会话导出（0 消息）是合法状态：不算缺失、严格可导入
        String json = "{\"format\":\"buzhou.session-export\",\"version\":1,"
                + "\"sessionId\":\"s\",\"exportedAtEpochMs\":1,\"messages\":[],"
                + "\"state\":[],\"extensions\":{}}";
        SessionExportAudit.AuditReport report = SessionExportAudit.audit(json);
        assertThat(report.missingRecommendedFields()).isEmpty();
        assertThat(report.strictCompatible()).isTrue();
    }

    @Test
    void nullRecommendedFieldReported() {
        String json = "{\"format\":\"buzhou.session-export\",\"version\":1,"
                + "\"sessionId\":null,\"exportedAtEpochMs\":1,\"messages\":[],"
                + "\"state\":[],\"extensions\":{}}";
        SessionExportAudit.AuditReport report = SessionExportAudit.audit(json);
        assertThat(report.missingRecommendedFields()).containsExactly("sessionId");
    }

    @Test
    void malformedJsonFailsFastSameAsFromJson() {
        assertThatThrownBy(() -> SessionExportAudit.audit("{not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
