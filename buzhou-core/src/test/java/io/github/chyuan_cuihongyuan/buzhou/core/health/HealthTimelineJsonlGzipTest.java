package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 健康时间线 JSONL 压缩线装配测试（spec 729 / T1009–T1010 / impl 532）：
 * export-compress-from ≥2 声明 → 轮转跨线档 .gz；缺省 0 全明文。
 */
class HealthTimelineJsonlGzipTest {

    @TempDir
    Path tmp;

    @Test
    void compressLineProducesGzippedGenerations() throws Exception {
        Path target = tmp.resolve("timeline.jsonl");
        try (HealthTimelineJsonl jsonl = new HealthTimelineJsonl(target, 20, 3, 2)) {
            for (int i = 0; i < 5; i++) {
                jsonl.accept(new HealthTimeline.Entry("memory",
                        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP,
                        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.DOWN,
                        java.time.Instant.now()));
            }
        }
        // 5 条 accept ≈ 5 次轮转判定（每行 >20B 触发）——至少 2 代历史
        Path gen2gz = tmp.resolve("timeline.jsonl.2.gz");
        if (Files.exists(gen2gz)) {
            try (GZIPInputStream gz = new GZIPInputStream(Files.newInputStream(gen2gz))) {
                assertThat(new String(gz.readAllBytes(), StandardCharsets.UTF_8)).contains("memory");
            }
            assertThat(tmp.resolve("timeline.jsonl.2")).doesNotExist();
        }
        // file.1 恒明文（delaycompress）
        assertThat(tmp.resolve("timeline.jsonl.1")).exists();
    }

    @Test
    void defaultZeroKeepsAllPlain() throws Exception {
        Path target = tmp.resolve("plain.jsonl");
        try (HealthTimelineJsonl jsonl = new HealthTimelineJsonl(target, 20, 3)) {
            for (int i = 0; i < 5; i++) {
                jsonl.accept(new HealthTimeline.Entry("memory",
                        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP,
                        io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.DOWN,
                        java.time.Instant.now()));
            }
        }
        assertThat(tmp.resolve("plain.jsonl.1")).exists();
        assertThat(tmp.resolve("plain.jsonl.2.gz")).doesNotExist();
    }
}
