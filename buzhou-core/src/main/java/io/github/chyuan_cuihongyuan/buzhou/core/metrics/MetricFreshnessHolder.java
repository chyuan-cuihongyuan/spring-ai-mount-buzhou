package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Optional;

/**
 * 指标新鲜度追踪 Holder（spec 1614 / T2379，spec 802 孤类接线）：
 * metrics 装配链包装 {@link MetricFreshnessTracker} 后在此登记——
 * {@link #audit} 静态便捷面供观测端点/排障查询「哪些指标不再有数」
 * （序列静默 = 写路径死亡或装配丢失的信号）。
 * @since 1.0.0
 */
public final class MetricFreshnessHolder {

    private static volatile MetricFreshnessTracker tracker;

    private MetricFreshnessHolder() {
    }

    /** 装配链登记（null = 清除）。 */
    public static void install(MetricFreshnessTracker instance) {
        tracker = instance;
    }

    /** 当前追踪器（未装配 = empty）。 */
    public static Optional<MetricFreshnessTracker> tracker() {
        return Optional.ofNullable(tracker);
    }

    /** 新鲜度审计便捷面（nowMillis = 审计时刻；未装配 = empty）。 */
    public static Optional<MetricFreshnessTracker.FreshnessReport> audit(long nowMillis,
            long staleAfterMillis) {
        return tracker().map(t -> t.audit(nowMillis, staleAfterMillis));
    }
}
