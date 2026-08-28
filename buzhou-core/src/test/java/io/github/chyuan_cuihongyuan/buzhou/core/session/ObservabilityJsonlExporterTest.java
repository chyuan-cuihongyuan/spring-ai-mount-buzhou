package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 观测 OLAP JSONL 导出红队（spec 60 §B / T270）：JSON Lines 规范合规——每行独立解析
 * 回等值对象、换行/引号负载转义、空集空输出、坏值列降级 + skipped 计数、全量分页覆盖、
 * 字段序稳定（两次导出字节级一致）、duration_ms 派生。
 */
class ObservabilityJsonlExporterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant T0 = Instant.parse("2026-08-29T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-08-29T00:00:02Z");

    private final InMemoryObservabilityStore store = new InMemoryObservabilityStore();
    private final ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(store);

    private static SpanRecord span(String id, String parent, String session, int turn,
            String name, Instant start, Instant end, Map<String, Object> attrs) {
        return new SpanRecord(id, parent, session, turn, "MODEL", name, start, end, "OK", attrs);
    }

    @Test
    void eachLineParsesIndependentlyBackToEquivalentFields() throws Exception {
        store.saveSpans(List.of(span("s1", null, "sess-a", 1, "chat", T0, T1,
                Map.of("model", "gpt-x"))));
        store.saveEvents(List.of(new EventRecord("e1", "s1", "sess-a", "thinking",
                T0.plusMillis(500), Map.of("delta", 3))));

        StringWriter out = new StringWriter();
        ObservabilityJsonlExporter.JsonlExportResult result = exporter.exportSession("sess-a", out);

        String[] lines = out.toString().split("\n", -1);
        assertThat(lines).hasSize(3); // span + event + 末行换行后的空尾
        assertThat(result.spans()).isEqualTo(1);
        assertThat(result.events()).isEqualTo(1);
        assertThat(result.skipped()).isZero();

        JsonNode spanLine = MAPPER.readTree(lines[0]);
        assertThat(spanLine.get("spanId").asText()).isEqualTo("s1");
        assertThat(spanLine.get("sessionId").asText()).isEqualTo("sess-a");
        assertThat(spanLine.get("turnSeq").asInt()).isEqualTo(1);
        assertThat(spanLine.get("startedAt").asText()).isEqualTo("2026-08-29T00:00:00Z");
        assertThat(spanLine.get("duration_ms").asLong()).isEqualTo(2000L); // 派生列
        assertThat(spanLine.get("attributes").get("model").asText()).isEqualTo("gpt-x");

        JsonNode eventLine = MAPPER.readTree(lines[1]);
        assertThat(eventLine.get("eventId").asText()).isEqualTo("e1");
        assertThat(eventLine.get("occurredAt").asText()).isEqualTo("2026-08-29T00:00:00.500Z");
        assertThat(eventLine.get("payload").get("delta").asInt()).isEqualTo(3);
    }

    /** 值内换行/引号/unicode 转义合规：一行一记录不被负载内的 \n 撕开。 */
    @Test
    void newlinesInPayloadDoNotBreakLineStructure() throws Exception {
        store.saveSpans(List.of(span("s1", null, "sess-a", 1, "多行\n名字\"引号\"",
                T0, T1, Map.of("text", "第一行\n第二行尾随\"引号\""))));

        StringWriter out = new StringWriter();
        exporter.exportSpans("sess-a", out);

        String content = out.toString();
        String[] lines = content.split("\n", -1);
        // 负载内的 \n 已转义为 \\n：物理行数 = 记录数 + 末换行空尾
        assertThat(lines).hasSize(2);
        JsonNode parsed = MAPPER.readTree(lines[0]); // 每行独立可解析
        assertThat(parsed.get("name").asText()).isEqualTo("多行\n名字\"引号\"");
        assertThat(parsed.get("attributes").get("text").asText())
                .isEqualTo("第一行\n第二行尾随\"引号\"");
    }

    /** 空会话导出 = 空输出（零行）；结果计数全零。 */
    @Test
    void emptySessionExportsNothing() throws Exception {
        StringWriter out = new StringWriter();
        ObservabilityJsonlExporter.JsonlExportResult result = exporter.exportSession("nobody", out);
        assertThat(out.toString()).isEmpty();
        assertThat(result.spans()).isZero();
        assertThat(result.events()).isZero();
    }

    /** 坏值列降级：不可序列化 attributes → 该列 null + skipped 计数，行仍完整可解析。 */
    @Test
    void unserializableAttributeDegradesColumnNotRow() throws Exception {
        Object bad = new Object() { // Jackson 无序列化器的匿名类型
            @Override
            public String toString() {
                return "opaque";
            }
        };
        store.saveSpans(List.of(span("s1", null, "sess-a", 1, "chat", T0, T1,
                Map.of("good", 1, "bad", bad))));

        StringWriter out = new StringWriter();
        ObservabilityJsonlExporter.JsonlExportResult result = exporter.exportSpans("sess-a", out);

        assertThat(result.skipped()).isEqualTo(1);
        JsonNode parsed = MAPPER.readTree(out.toString().stripTrailing());
        assertThat(parsed.get("spanId").asText()).isEqualTo("s1"); // 行完整
        assertThat(parsed.get("attributes").isNull()).isTrue(); // 坏列降级 null
    }

    /** 全量导出：多会话分页耗尽全覆盖；跨会话行序连续。 */
    @Test
    void exportAllCoversEverySessionViaPaging() throws Exception {
        for (int i = 0; i < 7; i++) {
            store.saveSpans(List.of(span("s-" + i, null, "sess-" + i, 1, "chat", T0, T1, Map.of())));
            store.saveEvents(List.of(new EventRecord("e-" + i, "s-" + i, "sess-" + i,
                    "reply", T0, Map.of())));
        }
        // 分页路径：页面小于/等于/大于页大小都收敛（内部页 100，7 会话单页即尽）
        StringWriter out = new StringWriter();
        ObservabilityJsonlExporter.JsonlExportResult result = exporter.exportAll(out);

        assertThat(result.sessions()).isEqualTo(7);
        assertThat(result.spans()).isEqualTo(7);
        assertThat(result.events()).isEqualTo(7);
        long lineCount = out.toString().stripTrailing().isEmpty() ? 0
                : out.toString().stripTrailing().split("\n").length;
        assertThat(lineCount).isEqualTo(14); // 7 span + 7 event
    }

    /** 字段序稳定：同输入两次导出字节级一致（追加装载/diff 不漂移）。 */
    @Test
    void fieldOrderIsStableAcrossRuns() throws Exception {
        store.saveSpans(List.of(span("s1", "p", "sess-a", 2, "tool", T0, T1,
                Map.of("z", 1, "a", 2))));
        StringWriter first = new StringWriter();
        StringWriter second = new StringWriter();
        exporter.exportSpans("sess-a", first);
        exporter.exportSpans("sess-a", second);
        assertThat(first.toString()).isEqualTo(second.toString());

        // 首个字段 = spanId（序钉住）
        assertThat(first.toString()).startsWith("{\"spanId\":\"s1\"");
    }

    /** 未关闭 span（endedAt null）：duration_ms 为 null 不炸。 */
    @Test
    void openSpanHasNullDuration() throws Exception {
        store.saveSpans(List.of(span("s1", null, "sess-a", 1, "chat", T0, null, Map.of())));
        StringWriter out = new StringWriter();
        exporter.exportSpans("sess-a", out);
        JsonNode parsed = MAPPER.readTree(out.toString().stripTrailing());
        assertThat(parsed.get("duration_ms").isNull()).isTrue();
        assertThat(parsed.get("endedAt").isNull()).isTrue();
    }
}
