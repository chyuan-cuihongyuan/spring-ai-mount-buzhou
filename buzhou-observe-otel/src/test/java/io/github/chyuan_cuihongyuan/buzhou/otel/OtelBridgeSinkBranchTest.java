package io.github.chyuan_cuihongyuan.buzhou.otel;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * OtelBridgeSink 分支补测（K 会话 R8 / spec 1207 / T1822——R7 逐类分支数据精定制导）：
 * 重复开启防御、驱逐护栏（4 参构造）、spanName 全 kind 回退、sessionTrace 回退与上界、
 * 属性类型适配器、空/缺 payload、无 spanId 事件、endedAt=null、旁路故障隔离（Proxy Tracer）。
 * 先例：OtelBridgeMappingTest（InMemorySpanExporter hermetic 断言）。
 */
class OtelBridgeSinkBranchTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final io.opentelemetry.sdk.trace.SdkTracerProvider provider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final Tracer tracer = provider.get("branch-test");

    @AfterEach
    void tearDown() {
        provider.close();
    }

    private OtelBridgeSink sink() {
        return new OtelBridgeSink(tracer, new OtelBridgeConfig(true, false));
    }

    private static SpanRecord span(String id, String parent, String kind, String name, String status,
                                   Instant start, Instant end, Map<String, Object> attrs) {
        return new SpanRecord(id, parent, "sess-1", 1, kind, name, start, end, status, attrs);
    }

    private static EventRecord event(String spanId, String type, Map<String, Object> payload) {
        return new EventRecord("evt-" + Math.abs(System.nanoTime()), spanId, "sess-1", type,
                Instant.ofEpochSecond(1), payload);
    }

    private static Map<String, Object> attrs(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static SpanData byName(List<SpanData> spans, String name) {
        return spans.stream().filter(sd -> sd.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void duplicateOpenEndsLeakedSpanDefensively() {
        OtelBridgeSink sink = sink();
        Instant t = Instant.ofEpochSecond(1);
        sink.onSpan(span("s1", null, SpanKind.SESSION, "session", SpanStatus.RUNNING, t, null, Map.of()));
        sink.onSpan(span("s1", null, SpanKind.SESSION, "session", SpanStatus.RUNNING, t, null, Map.of()));
        sink.onSpan(span("s1", null, SpanKind.SESSION, "session", SpanStatus.OK, t, t.plusSeconds(1), Map.of()));

        List<SpanData> finished = exporter.getFinishedSpanItems();
        // 防御 end 的泄漏 span + 正常终态 span
        assertThat(finished).hasSize(2);
        assertThat(sink.evictedSpans()).isZero(); // 重复开启走防御 end，不算驱逐
    }

    @Test
    void evictionGuardFiresWhenOpenSpansExceedBudget() {
        OtelBridgeSink sink = new OtelBridgeSink(tracer, new OtelBridgeConfig(true, false), 1, 10);
        Instant t = Instant.ofEpochSecond(1);
        sink.onSpan(span("a", null, SpanKind.TURN, "turn-a", SpanStatus.RUNNING, t, null, Map.of()));
        sink.onSpan(span("b", null, SpanKind.TURN, "turn-b", SpanStatus.RUNNING, t, null, Map.of()));
        sink.onSpan(span("b", null, SpanKind.TURN, "turn-b", SpanStatus.OK, t, t.plusSeconds(1), Map.of()));

        assertThat(sink.evictedSpans()).isEqualTo(1); // 护栏触发即上游 span 泄漏信号
        List<SpanData> finished = exporter.getFinishedSpanItems();
        assertThat(finished).hasSize(2);
        // TURN 类 span 名恒为 buzhou.turn——以驱逐标记区分被驱逐的那条
        SpanData evicted = finished.stream()
                .filter(sd -> Boolean.TRUE.equals(sd.getAttributes().get(AttributeKey.booleanKey("buzhou.evicted"))))
                .findFirst().orElseThrow();
        assertThat(evicted.getName()).isEqualTo("buzhou.turn");
    }

    @Test
    void spanNameFallbacksCoverAllKindBranches() {
        OtelBridgeSink sink = sink();
        Instant t = Instant.ofEpochSecond(1);
        // HARNESS_INTERNAL：无 internal.action，name 带 internal: 前缀
        sink.onSpan(span("h1", null, SpanKind.HARNESS_INTERNAL, "internal:foo.bar", SpanStatus.OK, t, t, Map.of()));
        // HARNESS_INTERNAL：无 action，name 不带前缀
        sink.onSpan(span("h2", null, SpanKind.HARNESS_INTERNAL, "plain-name", SpanStatus.OK, t, t, Map.of()));
        // HARNESS_INTERNAL：name 为 null
        sink.onSpan(span("h3", null, SpanKind.HARNESS_INTERNAL, null, SpanStatus.OK, t, t, Map.of()));
        // MODEL_CALL：无 model.name
        sink.onSpan(span("m1", null, SpanKind.MODEL_CALL, "model-call", SpanStatus.OK, t, t, Map.of()));
        // TOOL_CALL：无 tool.name
        sink.onSpan(span("tc1", null, SpanKind.TOOL_CALL, "tool", SpanStatus.OK, t, t, Map.of()));
        // 未知 kind：default → record.name()
        sink.onSpan(span("x1", null, "EVAL", "custom-eval", SpanStatus.OK, t, t, Map.of()));

        List<SpanData> finished = exporter.getFinishedSpanItems();
        assertThat(byName(finished, "buzhou.internal.foo.bar")).isNotNull();
        assertThat(byName(finished, "buzhou.internal")).isNotNull();
        assertThat(byName(finished, "chat")).isNotNull();
        assertThat(byName(finished, "execute_tool")).isNotNull();
        assertThat(byName(finished, "custom-eval")).isNotNull();
    }

    @Test
    void sessionTraceFallbacksForNullAndBlankSessionIds() {
        OtelBridgeSink sink = sink();
        Instant t = Instant.ofEpochSecond(1);
        sink.onSpan(new SpanRecord("r1", null, null, 0, SpanKind.SESSION, "s", t, t, SpanStatus.OK, Map.of()));
        sink.onSpan(new SpanRecord("r2", null, "  ", 0, SpanKind.SESSION, "s", t, t, SpanStatus.OK, Map.of()));

        List<SpanData> finished = exporter.getFinishedSpanItems();
        // null 与 blank 归并同一 unknown 会话 → 同一派生 traceId
        assertThat(finished.get(0).getTraceId()).isEqualTo(finished.get(1).getTraceId());
    }

    @Test
    void sessionTraceEvictionRebuildsLosslessly() {
        OtelBridgeSink sink = new OtelBridgeSink(tracer, new OtelBridgeConfig(true, false), 10, 1);
        Instant t = Instant.ofEpochSecond(1);
        sink.onSpan(new SpanRecord("ra", null, "sess-A", 0, SpanKind.SESSION, "s", t, t, SpanStatus.OK, Map.of()));
        sink.onSpan(new SpanRecord("rb", null, "sess-B", 0, SpanKind.SESSION, "s", t, t, SpanStatus.OK, Map.of()));
        // sess-B 挤掉 sess-A 的缓存项后 A 重建：traceId 确定性派生，驱逐无损
        sink.onSpan(new SpanRecord("ra2", null, "sess-A", 0, SpanKind.SESSION, "s", t, t, SpanStatus.OK, Map.of()));

        List<SpanData> finished = exporter.getFinishedSpanItems();
        assertThat(finished).hasSize(3);
        String traceA = finished.get(0).getTraceId();
        String traceB = finished.get(1).getTraceId();
        String traceARebuilt = finished.get(2).getTraceId();
        assertThat(traceB).isNotEqualTo(traceA);
        assertThat(traceARebuilt).isEqualTo(traceA);
    }

    @Test
    void eventTypeAdaptersMapValueTypesAndGateContentKeys() {
        OtelBridgeSink sink = sink();
        Instant t = Instant.ofEpochSecond(1);
        sink.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", SpanStatus.RUNNING, t, null, Map.of()));

        // 注：null 值经 EventRecord 的 Map.copyOf 即抛 NPE，putObject 的 null 分支为防御性不可达（R8 入档）
        Map<String, Object> payload = attrs(
                "flag", Boolean.TRUE, "ratio", 0.5f, "score", 0.75d,
                "count", 42, "total", 100L, "custom", new Object(),
                "result", "hidden-content");
        sink.onEvent(event("m", EventType.TOOL_OUTPUT, payload));
        sink.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", SpanStatus.OK, t, t.plusSeconds(1), Map.of()));

        var events = exporter.getFinishedSpanItems().get(0).getEvents();
        assertThat(events).hasSize(1);
        var a = events.get(0).getAttributes();
        assertThat(a.get(AttributeKey.booleanKey("flag"))).isTrue();
        assertThat(a.get(AttributeKey.doubleKey("ratio"))).isEqualTo(0.5d);
        assertThat(a.get(AttributeKey.doubleKey("score"))).isEqualTo(0.75d);
        assertThat(a.get(AttributeKey.longKey("count"))).isEqualTo(42L);
        assertThat(a.get(AttributeKey.longKey("total"))).isEqualTo(100L);
        // 自定义对象 String.valueOf 回退；内容键 include-content=false 被门控
        assertThat(a.get(AttributeKey.stringKey("custom"))).isNotNull();
        assertThat(a.get(AttributeKey.stringKey("result"))).isNull();
    }

    @Test
    void nullAndEmptyPayloadsAndSpanlessEventsAreDefensive() {
        OtelBridgeSink sink = sink();
        Instant t = Instant.ofEpochSecond(1);
        sink.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", SpanStatus.RUNNING, t, null, Map.of()));

        assertThatCode(() -> {
            sink.onEvent(new EventRecord("e1", "m", "sess-1", EventType.ERROR, t, null));
            sink.onEvent(new EventRecord("e2", "m", "sess-1", EventType.TOOL_OUTPUT, t, Map.of()));
            sink.onEvent(event(null, EventType.TOOL_OUTPUT, attrs("k", "v"))); // 无 spanId：静默丢弃
            sink.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", SpanStatus.OK, t, t.plusSeconds(1), null));
        }).doesNotThrowAnyException();

        List<SpanData> finished = exporter.getFinishedSpanItems();
        assertThat(finished).hasSize(1);
        assertThat(finished.get(0).getEvents()).hasSize(2); // null payload → 空属性事件仍记录
    }

    @Test
    void closeWithNullEndedAtFallsBackToNow() {
        OtelBridgeSink sink = sink();
        sink.onSpan(span("orphan", null, SpanKind.TURN, "turn", SpanStatus.OK, Instant.ofEpochSecond(1), null, Map.of()));
        SpanData finished = exporter.getFinishedSpanItems().get(0);
        assertThat(finished.getEndEpochNanos()).isPositive();
    }

    @Test
    void sinkFailureIsolationSwallowsAndRateLimits() {
        Tracer hostile = (Tracer) Proxy.newProxyInstance(Tracer.class.getClassLoader(),
                new Class<?>[]{Tracer.class},
                (p, m, a) -> {
                    if (m.getName().equals("spanBuilder")) {
                        throw new IllegalStateException("otel 爆炸");
                    }
                    return null;
                });
        OtelBridgeSink sink = new OtelBridgeSink(hostile, new OtelBridgeConfig(true, false));

        // PipelineSink 故障隔离契约：回调吞 RuntimeException 不污染主链路；
        // 100 次调用覆盖限频分支（首条 + 每 100 条一条 WARN）
        assertThatCode(() -> {
            for (int i = 0; i < 100; i++) {
                sink.onSpan(span("s", null, SpanKind.TURN, "turn", SpanStatus.RUNNING,
                        Instant.ofEpochSecond(1), null, Map.of()));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void unknownStatusDefaultsToOk() {
        OtelBridgeSink sink = sink();
        sink.onSpan(span("u", null, SpanKind.TURN, "turn", "WEIRD", Instant.ofEpochSecond(1),
                Instant.ofEpochSecond(2), Map.of()));
        SpanData finished = exporter.getFinishedSpanItems().get(0);
        assertThat(finished.getStatus().getStatusCode()).isEqualTo(StatusCode.OK);
    }
}
