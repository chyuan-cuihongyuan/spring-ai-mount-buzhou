package io.github.chyuan_cuihongyuan.buzhou.otel;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 驱动 {@link OtelBridgeSink} 的脚本化映射测试（spec 03「OTel 导出桥」映射规则）。
 * 用 {@link InMemorySpanExporter} hermetic 断言导出的 span 树，无需真实 Collector。
 */
class OtelBridgeMappingTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private OtelBridge bridge;

    @AfterEach
    void tearDown() {
        if (bridge != null) {
            bridge.close();
        }
    }

    private PipelineSink sink(boolean includeContent) {
        bridge = OtelBridge.withExporter(exporter, new OtelBridgeConfig(true, includeContent));
        return bridge.sink();
    }

    private static Instant t(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds);
    }

    // ---- 用例 ----

    @Test
    void fullSessionTreeMapsToSingleTraceWithCorrectNamesAndGenAiAttributes() {
        PipelineSink sink = sink(false);

        sink.onSpan(span("s1", null, SpanKind.SESSION, "session", -1, SpanStatus.RUNNING, t(100), null,
                attrs("agent.name", "agent")));
        sink.onSpan(span("t1", "s1", SpanKind.TURN, "turn", 1, SpanStatus.RUNNING, t(101), null, Map.of()));
        // 第一次模型调用（含 thinking）
        sink.onSpan(span("m1", "t1", SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.RUNNING, t(102), null,
                attrs("model.name", "gpt-4o", "model.provider", "openai",
                        "usage.prompt_tokens", 100, "usage.completion_tokens", 50, "usage.reasoning_tokens", 12,
                        "iteration", 1, "finish_reason", "tool_calls")));
        sink.onEvent(event("m1", EventType.THINKING, t(102), attrs("content", "thinking...", "provider.key", "reasoningContent")));
        sink.onSpan(span("m1", "t1", SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.OK, t(102), t(103),
                attrs("model.name", "gpt-4o", "usage.prompt_tokens", 100, "usage.completion_tokens", 50,
                        "usage.reasoning_tokens", 12, "finish_reason", "tool_calls")));
        // 工具调用
        sink.onSpan(span("tc1", "t1", SpanKind.TOOL_CALL, "tool:read_file", 1, SpanStatus.RUNNING, t(103), null,
                attrs("tool.name", "read_file", "tool.call.id", "call_1", "tool.type", "function", "tool.parallel.index", 0)));
        sink.onEvent(event("tc1", EventType.TOOL_INPUT, t(103), attrs("tool.name", "read_file", "tool.call.id", "call_1", "arguments", "{\"path\":\"/a\"}")));
        sink.onEvent(event("tc1", EventType.TOOL_OUTPUT, t(104), attrs("tool.name", "read_file", "tool.call.id", "call_1", "result", "file body", "evidence.id", "ev1")));
        sink.onSpan(span("tc1", "t1", SpanKind.TOOL_CALL, "tool:read_file", 1, SpanStatus.OK, t(103), t(104),
                attrs("tool.name", "read_file", "tool.call.id", "call_1")));
        // 内部动作
        sink.onSpan(span("h1", "t1", SpanKind.HARNESS_INTERNAL, "internal:micro-compact", 1, SpanStatus.RUNNING, t(104), null,
                attrs("internal.action", "micro-compact")));
        sink.onSpan(span("h1", "t1", SpanKind.HARNESS_INTERNAL, "internal:micro-compact", 1, SpanStatus.OK, t(104), t(104),
                attrs("internal.action", "micro-compact")));
        // 第二次模型调用（final reply）
        sink.onSpan(span("m2", "t1", SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.RUNNING, t(105), null,
                attrs("model.name", "gpt-4o", "usage.prompt_tokens", 120, "usage.completion_tokens", 80, "finish_reason", "stop")));
        sink.onEvent(event("m2", EventType.FINAL_REPLY, t(106), attrs("content", "final answer", "finish_reason", "stop")));
        sink.onSpan(span("m2", "t1", SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.OK, t(105), t(106),
                attrs("model.name", "gpt-4o", "usage.prompt_tokens", 120, "usage.completion_tokens", 80, "finish_reason", "stop")));
        // 关闭 turn / session
        sink.onSpan(span("t1", "s1", SpanKind.TURN, "turn", 1, SpanStatus.OK, t(101), t(106), attrs("turn.completed", true)));
        sink.onSpan(span("s1", null, SpanKind.SESSION, "session", -1, SpanStatus.OK, t(100), t(106), attrs("agent.name", "agent")));

        var finished = exporter.getFinishedSpanItems();
        assertThat(finished).hasSize(6);

        // 同会话同 trace
        assertThat(finished).allMatch(sd -> sd.getTraceId().equals(finished.get(0).getTraceId()));
        assertThat(finished.get(0).getTraceId()).hasSize(32).matches("[0-9a-f]{32}");

        var session = byName(finished, "buzhou.session");
        var turn = byName(finished, "buzhou.turn");
        var model = byName(finished, "chat gpt-4o"); // 两个 model call 同名，取任一
        var tool = byName(finished, "execute_tool read_file");
        var internal = byName(finished, "buzhou.internal.micro-compact");

        // 父子链：turn 挂 session；model/tool/internal 挂 turn（OTel spanId 由 SDK 生成）
        assertThat(turn.getParentSpanId()).isEqualTo(session.getSpanId());
        assertThat(model.getParentSpanId()).isEqualTo(turn.getSpanId());
        assertThat(tool.getParentSpanId()).isEqualTo(turn.getSpanId());
        assertThat(internal.getParentSpanId()).isEqualTo(turn.getSpanId());

        // 起止时间原样保真
        assertThat(session.getStartEpochNanos()).isEqualTo(nanos(t(100)));
        assertThat(session.getEndEpochNanos()).isEqualTo(nanos(t(106)));

        // gen_ai 语义约定（model）
        assertThat(model.getAttributes().get(AttributeKey.stringKey("gen_ai.operation.name"))).isEqualTo("chat");
        assertThat(model.getAttributes().get(AttributeKey.stringKey("gen_ai.request.model"))).isEqualTo("gpt-4o");
        assertThat(model.getAttributes().get(AttributeKey.longKey("gen_ai.usage.input_tokens"))).isEqualTo(100L);
        assertThat(model.getAttributes().get(AttributeKey.longKey("gen_ai.usage.output_tokens"))).isEqualTo(50L);
        // gen_ai 语义约定（tool）
        assertThat(tool.getAttributes().get(AttributeKey.stringKey("gen_ai.operation.name"))).isEqualTo("execute_tool");
        assertThat(tool.getAttributes().get(AttributeKey.stringKey("gen_ai.tool.name"))).isEqualTo("read_file");
        assertThat(tool.getAttributes().get(AttributeKey.stringKey("gen_ai.tool.call.id"))).isEqualTo("call_1");
        // turn_seq
        assertThat(turn.getAttributes().get(AttributeKey.longKey("buzhou.turn_seq"))).isEqualTo(1L);
        // 全部正常结束
        assertThat(finished).allMatch(sd -> sd.getStatus().getStatusCode() == StatusCode.OK);
    }

    @Test
    void differentSessionsGetDifferentTraceIds() {
        String traceA = exportSessionTrace(sink(false), "sess-A");
        exporter.reset();
        String traceB = exportSessionTrace(sink(false), "sess-B");
        assertThat(traceA).isNotEqualTo(traceB);
        assertThat(traceA).hasSize(32);
    }

    @Test
    void contentGatingRespectsIncludeContent() {
        // include-content=false：content/arguments/result 不出现在 event 属性
        PipelineSink off = sink(false);
        off.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.RUNNING, t(1), null,
                attrs("model.name", "gpt-4o")));
        off.onEvent(event("m", EventType.THINKING, t(1), attrs("content", "secret-thought", "provider.key", "k")));
        off.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.OK, t(1), t(2),
                attrs("model.name", "gpt-4o")));
        var offEvents = exporter.getFinishedSpanItems().get(0).getEvents();
        assertThat(offEvents).hasSize(1);
        assertThat(offEvents.get(0).getName()).isEqualTo("THINKING");
        assertThat(offEvents.get(0).getAttributes().get(AttributeKey.stringKey("content"))).isNull();
        assertThat(offEvents.get(0).getAttributes().get(AttributeKey.stringKey("provider.key"))).isEqualTo("k");

        // include-content=true：content 出现
        exporter.reset();
        PipelineSink on = sink(true);
        on.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.RUNNING, t(1), null,
                attrs("model.name", "gpt-4o")));
        on.onEvent(event("m", EventType.THINKING, t(1), attrs("content", "secret-thought", "provider.key", "k")));
        on.onSpan(span("m", null, SpanKind.MODEL_CALL, "model-call", 1, SpanStatus.OK, t(1), t(2),
                attrs("model.name", "gpt-4o")));
        var onEvents = exporter.getFinishedSpanItems().get(0).getEvents();
        assertThat(onEvents.get(0).getAttributes().get(AttributeKey.stringKey("content"))).isEqualTo("secret-thought");
    }

    @Test
    void statusMappingOkErrorCancelled() {
        PipelineSink sink = sink(false);
        // ERROR：伴随 ERROR event，span 附加 exception.*
        sink.onSpan(span("e", null, SpanKind.TOOL_CALL, "tool:x", 1, SpanStatus.RUNNING, t(1), null,
                attrs("tool.name", "x")));
        sink.onEvent(event("e", EventType.ERROR, t(2), attrs("exception.type", "java.lang.RuntimeException",
                "message", "boom", "stacktrace", "at ...")));
        sink.onSpan(span("e", null, SpanKind.TOOL_CALL, "tool:x", 1, SpanStatus.ERROR, t(1), t(2),
                attrs("tool.name", "x")));
        var errorSpan = exporter.getFinishedSpanItems().get(0);
        assertThat(errorSpan.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(errorSpan.getAttributes().get(AttributeKey.stringKey("exception.type"))).isEqualTo("java.lang.RuntimeException");
        assertThat(errorSpan.getAttributes().get(AttributeKey.stringKey("exception.message"))).isEqualTo("boom");
        // include-content=false 时 stacktrace 不导出
        assertThat(errorSpan.getAttributes().get(AttributeKey.stringKey("exception.stacktrace"))).isNull();

        // CANCELLED → UNSET + buzhou.cancelled=true
        exporter.reset();
        sink.onSpan(span("c", null, SpanKind.TURN, "turn", 1, SpanStatus.CANCELLED, t(1), t(2), Map.of()));
        var cancelled = exporter.getFinishedSpanItems().get(0);
        assertThat(cancelled.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
        assertThat(cancelled.getAttributes().get(AttributeKey.booleanKey("buzhou.cancelled"))).isTrue();
    }

    @Test
    void orphanTerminalSpanIsCreatedAndEnded() {
        PipelineSink sink = sink(false);
        // 直接给终态（未见 RUNNING）
        sink.onSpan(span("orphan", null, SpanKind.TURN, "turn", 1, SpanStatus.OK, t(1), t(3), Map.of()));
        var finished = exporter.getFinishedSpanItems();
        assertThat(finished).hasSize(1);
        assertThat(finished.get(0).getName()).isEqualTo("buzhou.turn");
        assertThat(finished.get(0).getStartEpochNanos()).isEqualTo(nanos(t(1)));
        assertThat(finished.get(0).getEndEpochNanos()).isEqualTo(nanos(t(3)));
    }

    @Test
    void eventForUnknownSpanIsDroppedWithoutThrowing() {
        PipelineSink sink = sink(false);
        sink.onEvent(event("nope", EventType.ERROR, t(1), attrs("message", "x")));
        assertThat(exporter.getFinishedSpanItems()).isEmpty();
    }

    // ---- helpers ----

    private String exportSessionTrace(PipelineSink sink, String sessionId) {
        sink.onSpan(new SpanRecord("root", null, sessionId, -1, SpanKind.SESSION, "session",
                t(1), t(2), SpanStatus.OK, Map.of()));
        var finished = exporter.getFinishedSpanItems();
        assertThat(finished).hasSize(1);
        return finished.get(0).getTraceId();
    }

    private static io.opentelemetry.sdk.trace.data.SpanData byName(List<io.opentelemetry.sdk.trace.data.SpanData> spans, String name) {
        return spans.stream().filter(sd -> sd.getName().equals(name)).findFirst().orElseThrow();
    }

    private static long nanos(Instant instant) {
        return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000_000L), instant.getNano());
    }

    private static SpanRecord span(String id, String parent, String kind, String name, int turnSeq,
                                   String status, Instant start, Instant end, Map<String, Object> attrs) {
        return new SpanRecord(id, parent, "sess-1", turnSeq, kind, name, start, end, status, attrs);
    }

    private static EventRecord event(String spanId, String type, Instant ts, Map<String, Object> payload) {
        return new EventRecord("evt-" + Math.abs(System.nanoTime()), spanId, "sess-1", type, ts, payload);
    }

    private static Map<String, Object> attrs(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
