package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 146 §B / T471：导出清单红队——一行一会话六列（id/首末活动/轮次/span/
 * event 计数）与数据体一致（eventCount 现算核对正是清单用途）；空库诚实零行；
 * 行独立可解析。借鉴：git pack 索引（目录与数据体分立）。
 */
class ObservabilityManifestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant T0 = Instant.parse("2026-08-29T00:00:00Z");

    private final InMemoryObservabilityStore store = new InMemoryObservabilityStore();
    private final ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(store);

    @Test
    void manifestRowsMatchDataBodyCounts() throws Exception {
        store.saveSpans(List.of(
                new SpanRecord("a1", null, "sess-a", 1, "MODEL", "chat", T0,
                        T0.plusSeconds(1), SpanStatus.OK, Map.of()),
                new SpanRecord("a2", "a1", "sess-a", 2, "TOOL", "search", T0.plusSeconds(2),
                        T0.plusSeconds(3), SpanStatus.OK, Map.of())));
        store.saveEvents(List.of(
                new EventRecord("e1", "a1", "sess-a", "thinking", T0.plusMillis(100), Map.of()),
                new EventRecord("e2", "a2", "sess-a", "tool-done", T0.plusMillis(200), Map.of()),
                new EventRecord("e3", "a2", "sess-a", "tool-done", T0.plusMillis(300), Map.of())));
        store.saveSpans(List.of(new SpanRecord("b1", null, "sess-b", 1, "MODEL", "chat",
                T0.plusSeconds(10), T0.plusSeconds(11), SpanStatus.OK, Map.of())));

        StringWriter out = new StringWriter();
        int sessions = exporter.exportManifest(out);

        assertThat(sessions).isEqualTo(2);
        String[] lines = out.toString().split("\n", -1);
        assertThat(lines).hasSize(3); // 两行 + 末行换行空尾

        // 行序随 store 摘要序（活跃度倒序）——按 id 定位不按位
        JsonNode rowA = java.util.Arrays.stream(lines)
                .filter(l -> !l.isBlank())
                .map(l -> { try { return MAPPER.readTree(l); } catch (Exception e) { throw new RuntimeException(e); } })
                .filter(n -> "sess-a".equals(n.get("sessionId").asText())).findFirst().orElseThrow();
        assertThat(rowA.get("sessionId").asText()).isEqualTo("sess-a");
        assertThat(rowA.get("spanCount").asInt()).isEqualTo(2);
        assertThat(rowA.get("eventCount").asInt()).isEqualTo(3); // 现算核对
        assertThat(rowA.get("firstActivityAt").asText()).isEqualTo("2026-08-29T00:00:00Z");
        assertThat(rowA.get("lastActivityAt").asText()).isEqualTo("2026-08-29T00:00:03Z");

        JsonNode rowB = java.util.Arrays.stream(lines)
                .filter(l -> !l.isBlank())
                .map(l -> { try { return MAPPER.readTree(l); } catch (Exception e) { throw new RuntimeException(e); } })
                .filter(n -> "sess-b".equals(n.get("sessionId").asText())).findFirst().orElseThrow();
        assertThat(rowB.get("sessionId").asText()).isEqualTo("sess-b");
        assertThat(rowB.get("spanCount").asInt()).isEqualTo(1);
        assertThat(rowB.get("eventCount").asInt()).isZero();
    }

    @Test
    void emptyStoreExportsZeroLinesHonest() throws Exception {
        StringWriter out = new StringWriter();
        assertThat(exporter.exportManifest(out)).isZero();
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void manifestColumnsStableOrder() throws Exception {
        store.saveSpans(List.of(new SpanRecord("c1", null, "sess-c", 1, "MODEL", "chat",
                T0, T0.plus(Duration.ofMillis(500)), SpanStatus.OK, Map.of())));
        StringWriter out = new StringWriter();
        exporter.exportManifest(out);
        JsonNode row = MAPPER.readTree(out.toString().split("\n")[0]);
        // 列序稳定（下游按位消费的契约面）
        java.util.List<String> fields = new java.util.ArrayList<>();
        row.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactly("sessionId", "firstActivityAt",
                "lastActivityAt", "turnCount", "spanCount", "eventCount");
    }
}
