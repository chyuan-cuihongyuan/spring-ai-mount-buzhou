package io.github.chyuan_cuihongyuan.buzhou.core.leak;

import java.util.Optional;

/**
 * 泄漏疑似聚合 Holder（spec 1615 / T2381，spec 839 孤类接线）：
 * 装配侧把聚合器复合进 LeakListener 链（宿主 listener 若有则两者都收），
 * {@link #aggregator()}/{@link #report()} 静态读出——「泄漏是同一处反复漏
 * 还是多处散漏」从日志流水变排行。
 * @since 1.0.0
 */
public final class LeakSuspectHolder {

    private static volatile LeakSuspectAggregator aggregator = new LeakSuspectAggregator();

    private LeakSuspectHolder() {
    }

    /** 当前聚合器（测试可替换；null = 重置新实例）。 */
    public static void install(LeakSuspectAggregator instance) {
        aggregator = instance == null ? new LeakSuspectAggregator() : instance;
    }

    /** 当前聚合器。 */
    public static LeakSuspectAggregator aggregator() {
        return aggregator;
    }

    /** 聚合报告便捷面（未装配恒有默认实例——empty 不出现）。 */
    public static LeakSuspectAggregator.Report report() {
        return aggregator().snapshot();
    }

    /** 复合宿主 listener 与聚合器（宿主 null = 仅聚合器）。 */
    public static ResourceLeakDetector.LeakListener compositeWith(
            ResourceLeakDetector.LeakListener hostListener) {
        LeakSuspectAggregator agg = aggregator;
        if (hostListener == null) {
            return agg;
        }
        return report -> {
            hostListener.onLeak(report);
            agg.onLeak(report);
        };
    }

    /** Optional 引用占位（保留 API 对称；当前恒非空）。 */
    static Optional<LeakSuspectAggregator> optional() {
        return Optional.of(aggregator);
    }
}
