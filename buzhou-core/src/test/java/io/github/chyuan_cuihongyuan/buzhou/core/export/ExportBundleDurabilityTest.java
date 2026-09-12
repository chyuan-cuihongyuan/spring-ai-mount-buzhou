package io.github.chyuan_cuihongyuan.buzhou.core.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导出打包落盘持久档测试（spec 621 / T892–T893 / impl 474，sqlite WAL 同步档位语义）：
 * FILE_AND_DIR 完成后 zip 可读且 manifest 完整；NONE 默认路径不变；null 档按 NONE。
 */
class ExportBundleDurabilityTest {

    @TempDir
    Path tmp;

    private LinkedHashMap<String, ExportBundle.ExportSource> oneSource(String content) {
        LinkedHashMap<String, ExportBundle.ExportSource> sources = new LinkedHashMap<>();
        sources.put("stats.jsonl", out -> {
            out.write(content);
            return 1;
        });
        return sources;
    }

    /** FILE_AND_DIR：force 完成后 zip 结构完整可读（manifest 首条 + 内容条目）。 */
    @Test
    void fileAndDirDurabilityProducesIntactBundle() throws Exception {
        Path zip = tmp.resolve("nested/out.zip");

        var manifest = ExportBundle.bundle(zip, oneSource("hello\n"),
                ExportBundle.Durability.FILE_AND_DIR);

        assertThat(manifest).hasSize(1);
        assertThat(manifest.get(0).name()).isEqualTo("stats.jsonl");
        assertThat(manifest.get(0).lines()).isEqualTo(1);
        assertThat(manifest.get(0).sha256()).isNotBlank();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry first = in.getNextEntry();
            assertThat(first.getName()).isEqualTo("manifest.json");
            byte[] manifestBytes = in.readAllBytes();
            assertThat(new String(manifestBytes, StandardCharsets.UTF_8)).contains("stats.jsonl");
            assertThat(in.getNextEntry().getName()).isEqualTo("stats.jsonl");
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello\n");
        }
    }

    /** NONE 默认与 null 档：既有行为（不 force）照常产出。 */
    @Test
    void noneAndNullDurabilityKeepDefaultPath() throws Exception {
        Path zipA = tmp.resolve("a.zip");
        Path zipB = tmp.resolve("b.zip");

        assertThat(ExportBundle.bundle(zipA, oneSource("x"))).hasSize(1);
        assertThat(ExportBundle.bundle(zipB, oneSource("x"), null)).hasSize(1);
        assertThat(Files.exists(zipA)).isTrue();
        assertThat(Files.exists(zipB)).isTrue();
    }
}
