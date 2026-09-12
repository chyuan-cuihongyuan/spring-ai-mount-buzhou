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
 * 会话导出 unchanged 协商测试（spec 710 / T971–T972 / impl 513）：同内容异时戳
 * UNCHANGED、内容变化 EXPORTED、垃圾匹配值 fail-open、指纹与整体校验和可区分。
 */
class SessionExportConditionalTest {

    private static BuzhouMessage message(String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s-etag", 1, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static SessionExport exportWith(String userText, List<BuzhouMessage> messages) {
        return SessionExport.of("s-etag", "app", "agent",
                messages, null, Map.of(), Map.of());
    }

    @Test
    void sameContentDifferentTimestampIsUnchanged() {
        // 同一消息列表（同 UUID）——两次导出仅 exportedAt 不同
        List<BuzhouMessage> messages = List.of(message("你好"));
        SessionExport first = exportWith("你好", messages);
        SessionExport fresh = exportWith("你好", messages);

        String fingerprint = SessionExportChecksum.contentFingerprint(first);
        SessionExportConditional.Result result =
                SessionExportConditional.exportIfChanged(fresh, fingerprint);

        assertThat(result.status()).isEqualTo(SessionExportConditional.Status.UNCHANGED);
        assertThat(result.export()).isNull(); // payload 不外发
        assertThat(result.contentFingerprint()).isEqualTo(fingerprint);
    }

    @Test
    void contentChangeExportsWithNewFingerprint() {
        SessionExport first = exportWith("你好", List.of(message("你好")));
        String fingerprint = SessionExportChecksum.contentFingerprint(first);

        SessionExport fresh = exportWith("变了", List.of(message("变了")));
        SessionExportConditional.Result result =
                SessionExportConditional.exportIfChanged(fresh, fingerprint);

        assertThat(result.status()).isEqualTo(SessionExportConditional.Status.EXPORTED);
        assertThat(result.export()).isSameAs(fresh);
        assertThat(result.contentFingerprint()).isNotEqualTo(fingerprint);
    }

    @Test
    void garbageMatchFailsOpen() {
        SessionExport fresh = exportWith("你好", List.of(message("你好")));
        String fingerprint = SessionExportChecksum.contentFingerprint(fresh);

        for (String garbage : new String[] {null, "", "   ", "bogus", "sha256:deadbeef"}) {
            SessionExportConditional.Result result =
                    SessionExportConditional.exportIfChanged(fresh, garbage);
            assertThat(result.status()).as("垃圾匹配值 " + garbage).isEqualTo(
                    SessionExportConditional.Status.EXPORTED);
        }
        assertThat(fingerprint).startsWith("sha256-c:");
    }

    @Test
    void contentFingerprintDiffersFromWholeChecksum() {
        SessionExport export = exportWith("你好", List.of(message("你好")));

        String content = SessionExportChecksum.contentFingerprint(export);
        String whole = SessionExportChecksum.of(export.toJson());

        assertThat(content).startsWith("sha256-c:").isNotEqualTo(whole);
        assertThat(whole).startsWith("sha256:");
    }
}
