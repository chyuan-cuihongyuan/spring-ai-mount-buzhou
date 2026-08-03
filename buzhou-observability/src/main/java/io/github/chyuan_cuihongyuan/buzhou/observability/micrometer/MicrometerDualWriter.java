package io.github.chyuan_cuihongyuan.buzhou.observability.micrometer;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micrometer 双写（spec 03 Micrometer 双写表）：Span 关闭与 Event 发出时同步写指标，
 * 与 span 属性双写，供现有 Prometheus 栈直接可用。
 *
 * <p>{@link MeterRegistry} 可选；为 null（或 {@link #NOOP}）时短路，零依赖起步。
 */
public class MicrometerDualWriter {

    public static final MicrometerDualWriter NOOP = new MicrometerDualWriter(null);

    private static final String MODEL_CALL_DURATION = "buzhou.model.call.duration";
    private static final String TOOL_CALL_DURATION = "buzhou.tool.call.duration";
    private static final String TOKENS = "buzhou.tokens";
    private static final String QUEUE_WAIT = "buzhou.observability.queue.wait";
    private static final String PERSIST_ERRORS = "buzhou.observability.persist.errors";

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public MicrometerDualWriter(MeterRegistry registry) {
        this.registry = registry;
    }

    /** span 关闭时记录耗时（仅 MODEL_CALL / TOOL_CALL）。 */
    public void recordSpanClose(String kind, String name, String status, Map<String, Object> attributes) {
        if (registry == null) {
            return;
        }
        if (SpanKind.MODEL_CALL.equals(kind)) {
            String model = stringOf(attributes.get("model.name"));
            timer(MODEL_CALL_DURATION, Tags.of("model.provider", stringOr(attributes.get("model.provider"), "unknown"),
                    "model.name", model)).record(java.time.Duration.ofMillis(longOf(attributes.get("duration.ms"), 0L)));
        } else if (SpanKind.TOOL_CALL.equals(kind)) {
            timer(TOOL_CALL_DURATION, Tags.of("tool.name", name, "status", status))
                    .record(java.time.Duration.ofMillis(longOf(attributes.get("duration.ms"), 0L)));
        }
    }

    /** Event 发出时的轻量计数（DEBUG 用，避免高基数）。 */
    public void recordEvent(String type) {
        // Event 不直接产 metric（payload 高基数），仅作可观测扩展位
    }

    /** token 用量采集：prompt / completion（reasoning 经 usage 字段单独记）。 */
    public void recordTokens(String modelName, String kind, long count) {
        if (registry == null || count <= 0) {
            return;
        }
        counter(TOKENS, Tags.of("kind", kind, "model.name", modelName == null ? "unknown" : modelName))
                .increment(count);
    }

    /** 入队背压等待时长（采集方自观测）。 */
    public void recordQueueWait(long millis) {
        if (registry == null) {
            return;
        }
        timer(QUEUE_WAIT, Tags.empty()).record(java.time.Duration.ofMillis(millis));
    }

    /** 落库异常计数（故障可观测）。 */
    public void recordPersistError() {
        if (registry == null) {
            return;
        }
        counter(PERSIST_ERRORS, Tags.empty()).increment();
    }

    private Timer timer(String name, Tags tags) {
        return timers.computeIfAbsent(name + tags,
                k -> Timer.builder(name).tags(tags).register(registry));
    }

    private Counter counter(String name, Tags tags) {
        return counters.computeIfAbsent(name + tags,
                k -> Counter.builder(name).tags(tags).register(registry));
    }

    private static String stringOf(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String stringOr(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    private static long longOf(Object o, long fallback) {
        return o instanceof Number n ? n.longValue() : fallback;
    }

    private static final java.util.Map<String, Object> EMPTY = java.util.Map.of();
}
