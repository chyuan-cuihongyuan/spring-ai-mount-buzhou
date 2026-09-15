package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 钩子异常类型分布（L 会话 1700 系 R20 = effort #1719 / spec 1719 /
 * 票 T2639 + T2640 / impl 1319）——Sentry 事件分组思想：钩子抛异常按
 * 「钩子名:异常类」指纹分组计数——「哪个钩子在频繁炸什么」一表显形，
 * 与 {@link HookTimingAggregator}（耗时面）互补。
 *
 * <p>实例面线程安全：分组键有界（默认 32，超出并桶 {@code _overflow_}
 * ——基数纪律防标签爆炸，TagCardinalityGuard 同思想）；census() 降序。
 *
 * @since 1.0.0
 */
public final class HookErrorDistribution {

    /** 默认分组基数上限。 */
    public static final int DEFAULT_MAX_GROUPS = 32;

    private final int maxGroups;
    private final Map<String, AtomicLong> groups = new LinkedHashMap<>();
    private final AtomicLong total = new AtomicLong();

    /** 默认基数上限。 */
    public HookErrorDistribution() {
        this(DEFAULT_MAX_GROUPS);
    }

    /** 自定义基数上限（&lt;1 按默认）。 */
    public HookErrorDistribution(int maxGroups) {
        this.maxGroups = maxGroups < 1 ? DEFAULT_MAX_GROUPS : maxGroups;
    }

    /** 记一次钩子异常（hookName+异常简单类名 = 分组指纹）。 */
    public void record(String hookName, Throwable throwable) {
        total.incrementAndGet();
        String simple = throwable == null ? "Unknown" : throwable.getClass().getSimpleName();
        String key = (hookName == null || hookName.isBlank() ? "_anonymous_" : hookName)
                + ":" + simple;
        AtomicLong counter = groups.get(key);
        if (counter == null) {
            if (groups.size() >= maxGroups) {
                key = "_overflow_";
                counter = groups.get(key);
            }
            if (counter == null) {
                counter = groups.computeIfAbsent(key, k -> new AtomicLong());
            }
        }
        counter.incrementAndGet();
    }

    /** 分组→计数只读快照（降序；unmodifiableMap 保序）。 */
    public Map<String, Long> census() {
        List<Map.Entry<String, Long>> desc = new ArrayList<>();
        groups.forEach((k, v) -> desc.add(Map.entry(k, v.get())));
        desc.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()));
        Map<String, Long> ordered = new LinkedHashMap<>();
        desc.forEach(e -> ordered.put(e.getKey(), e.getValue()));
        return Collections.unmodifiableMap(ordered);
    }

    /** 异常总数。 */
    public long total() {
        return total.get();
    }
}
