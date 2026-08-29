package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 观测数据 OLAP JSONL 导出（spec 60 / T269 / effort#20）：spans/events 平铺为
 * 「一行一个独立 JSON 对象」的 JSON Lines（Langfuse/Helicone 摄取面思想）——DuckDB /
 * ClickHouse {@code read_json_auto} 直接装载做跨会话分析（最慢模型/工具失败率/turn 深度）。
 *
 * <p><b>行形态</b>（字段序稳定——同结构 diff 与追加装载不漂移）：
 * <pre>
 * span : {"spanId":...,"parentSpanId":...,"sessionId":...,"turnSeq":...,"kind":...,
 *         "name":...,"startedAt":...,"endedAt":...,"status":...,"duration_ms":...,"attributes":{...}}
 * event: {"eventId":...,"spanId":...,"sessionId":...,"type":...,"occurredAt":...,"payload":{...}}
 * </pre>
 * 时间 ISO-8601 字符串；{@code duration_ms} 派生列（endedAt-startedAt；未关闭 span 为 null）。
 *
 * <p><b>合规与容错</b>：序列化走 Jackson JsonGenerator（字符串内换行天然转义——绝不手工
 * 拼接，保证一行一记录）；单条 attributes/payload 含不可序列化值 → 该条目丢该列 +
 * {@link JsonlExportResult#skipped()} 计数，不阻断整体导出。
 *
 * <p><b>诚实边界</b>：快照语义（运行中会话 = 当前已落库部分）；全量导出经
 * {@code listSessionSummaries} 分页耗尽驱动（最终一致视图）。
 *
 * @since 1.0.0
 */
public final class ObservabilityJsonlExporter {

    /** 导出结果（会话数 / span 行 / event 行 / 坏值降级条目；waterline = 增量水位）。 */
    public record JsonlExportResult(int sessions, long spans, long events, long skipped,
                                    Instant waterline) {

        /** spec 60 兼容 4 参构造（全量导出——无水位语义）。 */
        public JsonlExportResult(int sessions, long spans, long events, long skipped) {
            this(sessions, spans, events, skipped, null);
        }
    }

    /** 会话枚举分页大小（全量导出内步进；对导出结果无语义影响）。 */
    private static final int SESSION_PAGE_SIZE = 100;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final System.Logger LOGGER = System.getLogger(ObservabilityJsonlExporter.class.getName());

    private final ObservabilityStore store;

    public ObservabilityJsonlExporter(ObservabilityStore store) {
        this.store = store;
    }

    /** 单会话导出（spans 全量 + events 全量，span 行在前）。 */
    public JsonlExportResult exportSession(String sessionId, Writer out) throws IOException {
        JsonlExportResult spans = exportSpans(sessionId, out);
        JsonlExportResult events = exportEvents(sessionId, out);
        return new JsonlExportResult(1, spans.spans(), events.events(),
                spans.skipped() + events.skipped());
    }

    /** 单会话 span 行导出。 */
    public JsonlExportResult exportSpans(String sessionId, Writer out) throws IOException {
        long skipped = 0;
        List<SpanRecord> spans = store.spansOfSession(sessionId);
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (SpanRecord s : spans) {
                skipped += writeSpanLine(gen, s);
            }
        }
        return new JsonlExportResult(1, spans.size(), 0, skipped);
    }

    /** 单会话 event 行导出。 */
    public JsonlExportResult exportEvents(String sessionId, Writer out) throws IOException {
        long skipped = 0;
        List<EventRecord> events = store.eventsOfSession(sessionId);
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (EventRecord e : events) {
                skipped += writeEventLine(gen, e);
            }
        }
        return new JsonlExportResult(1, 0, events.size(), skipped);
    }

    /** 全量导出：会话枚举分页耗尽，逐会话 spans+events（会话序连续；返回累计结果）。 */
    public JsonlExportResult exportAll(Writer out) throws IOException {
        return export(out, null);
    }

    /**
     * 增量导出（spec 67 §A / T285，Langfuse cursor 水位语义）：只导出
     * lastActivityAt ≥ since 的会话；返回 waterline = 本次导出观测到的最大
     * activityAt（空结果 = since 原样）。会话粒度 at-least-once——有新数据的会话
     * 全量重导（OLAP 端按 spanId/eventId 主键 upsert）。
     */
    public JsonlExportResult exportAllSince(Writer out, Instant since) throws IOException {
        return export(out, since == null ? Instant.EPOCH : since);
    }

    /**
     * gzip 全量导出（spec 109 §A / T397）：GZIP 压缩流内写 UTF-8 JSONL——
     * 跨环境搬运（对象存储归档 / 跨网传输）体积降一个量级；行内容与未压缩面
     * 逐字节一致（同一 Writer 管线）。调用方负责关流（压缩流完整性由 close 收尾）。
     */
    public JsonlExportResult exportAllGzip(java.io.OutputStream out) throws IOException {
        try (java.io.Writer writer = new java.io.OutputStreamWriter(
                new java.util.zip.GZIPOutputStream(out), java.nio.charset.StandardCharsets.UTF_8)) {
            return exportAll(writer);
        }
    }

    /** gzip 增量导出（spec 67 水位语义 + spec 109 压缩面）。 */
    public JsonlExportResult exportAllSinceGzip(java.io.OutputStream out, Instant since)
            throws IOException {
        try (java.io.Writer writer = new java.io.OutputStreamWriter(
                new java.util.zip.GZIPOutputStream(out), java.nio.charset.StandardCharsets.UTF_8)) {
            return exportAllSince(writer, since);
        }
    }

    private JsonlExportResult export(Writer out, Instant since) throws IOException {
        int sessions = 0;
        long spans = 0;
        long events = 0;
        long skipped = 0;
        Instant waterline = since;
        String cursor = null;
        int seen = 0; // 枚举偏移（含被水位过滤跳过的会话——游标按页位置推进）
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            while (true) {
                List<SessionSummary> page = store.listSessionSummaries(cursor, SESSION_PAGE_SIZE);
                if (page.isEmpty()) {
                    break;
                }
                for (SessionSummary summary : page) {
                    seen++;
                    if (since != null && (summary.lastActivityAt() == null
                            || summary.lastActivityAt().isBefore(since))) {
                        continue; // 水位过滤：活跃早于 since 的会话跳过
                    }
                    for (SpanRecord s : store.spansOfSession(summary.sessionId())) {
                        skipped += writeSpanLine(gen, s);
                        spans++;
                    }
                    for (EventRecord e : store.eventsOfSession(summary.sessionId())) {
                        skipped += writeEventLine(gen, e);
                        events++;
                    }
                    sessions++;
                    if (since != null && summary.lastActivityAt() != null
                            && (waterline == null || summary.lastActivityAt().isAfter(waterline))) {
                        waterline = summary.lastActivityAt(); // 水位只在增量路径追踪
                    }
                }
                if (page.size() < SESSION_PAGE_SIZE) {
                    break; // 末页
                }
                cursor = String.valueOf(seen); // offset 语义游标（契约：不透明字符串）
            }
        }
        return new JsonlExportResult(sessions, spans, events, skipped, waterline);
    }

    // ---- 行写入（字段序稳定；返回坏值降级条目数 0/1） ----

    private static int writeSpanLine(JsonGenerator gen, SpanRecord s) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("spanId", s.spanId());
        gen.writeStringField("parentSpanId", s.parentSpanId());
        gen.writeStringField("sessionId", s.sessionId());
        gen.writeNumberField("turnSeq", s.turnSeq());
        gen.writeStringField("kind", s.kind());
        gen.writeStringField("name", s.name());
        gen.writeStringField("startedAt", iso(s.startedAt()));
        gen.writeStringField("endedAt", iso(s.endedAt()));
        gen.writeStringField("status", s.status());
        if (s.startedAt() != null && s.endedAt() != null) {
            gen.writeNumberField("duration_ms", Duration.between(s.startedAt(), s.endedAt()).toMillis());
        } else {
            gen.writeNullField("duration_ms");
        }
        int skipped = writeMapField(gen, "attributes", s.attributes());
        gen.writeEndObject();
        gen.writeRaw('\n'); // 行分隔（对象值内的换行已由 Jackson 转义）
        return skipped;
    }

    private static int writeEventLine(JsonGenerator gen, EventRecord e) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("eventId", e.eventId());
        gen.writeStringField("spanId", e.spanId());
        gen.writeStringField("sessionId", e.sessionId());
        gen.writeStringField("type", e.type());
        gen.writeStringField("occurredAt", iso(e.occurredAt()));
        int skipped = writeMapField(gen, "payload", e.payload());
        gen.writeEndObject();
        gen.writeRaw('\n');
        return skipped;
    }

    /** 对象列写入；不可序列化值先离线探（valueToTree）→ 失败整列降级 null + 计数（不阻断、不留半写行）。 */
    private static int writeMapField(JsonGenerator gen, String fieldName,
            java.util.Map<String, Object> values) throws IOException {
        com.fasterxml.jackson.databind.JsonNode node;
        try {
            node = MAPPER.valueToTree(values);
        } catch (RuntimeException bad) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "观测导出条目 " + fieldName + " 含不可序列化值，该列降级为 null（" + bad + "）");
            gen.writeNullField(fieldName);
            return 1;
        }
        gen.writeFieldName(fieldName);
        gen.writeTree(node);
        return 0;
    }

    private static String iso(Instant t) {
        return t == null ? null : t.toString();
    }
}
