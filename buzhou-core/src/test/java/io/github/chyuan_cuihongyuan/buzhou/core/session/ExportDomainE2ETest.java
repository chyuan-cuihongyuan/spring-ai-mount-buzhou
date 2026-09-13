package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-666 / spec 913：导出域三件套联动 e2e——710 协商 × 904 审计 × 911 规范化
 * 指纹 × 912 diff 全链编排语义闭环（机制单测绿 ≠ 编排咬合）。
 */
class ExportDomainE2ETest {

    private static BuzhouMessage msg(String id, String content, Map<String, Object> metadata) {
        return new BuzhouMessage(id, "s1", 1, 1, Role.USER, content,
                null, null, null, null, metadata, Instant.EPOCH);
    }

    @Test
    void keyOrderDriftDoesNotFalsifyNegotiation() {
        // 场景 1：同内容嵌套键序漂移——JCS 指纹不变 + 协商仍 UNCHANGED（漂移不误判变更）
        Map<String, Object> metaA = new LinkedHashMap<>();
        metaA.put("alpha", "1");
        metaA.put("beta", "2");
        Map<String, Object> metaB = new LinkedHashMap<>();
        metaB.put("beta", "2");
        metaB.put("alpha", "1");
        SessionExport freshA = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "hello", metaA)), null, Map.of());
        SessionExport freshB = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "hello", metaB)), null, Map.of());

        String canonicalA = SessionExportChecksum.canonicalContentFingerprint(freshA);
        String canonicalB = SessionExportChecksum.canonicalContentFingerprint(freshB);
        assertThat(canonicalA).isEqualTo(canonicalB);

        SessionExportConditional.Result negotiation =
                SessionExportConditional.exportIfChangedCanonical(freshB, canonicalA);
        assertThat(negotiation.status()).isEqualTo(SessionExportConditional.Status.UNCHANGED);
    }

    @Test
    void contentChangeNegotiatesExported() {
        // 场景 2：内容真变 → 规范化指纹变 → 协商 EXPORTED
        SessionExport v1 = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "v1", Map.of())), null, Map.of());
        SessionExport v2 = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "v2", Map.of())), null, Map.of());
        String fp1 = SessionExportChecksum.canonicalContentFingerprint(v1);
        String fp2 = SessionExportChecksum.canonicalContentFingerprint(v2);
        assertThat(fp1).isNotEqualTo(fp2);

        SessionExportConditional.Result negotiation =
                SessionExportConditional.exportIfChangedCanonical(v2, fp1);
        assertThat(negotiation.status()).isEqualTo(SessionExportConditional.Status.EXPORTED);
        assertThat(negotiation.export()).isSameAs(v2);
    }

    @Test
    void auditOrthogonalToLenientImport() {
        // 场景 3：未知顶层字段——宽松成功 + 审计拒绝（正交语义）
        String withUnknown = SessionExport.of("s1", "app", "agent",
                List.of(), null, Map.of()).toJson().replaceFirst("\\{", "{\"ghost\":1,");
        SessionExportAudit.AuditReport report = SessionExportAudit.audit(withUnknown);
        assertThat(report.strictCompatible()).isFalse();
        assertThat(report.unknownTopLevelFields()).containsExactly("ghost");
        assertThat(SessionExport.fromJson(withUnknown)).isNotNull(); // 宽松路径照常
    }

    @Test
    void diffAndNegotiationAgreeOnTimestampNoise() {
        // 场景 4：仅时戳不同的两次导出——diff identical（时戳双口径一致排除）
        SessionExport a = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "hi", Map.of())), null, Map.of());
        SessionExport b = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "hi", Map.of())), null, Map.of());
        SessionExportDiff.DiffReport report = SessionExportDiff.between(a, b);
        assertThat(report.identical()).isTrue(); // 时戳差异被 912 排除
    }

    @Test
    void diffAndFingerprintAgreeOnChange() {
        // 场景 5：内容变更——diff 显形 + 规范化指纹不同（两种口径同判「变了」）
        SessionExport a = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "old", Map.of())), null, Map.of());
        SessionExport b = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1", "new", Map.of())), null, Map.of());
        SessionExportDiff.DiffReport report = SessionExportDiff.between(a, b);
        assertThat(report.identical()).isFalse();
        assertThat(report.messageDiffs()).hasSize(1);
        assertThat(SessionExportChecksum.canonicalContentFingerprint(a))
                .isNotEqualTo(SessionExportChecksum.canonicalContentFingerprint(b));
    }
}
