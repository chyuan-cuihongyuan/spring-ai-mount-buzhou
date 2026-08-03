package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认 {@link SpanHandle}：累积属性，关闭时构造 {@link SpanRecord} 经 recorder 入队。
 *
 * <p>状态机：开启即 RUNNING 入队（dashboard 可见进行中的轮次，spec 03 推演 3 upsert 语义）；
 * 关闭时同 spanId upsert 为终态（OK 默认 / ERROR / CANCELLED）。{@link #error(Throwable)} 置 ERROR
 * 并发 ERROR Event。线程安全（同句柄可被并发工具回调持有）。
 */
public class DefaultSpanHandle implements SpanHandle {

    private final SpanContext context;
    private final String kind;
    private final String name;
    private final SpanContext parent;
    private final Instant startTime;
    private final BaseSpanRecorder recorder;
    private final MicrometerDualWriter meters;
    private final boolean includeStacktrace;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private final AtomicReference<String> status = new AtomicReference<>(SpanStatus.RUNNING);
    private volatile boolean closed;

    public DefaultSpanHandle(String kind, String name, SpanContext parent, SpanContext context,
                             Map<String, Object> initialAttributes, BaseSpanRecorder recorder,
                             MicrometerDualWriter meters, boolean includeStacktrace) {
        this.kind = kind;
        this.name = name;
        this.parent = parent;
        this.context = context;
        this.startTime = Instant.now();
        this.recorder = recorder;
        this.meters = meters;
        this.includeStacktrace = includeStacktrace;
        if (initialAttributes != null) {
            this.attributes.putAll(initialAttributes);
        }
        // 开启即落 RUNNING（upsert）
        recorder.enqueue(new PendingSpan(toRecord(SpanStatus.RUNNING, null)));
    }

    @Override
    public SpanContext context() {
        return context;
    }

    @Override
    public SpanHandle attribute(String key, Object value) {
        if (key != null) {
            synchronized (attributes) {
                attributes.put(key, value);
            }
        }
        return this;
    }

    @Override
    public SpanHandle attributes(Map<String, Object> attributes) {
        if (attributes != null) {
            synchronized (this.attributes) {
                this.attributes.putAll(attributes);
            }
        }
        return this;
    }

    @Override
    public void error(Throwable t) {
        if (closed || t == null) {
            return;
        }
        status.set(SpanStatus.ERROR);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exception.type", t.getClass().getName());
        payload.put("message", t.getMessage() == null ? "" : t.getMessage());
        if (includeStacktrace) {
            payload.put("stacktrace", stacktraceOf(t));
        }
        recorder.emit(context, io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType.ERROR, payload);
    }

    @Override
    public void close(String explicitStatus) {
        closeInternal(explicitStatus);
    }

    @Override
    public void close() {
        closeInternal(null);
    }

    private void closeInternal(String explicitStatus) {
        if (closed) {
            return;
        }
        closed = true;
        String finalStatus = status.compareAndSet(SpanStatus.RUNNING,
                explicitStatus != null ? explicitStatus : SpanStatus.OK)
                ? (explicitStatus != null ? explicitStatus : SpanStatus.OK)
                : status.get();
        Instant endTime = Instant.now();
        recorder.enqueue(new PendingSpan(toRecord(finalStatus, endTime)));
        meters.recordSpanClose(kind, name, finalStatus, withDuration(endTime));
    }

    private Map<String, Object> withDuration(Instant endTime) {
        long durationMs = Math.max(0, endTime.toEpochMilli() - startTime.toEpochMilli());
        Map<String, Object> snapshot;
        synchronized (attributes) {
            snapshot = new LinkedHashMap<>(attributes);
        }
        snapshot.put("duration.ms", durationMs);
        return snapshot;
    }

    private SpanRecord toRecord(String statusValue, Instant endTime) {
        Map<String, Object> snapshot;
        synchronized (attributes) {
            snapshot = Map.copyOf(attributes);
        }
        return new SpanRecord(
                context.spanId(),
                parent == null ? null : parent.spanId(),
                context.sessionId(),
                context.turnSeq(),
                kind,
                name,
                startTime,
                endTime,
                statusValue,
                snapshot);
    }

    private static String stacktraceOf(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    /** 生成新 SpanContext（UUID spanId）。 */
    public static SpanContext newContext(String sessionId, int turnSeq) {
        return new SpanContext(java.util.UUID.randomUUID().toString(), sessionId, turnSeq);
    }
}
