package io.github.chyuan_cuihongyuan.buzhou.otel;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OTel 导出桥旁路 sink（spec 03「OTel 导出桥」）。
 *
 * <p>实现 {@link PipelineSink}，在 Span/Event 入队（落库前）时刻同步把认知模型映射为 OTel span：
 * <ul>
 *   <li><b>traceId 由 sessionId 派生</b>（SHA-256 取前 16 字节 = 32 hex），同会话同 trace；</li>
 *   <li>Span 开启（RUNNING 记录）→ {@code tracer.spanBuilder(...).startSpan()}，按 {@code parentSpanId}
 *       链接父 span，无已知父则挂到 sessionId 派生的合成根（保证 traceId 一致）；</li>
 *   <li>Event → {@code span.addEvent(...)}（须在 span end 之前，故在 enqueue 时刻而非 drain 时刻回调）；</li>
 *   <li>Span 关闭（终态记录）→ 写全部属性 + status + {@code end(endedAt)}（起止时间原样映射保真回放）。</li>
 * </ul>
 *
 * <p><b>线程安全</b>：同一会话的并发工具回调在不同虚拟线程上并发回调，{@code openSpans}/{@code sessionTrace}
 * 用 {@link ConcurrentHashMap}；OTel {@link Span} 本身线程安全。
 *
 * <p><b>故障隔离</b>：所有回调吞 {@link RuntimeException}（{@link PipelineSink} 契约 + {@code BaseSpanRecorder}
 * 兜底双保险），OTel 故障不污染主链路。
 */
final class OtelBridgeSink implements PipelineSink {

    private static final System.Logger LOGGER = System.getLogger(OtelBridgeSink.class.getName());

    /** 受 {@code include-content} 门控的内容型 payload key（spec 03 推演 #15：由 THINKING/FINAL_REPLY 泛化到全部内容型字段）。 */
    private static final Set<String> CONTENT_KEYS = Set.of("content", "arguments", "result", "stacktrace");

    /** impl-47：旁路故障限频日志步长（首条 + 每 N 条一条，防异常风暴刷屏）。 */
    private static final long LOG_EVERY = 100;

    private final Tracer tracer;
    private final boolean includeContent;
    private final ConcurrentHashMap<String, Span> openSpans = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SessionTrace> sessionTrace = new ConcurrentHashMap<>();
    /** impl-47：未终态 span 上限（防泄漏护栏——正常流全部 span 都有终态回调，超限即上游异常）。 */
    private final int maxOpenSpans;
    /** impl-47：会话 trace 派生缓存上限（PipelineSink 无会话结束回调，靠上界防长跑无界增长）。 */
    private final int maxSessionTraces;
    private final AtomicLong evictedSpans = new AtomicLong();
    private final AtomicLong sinkFailures = new AtomicLong();

    OtelBridgeSink(Tracer tracer, OtelBridgeConfig config) {
        this(tracer, config, 10_000, 100_000);
    }

    OtelBridgeSink(Tracer tracer, OtelBridgeConfig config, int maxOpenSpans, int maxSessionTraces) {
        this.tracer = tracer;
        this.includeContent = config.includeContent();
        this.maxOpenSpans = Math.max(1, maxOpenSpans);
        this.maxSessionTraces = Math.max(1, maxSessionTraces);
    }

    /** impl-47：被驱逐的未终态 span 数（护栏触发即上游 span 泄漏信号，暴露给健康/测试）。 */
    long evictedSpans() {
        return evictedSpans.get();
    }

    @Override
    public void onSpan(SpanRecord record) {
        try {
            if (SpanStatus.RUNNING.equals(record.status())) {
                openSpan(record);
            } else {
                closeSpan(record);
            }
        } catch (RuntimeException e) {
            logSinkFailure("onSpan", e);
        }
    }

    @Override
    public void onEvent(EventRecord record) {
        try {
            Span span = record.spanId() == null ? null : openSpans.get(record.spanId());
            if (span == null) {
                return; // 归属 span 已关闭或未知：OTel span 已 end，addEvent 无效
            }
            if (EventType.ERROR.equals(record.type())) {
                applyExceptionAttributes(span, record.payload());
            }
            span.addEvent(record.type(), eventAttributes(record.payload()),
                    epochNanos(record.occurredAt()), TimeUnit.NANOSECONDS);
        } catch (RuntimeException e) {
            logSinkFailure("onEvent", e);
        }
    }

    // ---- span 生命周期 ----

    private void openSpan(SpanRecord record) {
        Span leaked = openSpans.get(record.spanId());
        if (leaked != null) {
            leaked.end(); // 防御：同 spanId 重复开启，先结束旧 span 避免泄漏
        }
        evictIfOverBudget();
        Span span = tracer.spanBuilder(spanName(record))
                .setParent(resolveParent(record))
                .setStartTimestamp(record.startedAt())
                .startSpan();
        openSpans.put(record.spanId(), span);
    }

    /**
     * impl-47：未终态 span 超限驱逐（任意一条——护栏而非 LRU；正常流不触发，
     * 触发即上游存在 span 泄漏，计数 + 指标 + WARN 可见）。驱逐的 span 以 UNSET 终态
     * end 并标 {@code buzhou.evicted=true}（trace 不悬挂）。
     */
    private void evictIfOverBudget() {
        while (openSpans.size() >= maxOpenSpans && !openSpans.isEmpty()) {
            var iterator = openSpans.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            var entry = iterator.next();
            iterator.remove();
            try {
                entry.getValue().setAttribute("buzhou.evicted", true);
                entry.getValue().end();
            } catch (RuntimeException ignored) {
                // 驱逐路径尽力而为
            }
            long total = evictedSpans.incrementAndGet();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.otel.span-evictions");
            if (total == 1 || total % LOG_EVERY == 0) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "otel 桥未终态 span 超上限（" + maxOpenSpans + "），已驱逐 " + total
                                + " 条——上游疑似 span 泄漏（流取消/异常路径缺终态）");
            }
        }
    }

    private void closeSpan(SpanRecord record) {
        Span span = openSpans.remove(record.spanId());
        if (span == null) {
            // 孤儿终态（未见 RUNNING）：即时创建并结束，保真 start/end 与属性
            span = tracer.spanBuilder(spanName(record))
                    .setParent(resolveParent(record))
                    .setStartTimestamp(record.startedAt())
                    .startSpan();
        }
        applyAttributes(span, record);
        applyStatus(span, record.status());
        span.end(record.endedAt() != null ? record.endedAt() : Instant.now());
    }

    private Context resolveParent(SpanRecord record) {
        Span parent = record.parentSpanId() == null ? null : openSpans.get(record.parentSpanId());
        if (parent != null) {
            return Context.root().with(parent);
        }
        // 无已知父：挂到 sessionId 派生的合成根（非记录 span），保证同会话同 traceId
        SessionTrace st = sessionTrace(record.sessionId());
        SpanContext root = SpanContext.create(st.traceId(), st.rootSpanId(),
                TraceFlags.getSampled(), TraceState.getDefault());
        return Context.root().with(Span.wrap(root));
    }

    // ---- 属性 / 状态映射 ----

    private void applyAttributes(Span span, SpanRecord record) {
        applyCanonical(span, record);
        Map<String, Object> attrs = record.attributes();
        if (attrs != null) {
            for (Map.Entry<String, Object> e : attrs.entrySet()) {
                setObject(span, e.getKey(), e.getValue());
            }
        }
    }

    private void applyCanonical(Span span, SpanRecord record) {
        switch (record.kind()) {
            case SpanKind.TURN -> span.setAttribute("buzhou.turn_seq", record.turnSeq());
            case SpanKind.MODEL_CALL -> {
                span.setAttribute("gen_ai.operation.name", "chat");
                setObject(span, "gen_ai.request.model", record.attributes().get("model.name"));
                setLong(span, "gen_ai.usage.input_tokens", record.attributes().get("usage.prompt_tokens"));
                setLong(span, "gen_ai.usage.output_tokens", record.attributes().get("usage.completion_tokens"));
                setLong(span, "gen_ai.usage.reasoning_tokens", record.attributes().get("usage.reasoning_tokens"));
            }
            case SpanKind.TOOL_CALL -> {
                span.setAttribute("gen_ai.operation.name", "execute_tool");
                setObject(span, "gen_ai.tool.name", record.attributes().get("tool.name"));
                setObject(span, "gen_ai.tool.call.id", record.attributes().get("tool.call.id"));
            }
            case SpanKind.SESSION, SpanKind.HARNESS_INTERNAL -> {
                // 名字已承载语义；属性经 passthrough 原样透传
            }
            default -> { }
        }
    }

    private void applyStatus(Span span, String status) {
        switch (status) {
            case SpanStatus.ERROR -> span.setStatus(StatusCode.ERROR);
            case SpanStatus.CANCELLED -> {
                span.setStatus(StatusCode.UNSET);
                span.setAttribute("buzhou.cancelled", true);
            }
            default -> span.setStatus(StatusCode.OK);
        }
    }

    private void applyExceptionAttributes(Span span, Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        setObject(span, "exception.type", payload.get("exception.type"));
        setObject(span, "exception.message", payload.get("message"));
        if (includeContent) {
            setObject(span, "exception.stacktrace", payload.get("stacktrace"));
        }
    }

    private Attributes eventAttributes(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Attributes.empty();
        }
        AttributesBuilder b = Attributes.builder();
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (!includeContent && CONTENT_KEYS.contains(e.getKey())) {
                continue;
            }
            putObject(b, e.getKey(), e.getValue());
        }
        return b.build();
    }

    private static String spanName(SpanRecord record) {
        switch (record.kind()) {
            case SpanKind.SESSION:
                return "buzhou.session";
            case SpanKind.TURN:
                return "buzhou.turn";
            case SpanKind.MODEL_CALL: {
                Object m = record.attributes().get("model.name");
                return m == null ? "chat" : "chat " + m;
            }
            case SpanKind.TOOL_CALL: {
                Object t = record.attributes().get("tool.name");
                return t == null ? "execute_tool" : "execute_tool " + t;
            }
            case SpanKind.HARNESS_INTERNAL: {
                Object action = record.attributes().get("internal.action");
                if (action != null) {
                    return "buzhou.internal." + action;
                }
                String n = record.name();
                return (n != null && n.startsWith("internal:"))
                        ? "buzhou.internal." + n.substring("internal:".length())
                        : "buzhou.internal";
            }
            default:
                return record.name();
        }
    }

    // ---- 属性值类型适配 ----

    private static void setObject(Span span, String key, Object value) {
        if (value == null) {
            return;
        }
        switch (value) {
            case String s -> span.setAttribute(key, s);
            case Boolean b -> span.setAttribute(key, b);
            // 浮点须在 Number 之前匹配（Double/Float 亦为 Number 子类）
            case Float f -> span.setAttribute(key, f.doubleValue());
            case Double d -> span.setAttribute(key, d);
            case Number n -> span.setAttribute(key, n.longValue()); // Integer/Long/Short/Byte 等整型归 long
            default -> span.setAttribute(key, String.valueOf(value));
        }
    }

    private static void setLong(Span span, String key, Object value) {
        if (value instanceof Number n) {
            span.setAttribute(key, n.longValue());
        }
    }

    private static void putObject(AttributesBuilder b, String key, Object value) {
        if (value == null) {
            return;
        }
        switch (value) {
            case String s -> b.put(key, s);
            case Boolean bl -> b.put(key, bl);
            case Float f -> b.put(key, f.doubleValue());
            case Double d -> b.put(key, d);
            case Number n -> b.put(key, n.longValue());
            default -> b.put(key, String.valueOf(value));
        }
    }

    // ---- sessionId → traceId 派生 ----

    private SessionTrace sessionTrace(String sessionId) {
        String sid = (sessionId == null || sessionId.isBlank()) ? "unknown" : sessionId;
        SessionTrace existing = sessionTrace.get(sid);
        if (existing != null) {
            return existing;
        }
        // impl-47：trace 派生缓存上界（无会话结束回调，靠驱逐防长跑无界；traceId 由 sid 确定性派生，驱逐后重建无损）
        if (sessionTrace.size() >= maxSessionTraces && sessionTrace.size() > 0) {
            var iterator = sessionTrace.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.remove();
            }
        }
        return sessionTrace.computeIfAbsent(sid, OtelBridgeSink::deriveSessionTrace);
    }

    /** impl-47：旁路失败限频 WARN（首条 + 每 N 条；此前纯静默，导出链路故障生产不可见）。 */
    private void logSinkFailure(String operation, RuntimeException e) {
        long total = sinkFailures.incrementAndGet();
        if (total == 1 || total % LOG_EVERY == 0) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "otel 桥 " + operation + " 失败（已隔离，第 " + total + " 次）："
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static SessionTrace deriveSessionTrace(String sessionId) {
        byte[] digest = sha256(sessionId);
        HexFormat hex = HexFormat.of();
        return new SessionTrace(hex.formatHex(digest, 0, 16), hex.formatHex(digest, 16, 24));
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static long epochNanos(Instant instant) {
        if (instant == null) {
            return 0L;
        }
        return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000_000L), instant.getNano());
    }

    private record SessionTrace(String traceId, String rootSpanId) {
    }
}
