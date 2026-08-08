package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用 SpanRecorder：内存记录 span（kind/name/attrs/状态）与 event（type/payload），
 * 供断言热更事件链路。同步直写、线程安全。
 */
public final class RecordingSpanRecorder implements SpanRecorder {

    public record SpanRecordView(String kind, String name, Map<String, Object> attributes,
                                 String status) {
    }

    public record EventView(String type, Map<String, Object> payload) {
    }

    private final List<SpanRecordView> spans = new CopyOnWriteArrayList<>();
    private final List<EventView> events = new CopyOnWriteArrayList<>();

    @Override
    public SpanHandle openSpan(String kind, String name, SpanContext parent) {
        return openSpan(kind, name, parent, Map.of());
    }

    @Override
    public SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes) {
        return openSpan(kind, name, parent, attributes, null);
    }

    @Override
    public SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes,
                               SpanContext explicitContext) {
        SpanContext ctx = new SpanContext(UUID.randomUUID().toString(), "unknown", 0);
        Map<String, Object> attrs = new ConcurrentHashMap<>(attributes);
        RecordingHandle handle = new RecordingHandle(ctx, attrs);
        spans.add(new SpanRecordView(kind, name, attrs, "RUNNING"));
        return handle;
    }

    @Override
    public void emit(SpanContext span, String type, Map<String, Object> payload) {
        events.add(new EventView(type, payload == null ? Map.of() : Map.copyOf(payload)));
    }

    @Override
    public void flush() {
    }

    public List<SpanRecordView> spans() {
        return spans;
    }

    public List<EventView> events() {
        return events;
    }

    public List<EventView> eventsOf(String type) {
        return events.stream().filter(e -> e.type().equals(type)).toList();
    }

    private final class RecordingHandle implements SpanHandle {
        private final SpanContext ctx;
        private final Map<String, Object> attrs;

        RecordingHandle(SpanContext ctx, Map<String, Object> attrs) {
            this.ctx = ctx;
            this.attrs = attrs;
        }

        @Override
        public SpanContext context() {
            return ctx;
        }

        @Override
        public SpanHandle attribute(String key, Object value) {
            attrs.put(key, value);
            return this;
        }

        @Override
        public SpanHandle attributes(Map<String, Object> attributes) {
            attrs.putAll(attributes);
            return this;
        }

        @Override
        public void error(Throwable t) {
            attrs.put("error", t.getClass().getSimpleName());
        }

        @Override
        public void close(String status) {
            attrs.put("status", status);
        }

        @Override
        public void close() {
            attrs.put("status", "OK");
        }
    }
}
