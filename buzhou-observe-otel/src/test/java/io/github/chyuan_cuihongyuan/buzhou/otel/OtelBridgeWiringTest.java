package io.github.chyuan_cuihongyuan.buzhou.otel;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.SynchronousObservabilityPipeline;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 OTel sink 经真实采集管线（{@link SynchronousObservabilityPipeline} + {@link SpanHandle}）
 * 接入：{@code BaseSpanRecorder.enqueue} → {@code dispatchToSinks} → {@link OtelBridgeSink}。
 *
 * <p>区别于 {@link OtelBridgeMappingTest}（直接喂 {@code SpanRecord}），本测试走 {@code recorder.openSpan()}
 * 真实句柄生命周期，证明「Span/Event 落库的同时」旁路生效（spec 03「OTel 导出桥」）。
 *
 * <p>用内联 {@link RecordingStore} 而非 {@code core.internal.memory.InMemoryObservabilityStore}——
 * 后者位于 {@code internal} 子包，跨模块禁止引用（09 模块工程档）。
 */
class OtelBridgeWiringTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private OtelBridge bridge;

    @AfterEach
    void tearDown() {
        if (bridge != null) {
            bridge.close();
        }
    }

    @Test
    void sinkReceivesRecordsThroughRealPipelineLifecycle() {
        bridge = OtelBridge.withExporter(exporter, OtelBridgeConfig.enabledDefaults());
        var store = new RecordingStore();
        var config = ObservabilityConfig.testDefaults();
        // 关键：sink 经全参构造注入管线
        var recorder = new SynchronousObservabilityPipeline(store, config, MicrometerDualWriter.NOOP,
                List.of(bridge.sink()));

        SpanContext sessionCtx = new SpanContext("session-1", "sess-A", -1);
        // SESSION 为根：parent=null，自定 spanId/sessionId 经 explicitContext 下传
        try (SpanHandle session = recorder.openSpan(SpanKind.SESSION, "session", null, Map.of(), sessionCtx)) {
            session.attribute("agent.name", "agent-a");
            SpanContext turnCtx = new SpanContext("turn-1", "sess-A", 1);
            SpanHandle turn = recorder.openSpan(SpanKind.TURN, "turn", sessionCtx, Map.of(), turnCtx);
            turn.attribute("turn.seq", 1);
            recorder.emit(turnCtx, EventType.FINAL_REPLY, Map.of("content", "hi", "finish_reason", "stop"));
            turn.close();
        }

        var finished = exporter.getFinishedSpanItems();
        assertThat(finished).hasSize(2);
        assertThat(finished).extracting(sd -> sd.getName())
                .containsExactlyInAnyOrder("buzhou.session", "buzhou.turn");

        var sessionSpan = finished.stream().filter(sd -> sd.getName().equals("buzhou.session")).findFirst().orElseThrow();
        var turnSpan = finished.stream().filter(sd -> sd.getName().equals("buzhou.turn")).findFirst().orElseThrow();
        // 父子链 + 同 trace（经 openSpans 映射，非合成根）
        assertThat(turnSpan.getParentSpanId()).isEqualTo(sessionSpan.getSpanId());
        assertThat(turnSpan.getTraceId()).isEqualTo(sessionSpan.getTraceId());
        // 落库的同时旁路：FINAL_REPLY event 已挂到 turn span（include-content=false 故 content 丢弃）
        assertThat(turnSpan.getEvents()).hasSize(1);
        assertThat(turnSpan.getEvents().get(0).getName()).isEqualTo("FINAL_REPLY");
        assertThat(turnSpan.getEvents().get(0).getAttributes().get(AttributeKey.stringKey("finish_reason"))).isEqualTo("stop");
        assertThat(turnSpan.getEvents().get(0).getAttributes().get(AttributeKey.stringKey("content"))).isNull();
        // 双写：store 与 OTel 同时落库（in-memory store 按 append，含 RUNNING+终态记录）
        assertThat(store.spansOfSession("sess-A")).extracting(SpanRecord::spanId)
                .contains("session-1", "turn-1");
    }

    @Test
    void disabledConfigAttachesNoSinkMeansZeroExport() {
        // 契约：otel.enabled=false 时装配侧不接 sink → 主链路零开销、零导出（落库照常）
        var store = new RecordingStore();
        var config = ObservabilityConfig.testDefaults();
        // 模拟装配侧判定：enabled=false → 不创建 bridge、传空 sink 列表
        var recorder = new SynchronousObservabilityPipeline(store, config, MicrometerDualWriter.NOOP, List.of());

        SpanContext ctx = new SpanContext("s", "sess", -1);
        try (SpanHandle ignored = recorder.openSpan(SpanKind.SESSION, "session", null, Map.of(), ctx)) {
            // 主链路正常落库
        }
        assertThat(exporter.getFinishedSpanItems()).isEmpty();
        assertThat(store.spansOfSession("sess")).extracting(SpanRecord::spanId).contains("s");
    }

    /** 最小 ObservabilityStore 桩：仅落库 span 供双写断言，其余读方法返回空。 */
    static final class RecordingStore implements ObservabilityStore {
        private final CopyOnWriteArrayList<SpanRecord> spans = new CopyOnWriteArrayList<>();

        @Override
        public void saveSpans(List<SpanRecord> spans) {
            this.spans.addAll(spans);
        }

        @Override
        public List<SpanRecord> spansOfSession(String sessionId) {
            return spans.stream().filter(s -> s.sessionId().equals(sessionId)).toList();
        }

        @Override
        public void saveEvents(List<EventRecord> events) {
            // no-op
        }

        @Override
        public List<EventRecord> eventsOfSession(String sessionId) {
            return List.of();
        }

        @Override
        public List<EventRecord> eventsOfSpan(String spanId) {
            return List.of();
        }

        @Override
        public void saveInjectionSnapshot(InjectionSnapshot snapshot) {
            // no-op
        }

        @Override
        public Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq) {
            return Optional.empty();
        }

        @Override
        public List<SessionSummary> listSessionSummaries(String cursor, int size) {
            return List.of();
        }
    }
}
