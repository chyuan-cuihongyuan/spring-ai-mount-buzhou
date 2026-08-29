package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 112 §B / T406：错误签名 JSONL 导出红队——行数 = 在册族数；count 降序
 * （与 top 同序）；逐行独立 JSON；空表零行。spec 83 fog 收口（导出四族第五员）。
 */
class ErrorSignaturesJsonlTest {

    @Test
    void exportsSignaturesAsJsonlInCountOrder() throws Exception {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "boom A");
        registry.record("tool", "boom A");
        registry.record("tool", "boom A");
        registry.record("model", "boom B");
        registry.record("model", "boom C");

        StringWriter out = new StringWriter();
        long lines = ErrorSignaturesJsonl.export(registry, out);

        assertThat(lines).isEqualTo(3);
        String[] rows = out.toString().strip().split("\n");
        assertThat(rows).hasSize(3);
        Map<String, Object> first = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(rows[0], Map.class);
        assertThat(first).containsEntry("signature", "tool:boom A").containsEntry("count", 3);
        Map<String, Object> second = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(rows[1], Map.class);
        assertThat(second.get("count")).isEqualTo(1);
        // 每行独立可解析（换行转义纪律）
        for (String row : rows) {
            assertThat(new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(row, Map.class)).containsKey("signature");
        }
    }

    @Test
    void emptyRegistryExportsNothing() throws Exception {
        StringWriter out = new StringWriter();
        assertThat(ErrorSignaturesJsonl.export(ErrorSignatures.create(), out)).isZero();
        assertThat(out.toString()).isEmpty();
    }
}
