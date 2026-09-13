package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExport;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 743 / T1086–T1087：导出脱敏命中计数——PiiType 命中/自定义规则命中/
 * 累计跨调用。
 */
class SanitizerHitCountsTest {

    @Test
    void hitsCountedByTypeAndCustomRule() {
        SessionExportSanitizer sanitizer = new SessionExportSanitizer(
                null, new CustomPiiRules(List.of(CustomPiiRules.Rule.of("TICKET_NO", "GD-\\d+"))));
        BuzhouMessage message = new BuzhouMessage("m1", "s1", 0, 0,
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                "联系 13800138000 或 a@b.com，工单号 GD-12345",
                null, null, null, null, null, Instant.EPOCH);
        SessionExport export = SessionExport.of("s1", "app", "agent",
                List.of(message), null, Map.of(), Map.of());

        SessionExport redacted = sanitizer.sanitize(export);
        assertThat(redacted.messages().get(0).content())
                .doesNotContain("13800138000")
                .doesNotContain("GD-12345");

        // 命中计数：EMAIL/PHONE 各 1（PiiDetector 口径）+ custom 工单号 1
        assertThat(sanitizer.hitCounts().get("CN_PHONE")).isEqualTo(1L);
        assertThat(sanitizer.hitCounts().get("EMAIL")).isEqualTo(1L);
        assertThat(sanitizer.hitCounts().get("custom:TICKET_NO")).isEqualTo(1L);
        assertThat(sanitizer.totalHits()).isEqualTo(3L);
    }
}
