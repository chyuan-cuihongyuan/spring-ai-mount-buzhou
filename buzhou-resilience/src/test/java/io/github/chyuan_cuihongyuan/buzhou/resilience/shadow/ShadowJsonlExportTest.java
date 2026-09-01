package io.github.chyuan_cuihongyuan.buzhou.resilience.shadow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 309 / impl-332：影子对照 JSONL 导出回归——行格式 / 节选封顶 /
 * 非目标事件忽略 / IO 失败吞计不放大。
 */
class ShadowJsonlExportTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    private SessionEvent compared(Map<String, Object> payload) {
        return new SessionEvent(ShadowTrafficController.EVENT_COMPARED, payload, Instant.now());
    }

    @Test
    void writesJsonlLinePerComparedEvent() throws IOException {
        Path file = tempDir.resolve("shadow-detail.jsonl");
        ShadowComparisonJsonl exporter = new ShadowComparisonJsonl(file);
        exporter.onEvent(compared(Map.of(
                "primary", "main-model", "shadow", "shadow-model",
                "primaryMs", 120L, "shadowMs", 200L, "deltaMs", 80L, "tokens", 42)));
        exporter.onEvent(compared(Map.of(
                "primary", "main-model", "shadow", "shadow-model",
                "primaryMs", 90L, "shadowMs", 80L, "deltaMs", -10L, "tokens", 7)));
        exporter.close();

        var lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        JsonNode first = MAPPER.readTree(lines.get(0));
        assertThat(first.get("primary").asText()).isEqualTo("main-model");
        assertThat(first.get("shadowMs").asLong()).isEqualTo(200L);
        assertThat(first.get("tokens").asLong()).isEqualTo(42);
        assertThat(first.get("at")).isNotNull();
        assertThat(exporter.written()).isEqualTo(2);
    }

    @Test
    void excerptCapsLongStringValues() throws IOException {
        Path file = tempDir.resolve("cap.jsonl");
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < ShadowComparisonJsonl.EXCERPT_CAP + 500; i++) {
            longText.append('x');
        }
        ShadowComparisonJsonl exporter = new ShadowComparisonJsonl(file);
        exporter.onEvent(compared(Map.of("primary", longText.toString(),
                "shadowMs", 1L)));
        exporter.close();

        JsonNode line = MAPPER.readTree(Files.readString(file));
        assertThat(line.get("primary").asText()).hasSize(ShadowComparisonJsonl.EXCERPT_CAP);
    }

    @Test
    void ignoresOtherEventTypes() throws IOException {
        Path file = tempDir.resolve("ignore.jsonl");
        ShadowComparisonJsonl exporter = new ShadowComparisonJsonl(file);
        exporter.onEvent(new SessionEvent("turn.completed", Map.of("sessionId", "s"), Instant.now()));
        exporter.close();

        assertThat(Files.exists(file)).isTrue();
        assertThat(Files.readString(file)).isEmpty();
        assertThat(exporter.written()).isZero();
    }

    @Test
    void ioFailureAfterCloseIsSwallowedAndCounted() throws IOException {
        Path file = tempDir.resolve("closed.jsonl");
        ShadowComparisonJsonl exporter = new ShadowComparisonJsonl(file);
        exporter.close();
        // 句柄已关：写入失败应吞 + 计数（旁路语义不放大），不抛
        exporter.onEvent(compared(Map.of("shadowMs", 1L)));
        assertThat(exporter.failed()).isEqualTo(1);
        assertThat(exporter.written()).isZero();
    }
}
