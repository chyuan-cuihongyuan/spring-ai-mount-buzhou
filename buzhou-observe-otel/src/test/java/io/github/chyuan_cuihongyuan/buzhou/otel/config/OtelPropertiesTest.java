package io.github.chyuan_cuihongyuan.buzhou.otel.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OtelProperties 构造分支补测（K 会话 R8 / spec 1207 / T1822——R7 逐类分支数据精定制导）：
 * null 默认链、blank 回退、headers 防御拷贝、exporter-mode/timeout fail-fast。
 */
class OtelPropertiesTest {

    @Test
    void allNullsFallBackToDocumentedDefaults() {
        OtelProperties p = new OtelProperties(null, null, null, null, null, null);

        assertThat(p.enabled()).isFalse();
        assertThat(p.includeContent()).isFalse();
        assertThat(p.exporterMode()).isEqualTo("otlp");
        assertThat(p.endpoint()).isEqualTo(io.github.chyuan_cuihongyuan.buzhou.otel.OtelBridge.DEFAULT_OTLP_ENDPOINT);
        assertThat(p.headers()).isEmpty();
        assertThat(p.timeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void blankStringsFallBackToDefaults() {
        OtelProperties p = new OtelProperties(true, true, "  ", " ", null, null);

        assertThat(p.exporterMode()).isEqualTo("otlp");
        assertThat(p.endpoint()).isEqualTo(io.github.chyuan_cuihongyuan.buzhou.otel.OtelBridge.DEFAULT_OTLP_ENDPOINT);
        assertThat(p.enabled()).isTrue();
        assertThat(p.includeContent()).isTrue();
    }

    @Test
    void headersAreDefensivelyCopied() {
        Map<String, String> original = new HashMap<>();
        original.put("Authorization", "Bearer token");

        OtelProperties p = new OtelProperties(null, null, "otlp", null, original, null);
        original.put("mutated-after", "x");

        assertThat(p.headers()).containsOnlyKeys("Authorization").containsEntry("Authorization", "Bearer token");
    }

    @Test
    void explicitValuesArePreserved() {
        Map<String, String> headers = Map.of("k", "v");
        OtelProperties p = new OtelProperties(true, false, "tracer",
                "http://collector:4317", headers, Duration.ofSeconds(3));

        assertThat(p.exporterMode()).isEqualTo("tracer");
        assertThat(p.endpoint()).isEqualTo("http://collector:4317");
        assertThat(p.headers()).isEqualTo(headers);
        assertThat(p.timeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void illegalExporterModeFailsFast() {
        assertThatThrownBy(() -> new OtelProperties(null, null, "zipkin", null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("zipkin");
    }

    @Test
    void nonPositiveTimeoutFailsFast() {
        assertThatThrownBy(() -> new OtelProperties(null, null, "otlp", null, null, Duration.ZERO))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("buzhou.observe.otel.timeout");
        assertThatThrownBy(() -> new OtelProperties(null, null, "otlp", null, null,
                Duration.ofSeconds(-1)))
                .isInstanceOf(BuzhouConfigurationException.class);
    }
}
