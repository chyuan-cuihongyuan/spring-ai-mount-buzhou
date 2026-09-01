package io.github.chyuan_cuihongyuan.buzhou.core.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 317 / impl-340：导出族合流打包回归——多源打包读回 / manifest 行数与
 * sha256 对账 / 空源零条目 / 单源故障隔离。
 */
class ExportBundleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    private static long writeLines(java.io.Writer out, String... lines) throws IOException {
        for (String line : lines) {
            out.write(line);
            out.write('\n');
        }
        return lines.length;
    }

    private static String entryOf(Path zip, String name) throws IOException {
        try (ZipFile file = new ZipFile(zip.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = file.getEntry(name);
            if (entry == null) {
                return null;
            }
            return new String(file.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void bundlesMultipleSourcesWithManifest() throws IOException {
        Path zip = tempDir.resolve("window-bundle.zip");
        LinkedHashMap<String, ExportBundle.ExportSource> sources = new LinkedHashMap<>();
        sources.put("pii-hits.jsonl", out -> writeLines(out, "{\"name\":\"EMAIL\"}"));
        sources.put("model-cost.jsonl", out -> writeLines(out,
                "{\"model\":\"gpt-x\"}", "{\"model\":\"gpt-y\"}"));

        var manifest = ExportBundle.bundle(zip, sources);

        assertThat(manifest).hasSize(2);
        assertThat(manifest.get(0).name()).isEqualTo("pii-hits.jsonl");
        assertThat(manifest.get(0).lines()).isEqualTo(1L);
        assertThat(manifest.get(1).lines()).isEqualTo(2L);
        // manifest 首条目可读且 sha256 在册
        String manifestJson = entryOf(zip, "manifest.json");
        JsonNode parsed = MAPPER.readTree(manifestJson);
        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0).get("sha256").asText()).hasSize(64);
        // 条目内容读回
        assertThat(entryOf(zip, "pii-hits.jsonl")).contains("EMAIL");
        assertThat(entryOf(zip, "model-cost.jsonl").split("\n")).hasSize(2);
    }

    @Test
    void sha256MatchesEntryContent() throws Exception {
        Path zip = tempDir.resolve("hash-bundle.zip");
        LinkedHashMap<String, ExportBundle.ExportSource> sources = new LinkedHashMap<>();
        sources.put("s.jsonl", out -> writeLines(out, "abc"));

        var manifest = ExportBundle.bundle(zip, sources);

        byte[] content = entryOf(zip, "s.jsonl").getBytes(StandardCharsets.UTF_8);
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        String expected = java.util.HexFormat.of().formatHex(digest.digest(content));
        assertThat(manifest.get(0).sha256()).isEqualTo(expected);
    }

    @Test
    void emptySourcesProduceManifestOnly() throws IOException {
        Path zip = tempDir.resolve("empty-bundle.zip");

        var manifest = ExportBundle.bundle(zip, new LinkedHashMap<>());

        assertThat(manifest).isEmpty();
        assertThat(entryOf(zip, "manifest.json")).isEqualTo("[]");
    }

    @Test
    void failingSourceIsolatedOthersSurvive() throws IOException {
        Path zip = tempDir.resolve("mixed-bundle.zip");
        LinkedHashMap<String, ExportBundle.ExportSource> sources = new LinkedHashMap<>();
        sources.put("good.jsonl", out -> writeLines(out, "{\"ok\":1}"));
        sources.put("bad.jsonl", out -> {
            throw new IllegalStateException("source down");
        });
        sources.put("good2.jsonl", out -> writeLines(out, "{\"ok\":2}"));

        var manifest = ExportBundle.bundle(zip, sources);

        assertThat(manifest).hasSize(3);
        assertThat(manifest.get(1).error()).contains("source down");
        assertThat(manifest.get(1).sha256()).isNull();
        assertThat(manifest.get(0).error()).isNull();
        assertThat(manifest.get(2).lines()).isEqualTo(1L);
        assertThat(entryOf(zip, "good.jsonl")).contains("\"ok\":1");
        assertThat(entryOf(zip, "bad.jsonl.error")).contains("source down");
    }
}
