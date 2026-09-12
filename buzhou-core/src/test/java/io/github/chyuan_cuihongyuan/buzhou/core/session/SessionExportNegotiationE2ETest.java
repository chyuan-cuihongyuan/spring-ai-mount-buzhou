package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导出协商联动补验（spec 733 / T1017–T1018 / impl 536）：往返指纹稳定、
 * 两轮协商周期、双校验和幂等。
 */
class SessionExportNegotiationE2ETest {

    private static BuzhouMessage message(String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s-e2e", 1, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void fingerprintStableAcrossJsonRoundtrip() {
        SessionExport export = SessionExport.of("s-e2e", "app", "agent",
                List.of(message("问")), null, Map.of(), Map.of());

        String before = SessionExportChecksum.contentFingerprint(export);
        SessionExport roundtrip = SessionExport.fromJson(export.toJson());
        String after = SessionExportChecksum.contentFingerprint(roundtrip);

        assertThat(after).isEqualTo(before); // 往返不漂
    }

    @Test
    void twoRoundSyncCycleSemantics() {
        // 轮 1：导出 + 持久化指纹
        SessionExport round1 = SessionExport.of("s-e2e", "app", "agent",
                List.of(message("v1")), null, Map.of(), Map.of());
        String etag = SessionExportChecksum.contentFingerprint(round1);

        // 轮 2：内容未变（新导出，exportedAt 不同）→ UNCHANGED
        SessionExport round2Same = SessionExport.of("s-e2e", "app", "agent",
                round1.messages(), null, Map.of(), Map.of());
        assertThat(SessionExportConditional.exportIfChanged(round2Same, etag).status())
                .isEqualTo(SessionExportConditional.Status.UNCHANGED);

        // 轮 3：内容变了 → EXPORTED + 新指纹（供轮 4 协商）
        SessionExport round3 = SessionExport.of("s-e2e", "app", "agent",
                List.of(message("v2")), null, Map.of(), Map.of());
        SessionExportConditional.Result result =
                SessionExportConditional.exportIfChanged(round3, etag);
        assertThat(result.status()).isEqualTo(SessionExportConditional.Status.EXPORTED);
        assertThat(result.contentFingerprint()).isNotEqualTo(etag);

        // 轮 4：携新指纹再协商 → UNCHANGED（同步方稳定收敛）
        SessionExport round4Same = SessionExport.of("s-e2e", "app", "agent",
                round3.messages(), null, Map.of(), Map.of());
        assertThat(SessionExportConditional.exportIfChanged(round4Same,
                result.contentFingerprint()).status())
                .isEqualTo(SessionExportConditional.Status.UNCHANGED);
    }

    @Test
    void bothChecksumsIdempotentAcrossRoundtrip() {
        SessionExport export = SessionExport.of("s-e2e", "app", "agent",
                List.of(message("v")), null, Map.of(), Map.of());
        String json1 = export.toJson();
        String whole1 = SessionExportChecksum.of(json1);
        String content1 = SessionExportChecksum.contentFingerprint(export);

        SessionExport roundtrip = SessionExport.fromJson(json1);
        assertThat(SessionExportChecksum.of(roundtrip.toJson())).isEqualTo(whole1);
        assertThat(SessionExportChecksum.contentFingerprint(roundtrip)).isEqualTo(content1);
    }
}
