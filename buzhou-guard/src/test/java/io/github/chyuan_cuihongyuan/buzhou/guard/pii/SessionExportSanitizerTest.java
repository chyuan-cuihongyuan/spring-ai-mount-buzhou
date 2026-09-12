package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExport;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 518 / T787–T788：导出脱敏——消息/摘要/state 三内容域占位符化、
 * 结构字段原样、不可变副本（原导出零改动）、custom rules 叠加。
 */
class SessionExportSanitizerTest {

    private static BuzhouMessage userMessage(String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "sess-1", 1, 0,
                Role.USER, content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static SessionExport exportWithPii() {
        BuzhouMessage pii = userMessage(
                "联系 zhang.san@corp.com 或 13812345678，工单 ORD-123456");
        BuzhouMessage clean = userMessage("这条没有敏感信息");
        return new SessionExport(
                SessionExport.FORMAT, SessionExport.CURRENT_VERSION, "sess-1", "app", "agent",
                System.currentTimeMillis(), List.of(pii, clean),
                new StructuredSummary("sess-1", 1, Map.of("facts", "邮箱 zhang.san@corp.com"), 10,
                        Instant.now()),
                Map.of("pref", new StateEntry("pref", "手机 13812345678",
                        "test", 1, null, Instant.now())),
                Map.of());
    }

    @Test
    void allContentDomainsRedactedInCopy() {
        SessionExportSanitizer sanitizer = new SessionExportSanitizer();
        SessionExport original = exportWithPii();
        SessionExport sanitized = sanitizer.sanitize(original);

        // 三内容域占位符化
        assertThat(sanitized.messages().get(0).content())
                .contains("[PII:EMAIL]").contains("[PII:CN_PHONE]").doesNotContain("zhang.san@corp.com");
        assertThat(sanitized.summary().sections().get("facts")).contains("[PII:EMAIL]");
        assertThat(sanitized.state().get("pref").value()).contains("[PII:CN_PHONE]");
        // 干净内容恒等
        assertThat(sanitized.messages().get(1).content()).isEqualTo("这条没有敏感信息");
        // 结构字段原样
        assertThat(sanitized.sessionId()).isEqualTo("sess-1");
        assertThat(sanitized.messages().size()).isEqualTo(2);

        // 原导出零改动（不可变副本）
        assertThat(original.messages().get(0).content()).contains("zhang.san@corp.com");
        assertThat(original.state().get("pref").value()).contains("13812345678");
    }

    @Test
    void customRulesAndTypeSubsetCompose() {
        SessionExportSanitizer sanitizer = new SessionExportSanitizer(
                java.util.EnumSet.of(PiiType.EMAIL),
                new CustomPiiRules(List.of(CustomPiiRules.Rule.of("ORDER_ID", "ORD-\\d{6}"))));
        SessionExport sanitized = sanitizer.sanitize(exportWithPii());
        String content = sanitized.messages().get(0).content();
        assertThat(content).contains("[PII:EMAIL]").contains("[PII:ORDER_ID]");
        // 手机号类型未启用——不脱敏（类型子集语义）
        assertThat(content).contains("13812345678");
    }

    @Test
    void composeWithSealedExportPipeline() {
        // sanitize → 510 seal 组合（先脱敏再封缄——对外分享全链）
        var cipher = new io.github.chyuan_cuihongyuan.buzhou.core.crypto.EnvelopeCipher(
                java.util.Base64.getEncoder().encodeToString(new byte[32]), null);
        var sealer = new io.github.chyuan_cuihongyuan.buzhou.core.session.EncryptedSessionExport(cipher);
        SessionExport sanitized = new SessionExportSanitizer().sanitize(exportWithPii());
        String sealed = sealer.seal(sanitized);
        SessionExport reopened = sealer.open(sealed);
        assertThat(reopened.messages().get(0).content()).doesNotContain("zhang.san@corp.com");
    }

    @Test
    void nullExportFailFast() {
        assertThatThrownBy(() -> new SessionExportSanitizer().sanitize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
