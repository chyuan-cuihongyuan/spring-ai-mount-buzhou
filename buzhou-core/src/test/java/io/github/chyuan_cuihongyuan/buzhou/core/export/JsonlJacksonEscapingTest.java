package io.github.chyuan_cuihongyuan.buzhou.core.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.FailureTurnSnapshots;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookDeadLetter;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookDeadLetterJsonl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON 行手工拼接收口对抗测试（spec 644 / T938–T939 / impl 497）：注入面
 * （引号/反斜杠/换行/回车/制表符）→ 行仍合法 JSON 且 Jackson 回读得原值
 * （旧手工拼接：ExportBundle 土法=丢信息、FailureTurnSnapshots/DeadLetter
 * 自有 escape=不覆盖控制字符；PromptUsage 用例在 prompt 包同款）。
 */
class JsonlJacksonEscapingTest {

    private static final ObjectMapper READER = new ObjectMapper();

    /** 注入样本：覆盖引号/反斜杠/换行/回车/制表符。 */
    static final String HOSTILE = "a\"b\\c\nd\re\tf";

    @TempDir
    Path dir;

    /** WebhookDeadLetterJsonl：hostile eventId/type 回读原值。 */
    @Test
    void deadLetterHostileFieldsRoundTrip() throws Exception {
        StringWriter out = new StringWriter();
        WebhookDeadLetterJsonl.export(out, java.util.List.of(
                new WebhookDeadLetter(HOSTILE, HOSTILE, 2, Instant.parse("2026-09-13T00:00:00Z"))));
        var node = READER.readTree(out.toString().trim());
        assertThat(node.get("eventId").asText()).isEqualTo(HOSTILE);
        assertThat(node.get("type").asText()).isEqualTo(HOSTILE);
        assertThat(node.get("attempts").asInt()).isEqualTo(2);
    }

    /** ExportBundle manifest：hostile error（多行 IO 异常消息）清单合法且信息不丢。 */
    @Test
    void exportBundleHostileErrorRoundTrips() throws Exception {
        LinkedHashMap<String, ExportBundle.ExportSource> sources = new LinkedHashMap<>();
        sources.put("source-a", w -> {
            throw new IllegalStateException("boom \"q\"\nline-2\ttab");
        });
        sources.put("source-b", w -> {
            w.write("{\"ok\":true}\n");
            return 1L;
        });
        Path zip = dir.resolve("bundle.zip");
        java.util.List<ExportBundle.ManifestEntry> manifest = ExportBundle.bundle(zip, sources);
        assertThat(manifest.get(0).error()).contains("boom \"q\"", "line-2\ttab");
        // manifest.json 首条目回读：error 原值（含引号/换行/制表符——旧土法丢信息）
        try (var zin = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
            java.util.zip.ZipEntry e;
            String manifestJson = null;
            while ((e = zin.getNextEntry()) != null) {
                if ("manifest.json".equals(e.getName())) {
                    manifestJson = new String(zin.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            assertThat(manifestJson).isNotNull();
            var entries = READER.readTree(manifestJson);
            assertThat(entries.get(0).get("error").asText())
                    .isEqualTo(manifest.get(0).error());
        }
    }

    /** FailureTurnSnapshots：hostile message/preview 回读原值。 */
    @Test
    void failureSnapshotsHostileFieldsRoundTrip() throws Exception {
        FailureTurnSnapshots snapshots = new FailureTurnSnapshots(8, 512);
        snapshots.onTurnStart(1, HOSTILE);
        snapshots.onTurnError(1, new IllegalStateException(HOSTILE));
        StringWriter out = new StringWriter();
        assertThat(snapshots.exportJsonl(out)).isEqualTo(1);
        var node = READER.readTree(out.toString().trim());
        assertThat(node.get("errorClass").asText()).isEqualTo(IllegalStateException.class.getName());
        assertThat(node.get("errorMessage").asText()).isEqualTo(HOSTILE);
        assertThat(node.get("inputPreview").asText()).isEqualTo(HOSTILE);
    }
}
