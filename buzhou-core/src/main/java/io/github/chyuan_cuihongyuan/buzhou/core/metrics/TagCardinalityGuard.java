package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * tag 基数守卫（spec 132 §A / T457，Grafana Loki label cardinality limit 借鉴）：
 * 装饰任意 {@link BuzhouMetrics}——per (指标名, tag 键) 的<b>去重值集</b>封顶
 * {@value #DEFAULT_MAX_VALUES_PER_TAG}，越限新值折 {@code __overflow__}（既有值
 * 照常直通）。防「tag 值有界」纪律失守时一个越界 tag 值把时序库 cardinality 打爆。
 *
 * <p><b>口径（诚实声明）</b>：折入 {@code __overflow__} 的样本<b>不丢</b>——计数/
 * 时长照记，丢的是维度细分；守卫自身计数 {@code buzhou.metrics.tag-overflow}
 * （无 tag——守卫的越界面不能自己变成无界面）。并发双插最坏轻微超限 1-2 值，
 * 无正确性影响（{@link ErrorSignatures} 封顶同先例）。指标名空间亦有界
 * （{@value #MAX_NAMES}，满则新名全折——新名是代码新增面的信号，响亮胜过静默）。
 *
 * <p><b>热路径权衡</b>：不抛异常不格式化——畸形键值对（奇数个/null）原样透传
 * （守卫失守不放大为指标路径故障）。
 */
public final class TagCardinalityGuard implements BuzhouMetrics {

    /** 每 (指标名, tag 键) 去重值封顶。 */
    public static final int DEFAULT_MAX_VALUES_PER_TAG = 64;
    /** 指标名空间封顶。 */
    public static final int MAX_NAMES = 512;
    /** 越限折入占位值。 */
    public static final String OVERFLOW = "__overflow__";

    private final BuzhouMetrics delegate;
    private final int maxValuesPerTag;
    /** name → tagKey → 值集（CHM 当有界集用，Boolean 恒 TRUE）。 */
    private final Map<String, Map<String, Map<String, Boolean>>> seen =
            new ConcurrentHashMap<>();
    private final AtomicLong folds = new AtomicLong();

    private TagCardinalityGuard(BuzhouMetrics delegate, int maxValuesPerTag) {
        this.delegate = delegate == null ? BuzhouMetrics.noop() : delegate;
        this.maxValuesPerTag = maxValuesPerTag;
    }

    /** 包裹委托（默认每键 {@value #DEFAULT_MAX_VALUES_PER_TAG} 值封顶）。 */
    public static TagCardinalityGuard wrap(BuzhouMetrics delegate) {
        return new TagCardinalityGuard(delegate, DEFAULT_MAX_VALUES_PER_TAG);
    }

    /** 包裹委托（自定每键封顶；≥1）。 */
    public static TagCardinalityGuard wrap(BuzhouMetrics delegate, int maxValuesPerTag) {
        if (maxValuesPerTag < 1) {
            throw new IllegalArgumentException("maxValuesPerTag must be >= 1: " + maxValuesPerTag);
        }
        return new TagCardinalityGuard(delegate, maxValuesPerTag);
    }

    @Override
    public void counter(String name, long delta, String... tagKeyValue) {
        delegate.counter(name, delta, sanitize(name, tagKeyValue));
    }

    @Override
    public void timer(String name, Duration duration, String... tagKeyValue) {
        delegate.timer(name, duration, sanitize(name, tagKeyValue));
    }

    @Override
    public void gauge(String name, java.util.function.Supplier<Number> value,
                      String... tagKeyValue) {
        delegate.gauge(name, value, sanitize(name, tagKeyValue));
    }

    /** 越限折入次数（守卫面：健康/测试——「纪律失守了几次」）。 */
    public long folds() {
        return folds.get();
    }

    private String[] sanitize(String name, String... tagKeyValue) {
        if (tagKeyValue == null || tagKeyValue.length < 2) {
            return tagKeyValue; // 畸形透传（不放大为指标路径故障）
        }
        Map<String, Map<String, Boolean>> perName = seen.get(name);
        if (perName == null) {
            if (seen.size() >= MAX_NAMES) {
                foldCounted();
                return foldAll(tagKeyValue);
            }
            perName = seen.computeIfAbsent(name, k -> new ConcurrentHashMap<>());
        }
        String[] out = tagKeyValue;
        for (int i = 0; i + 1 < tagKeyValue.length; i += 2) {
            String key = tagKeyValue[i];
            String value = tagKeyValue[i + 1];
            if (key == null || value == null) {
                continue;
            }
            Map<String, Boolean> valueSet = perName.get(key);
            if (valueSet == null) {
                valueSet = perName.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
            }
            if (valueSet.containsKey(value)) {
                continue; // 在册直通
            }
            if (valueSet.size() >= maxValuesPerTag) {
                foldCounted();
                if (out == tagKeyValue) {
                    out = tagKeyValue.clone();
                }
                out[i + 1] = OVERFLOW;
                continue;
            }
            valueSet.put(value, Boolean.TRUE);
        }
        return out;
    }

    /** 折入计数 + 指标（spec 204 §A / T568：越限进 buzhou.metrics.tag-overflow——守卫失守可告警）。 */
    private void foldCounted() {
        folds.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter("buzhou.metrics.tag-overflow");
    }

    private static String[] foldAll(String[] tagKeyValue) {
        String[] out = tagKeyValue.clone();
        for (int i = 1; i < out.length; i += 2) {
            if (out[i] != null) {
                out[i] = OVERFLOW;
            }
        }
        return out;
    }
}
