package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SpanRecorder 抽象基类：构造 span handle 与 event 记录的公共逻辑，子类决定落库管线
 * （{@link AsyncObservabilityPipeline} 异步批量 / {@link SynchronousObservabilityPipeline} 同步直写）。
 */
public abstract class BaseSpanRecorder implements SpanRecorder {

    protected final MicrometerDualWriter meters;
    protected final boolean includeStacktrace;

    protected BaseSpanRecorder(MicrometerDualWriter meters, boolean includeStacktrace) {
        this.meters = meters == null ? MicrometerDualWriter.NOOP : meters;
        this.includeStacktrace = includeStacktrace;
    }

    /** 供 advisor 等采集方直接访问 Micrometer 双写器（buzhou.tokens 等）。 */
    public MicrometerDualWriter meters() {
        return meters;
    }

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
        SpanContext ctx = explicitContext != null ? explicitContext
                : DefaultSpanHandle.newContext(parent != null ? parent.sessionId() : "unknown", 0);
        if (parent != null && explicitContext == null) {
            ctx = new SpanContext(ctx.spanId(), parent.sessionId(), parent.turnSeq());
        }
        return new DefaultSpanHandle(kind, name, parent, ctx, attributes, this, meters, includeStacktrace);
    }

    @Override
    public void emit(SpanContext span, String type, Map<String, Object> payload) {
        String resolved = EventType.of(type);
        EventRecord record = new EventRecord(
                UUID.randomUUID().toString(),
                span == null ? null : span.spanId(),
                span == null ? "unknown" : span.sessionId(),
                resolved,
                Instant.now(),
                payload == null ? Map.of() : Map.copyOf(payload));
        enqueue(new PendingEvent(record));
        meters.recordEvent(resolved);
    }

    /** 子类实现：把待落库项推入管线（异步入队 / 同步直写）。 */
    public abstract void enqueue(PendingItem item);

    /** 子类实现：强制把在途记录落库。 */
    @Override
    public abstract void flush();
}
