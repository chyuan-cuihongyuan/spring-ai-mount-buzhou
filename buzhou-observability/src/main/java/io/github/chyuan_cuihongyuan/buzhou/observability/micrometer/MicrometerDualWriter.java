package io.github.chyuan_cuihongyuan.buzhou.observability.micrometer;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;

import java.time.Duration;
import java.util.Map;

/**
 * Micrometer 双写适配器（impl-46 / spec 14 §B：指标家族收敛进 core 单一口径）。
 *
 * <p>不再持有私有 {@code MeterRegistry} 与平行指标缓存——全部经
 * {@link BuzhouMetricsHolder} 全局实例记录（core 在有 micrometer 时安装实现，
 * 未装时 no-op 零开销）；指标名在 core {@code BuzhouMetricsBinder} 预注册，
 * 首次记录即 {@code /metrics} 可见。落库失败并入既有
 * {@code buzhou.store.write.failures}（policy=degrade），不再另设平行计数器。
 *
 * <p>构造器保留（registry 参数被忽略）仅为源码兼容；新代码直接用 {@link #NOOP} 或无参构造。
 */
public class MicrometerDualWriter {

    /** 兼容哨兵：全部方法 no-op（buzhou.observability.micrometer-enabled=false 时使用）。 */
    public static final MicrometerDualWriter NOOP = new MicrometerDualWriter(true);

    private final boolean disabled;

    public MicrometerDualWriter(io.micrometer.core.instrument.MeterRegistry registry) {
        // registry 由 core 装配层统一安装进 BuzhouMetricsHolder，此处忽略（impl-46 收敛）
        this(false);
    }

    public MicrometerDualWriter() {
        this(false);
    }

    private MicrometerDualWriter(boolean disabled) {
        this.disabled = disabled;
    }

    /** span 关闭时记录耗时（仅 MODEL_CALL / TOOL_CALL；tag 值经 bounded 截断防无界基数）。 */
    public void recordSpanClose(String kind, String name, String status, Map<String, Object> attributes) {
        if (disabled) {
            return;
        }
        if (SpanKind.MODEL_CALL.equals(kind)) {
            BuzhouMetricsHolder.metrics().timer("buzhou.model.call.duration",
                    durationOf(attributes),
                    "model.provider", bounded(stringOr(attributes.get("model.provider"), "unknown"), 32),
                    "model.name", bounded(stringOr(attributes.get("model.name"), "unknown"), 64));
        } else if (SpanKind.TOOL_CALL.equals(kind)) {
            BuzhouMetricsHolder.metrics().timer("buzhou.tool.call.duration",
                    durationOf(attributes),
                    "tool.name", bounded(name, 64), "status", bounded(status, 16));
        }
    }

    /** Event 类型不直接产 metric（payload 高基数）；显式 no-op 钩子，供未来指标扩展。 */
    public void recordEvent(String type) {
        // 有意 no-op（impl-46：原为无 javadoc 的静默空方法，现显式声明语义）
    }

    /** token 用量采集：prompt / completion（reasoning 经 usage 字段单独记）。 */
    public void recordTokens(String modelName, String kind, long count) {
        if (disabled || count <= 0) {
            return;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.tokens", count,
                "kind", bounded(kind, 16), "model.name", bounded(modelName == null ? "unknown" : modelName, 64));
    }

    /** 入队背压等待时长（采集方自观测）。 */
    public void recordQueueWait(long millis) {
        if (disabled) {
            return;
        }
        BuzhouMetricsHolder.metrics().timer("buzhou.observability.queue.wait", Duration.ofMillis(millis));
    }

    /** 落库异常计数（并入 core 家族 store.write.failures，policy=degrade：观测降级不拖主链路）。 */
    public void recordPersistError() {
        if (disabled) {
            return;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.store.write.failures", 1, "policy", "degrade");
    }

    private static Duration durationOf(Map<String, Object> attributes) {
        Object v = attributes == null ? null : attributes.get("duration.ms");
        return Duration.ofMillis(v instanceof Number n ? n.longValue() : 0L);
    }

    private static String stringOr(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    /** tag 值有界截断（tag 基数纪律）。 */
    private static String bounded(String value, int max) {
        if (value == null) {
            return "unknown";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
