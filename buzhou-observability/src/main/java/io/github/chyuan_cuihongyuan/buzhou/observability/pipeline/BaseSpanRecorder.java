package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SpanRecorder 抽象基类：构造 span handle 与 event 记录的公共逻辑，子类决定落库管线
 * （{@link AsyncObservabilityPipeline} 异步批量 / {@link SynchronousObservabilityPipeline} 同步直写）。
 */
public abstract class BaseSpanRecorder implements SpanRecorder {

    protected final MicrometerDualWriter meters;
    protected final boolean includeStacktrace;
    private final List<PipelineSink> sinks;

    protected BaseSpanRecorder(MicrometerDualWriter meters, boolean includeStacktrace) {
        this(meters, includeStacktrace, List.of());
    }

    /**
     * 全参构造：额外传入旁路 {@link PipelineSink} 列表（OTel 导出桥等），在每次 {@link #enqueue}
     * 时按入队顺序同步回调。{@code null} 或空表视为无旁路（零开销）。
     */
    protected BaseSpanRecorder(MicrometerDualWriter meters, boolean includeStacktrace, List<PipelineSink> sinks) {
        this.meters = meters == null ? MicrometerDualWriter.NOOP : meters;
        this.includeStacktrace = includeStacktrace;
        this.sinks = sinks == null ? List.of() : List.copyOf(sinks);
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

    /**
     * 入队模板方法：先按入队顺序同步通知旁路 {@link PipelineSink}（OTel 导出桥等），再交子类落库。
     *
     * <p>在 {@code enqueue} 时刻回调而非 drain 时刻，是为了保留 open→event→close 的调用顺序——
     * 批量 drain 会把同一批 Span 与 Event 拆分重排（见 {@code AsyncObservabilityPipeline#applyBatch}），
     * 旁路消费者（如 OTel span 的 addEvent 必须在 span end 之前）依赖原始时序。
     */
    public final void enqueue(PendingItem item) {
        dispatchToSinks(item);
        doEnqueue(item);
    }

    /** 子类实现：把待落库项推入管线（异步入队 / 同步直写）。 */
    protected abstract void doEnqueue(PendingItem item);

    private void dispatchToSinks(PendingItem item) {
        if (sinks.isEmpty()) {
            return;
        }
        switch (item) {
            case PendingSpan s -> {
                SpanRecord record = s.record();
                for (PipelineSink sink : sinks) {
                    try {
                        sink.onSpan(record);
                    } catch (RuntimeException e) {
                        // 旁路故障不得污染主链路；吞异常即可（落库仍由 doEnqueue 保证）
                    }
                }
            }
            case PendingEvent e -> {
                EventRecord record = e.record();
                for (PipelineSink sink : sinks) {
                    try {
                        sink.onEvent(record);
                    } catch (RuntimeException ignored) {
                        // 同上
                    }
                }
            }
            default -> {
                // 快照 / flush token 不经过旁路 sink
            }
        }
    }

    /** 子类实现：强制把在途记录落库。 */
    @Override
    public abstract void flush();
}
