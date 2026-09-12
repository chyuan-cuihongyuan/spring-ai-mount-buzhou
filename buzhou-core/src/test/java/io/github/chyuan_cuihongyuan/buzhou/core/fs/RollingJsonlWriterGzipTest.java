package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JSONL 轮转旧档 gzip 压缩测试（spec 712 / T975–T976 / impl 515）：压缩线以上
 * .gz 可解、file.1 恒明文、双形态清理、默认 0 全明文零回归、静态路径同口径。
 */
class RollingJsonlWriterGzipTest {

    @TempDir
    Path tmp;

    private static String content(Path file) throws Exception {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static String gunzip(Path file) throws Exception {
        try (GZIPInputStream gz = new GZIPInputStream(Files.newInputStream(file))) {
            return new String(gz.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void generationsAboveCompressLineAreGzipped() throws Exception {
        Path target = tmp.resolve("events.jsonl");
        try (RollingJsonlWriter writer =
                new RollingJsonlWriter(target, 20, 3, 2)) { // 3 代历史，代际 ≥2 压缩
            writer.appendLine("{\"g\":1}");
            writer.appendLine("{\"g\":2}");   // 16B ≤ 20B 同代
            writer.appendLine("{\"g\":3}");   // 轮转 1：{g1,g2} → file.1
            writer.appendLine("{\"g\":4}");   // 16B 同代
            writer.appendLine("{\"g\":5}");   // 轮转 2：{g3,g4} → file.1，{g1,g2} → file.2 跨线转码
        }

        assertThat(target).exists();                                    // 当前代 g5
        assertThat(tmp.resolve("events.jsonl.1")).exists();             // file.1 恒明文（delaycompress）
        assertThat(content(tmp.resolve("events.jsonl.1"))).contains("\"g\":3");
        assertThat(tmp.resolve("events.jsonl.2.gz")).exists();          // 跨线档已压缩
        assertThat(gunzip(tmp.resolve("events.jsonl.2.gz"))).contains("\"g\":1");
        assertThat(tmp.resolve("events.jsonl.2")).doesNotExist();       // 明文形态不存在
    }

    @Test
    void zeroCompressKeepsAllPlain() throws Exception {
        Path target = tmp.resolve("plain.jsonl");
        try (RollingJsonlWriter writer = new RollingJsonlWriter(target, 20, 3)) {
            writer.appendLine("{\"g\":1}");
            writer.appendLine("{\"g\":2}");
            writer.appendLine("{\"g\":3}");
            writer.appendLine("{\"g\":4}");
            writer.appendLine("{\"g\":5}");
        }
        assertThat(tmp.resolve("plain.jsonl.1")).exists();
        assertThat(tmp.resolve("plain.jsonl.2")).exists(); // 明文——默认零变化
        assertThat(tmp.resolve("plain.jsonl.2.gz")).doesNotExist();
    }

    @Test
    void oldestGenerationClearedInBothForms() throws Exception {
        Path target = tmp.resolve("roll.jsonl");
        // 残留一个更老的 .gz（压缩线调整/重开残留场景）
        Files.writeString(tmp.resolve("roll.jsonl.3.gz"), "stale",
                StandardCharsets.UTF_8);
        try (RollingJsonlWriter writer = new RollingJsonlWriter(target, 20, 3, 2)) {
            writer.appendLine("{\"a\":1}");
            writer.appendLine("{\"a\":2}");
            writer.appendLine("{\"a\":3}");   // 轮转：gen3 双形态清理
        }
        assertThat(tmp.resolve("roll.jsonl.3.gz")).doesNotExist(); // 双形态清理
        assertThat(tmp.resolve("roll.jsonl.3")).doesNotExist();
    }

    @Test
    void staticRotateIfNeededHonorsCompressLine() throws Exception {
        Path target = tmp.resolve("static.jsonl");
        Files.writeString(target, "{\"s\":1}".repeat(10), StandardCharsets.UTF_8);

        int rotated = RollingJsonlWriter.rotateIfNeeded(target, 10, 50, 3, 2);

        assertThat(rotated).isEqualTo(1);
        assertThat(tmp.resolve("static.jsonl.2.gz")).doesNotExist(); // 只轮一代——file.1 明文
        assertThat(tmp.resolve("static.jsonl.1")).exists();

        // 第二次：file.1 → file.2 跨线转码
        Files.writeString(target, "{\"s\":2}".repeat(10), StandardCharsets.UTF_8);
        RollingJsonlWriter.rotateIfNeeded(target, 10, 50, 3, 2);
        assertThat(tmp.resolve("static.jsonl.2.gz")).exists();
        assertThat(gunzip(tmp.resolve("static.jsonl.2.gz"))).contains("\"s\":1");
    }

    @Test
    void compressLineOneRejected() {
        assertThatThrownBy(() -> new RollingJsonlWriter(tmp.resolve("x.jsonl"), 50, 3, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delaycompress");
    }
}
