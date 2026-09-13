package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 路由流量分布倾斜读数（spec 805 / T1111，Spark skew detection 借鉴）：
 * 实际调用分布 vs 声明权重分布的偏差结构化——「权重说 50/50、实际 95/5」
 * 的漂移（慢启动残留/压权未恢复/路由粘连）一眼可见。
 *
 * <p>双层：{@link Collector} 名下按路由计数（封顶 {@value #MAX_ROUTES}+
 * truncated，线程安全）；{@link #analyze(Map, Map)} 纯函数——份额/期望份额/
 * 偏差排序 + 实际分布基尼系数（0=均匀，全集中单路由=(n-1)/n）。
 * 只读不纠偏（权重改写归 RoutingWeightsHotReload——读数面不改行为）。
 */
public final class RouteDistributionReadout {

    /** 路由数封顶。 */
    public static final int MAX_ROUTES = 32;

    /** 单路由偏差行（|share-expected| 降序）。 */
    public record Deviation(String route, long count, double share, double expectedShare, double deviation) {
    }

    /** 分析报告（不可变）。 */
    public record Report(List<Deviation> deviations, double gini, long totalCalls, int routes,
                         String dominantRoute, double dominantShare) {
    }

    /** 计数收集器（record 侧）。 */
    public static final class Collector {
        private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();
        private final AtomicLong total = new AtomicLong();
        private volatile boolean truncated;

        /** 记一次路由选择（null/空白忽略；超封顶截断）。 */
        public void record(String route) {
            if (route == null || route.isBlank()) {
                return;
            }
            total.incrementAndGet();
            if (!counts.containsKey(route) && counts.size() >= MAX_ROUTES) {
                truncated = true;
                return;
            }
            counts.computeIfAbsent(route, k -> new AtomicLong()).incrementAndGet();
        }

        /** 计数只读快照。 */
        public Map<String, Long> counts() {
            Map<String, Long> snapshot = new LinkedHashMap<>();
            counts.forEach((k, v) -> snapshot.put(k, v.get()));
            return snapshot;
        }

        /** 累计记录条数（含被截断丢弃的路由首笔）。 */
        public long totalRecorded() {
            return total.get();
        }

        public boolean truncated() {
            return truncated;
        }
    }

    /**
     * 纯函数分析：actualCounts 任意（expectedWeights 缺失的路由期望份额按 0、
     * 反之期望有但无实际流量也是偏差行）；两 map 皆空 → 空报告（gini=0）。
     */
    public static Report analyze(Map<String, Long> actualCounts, Map<String, Integer> expectedWeights) {
        Objects.requireNonNull(actualCounts, "actualCounts");
        Objects.requireNonNull(expectedWeights, "expectedWeights");
        long totalCalls = actualCounts.values().stream().filter(v -> v != null && v > 0)
                .mapToLong(Long::longValue).sum();
        int weightSum = expectedWeights.values().stream().filter(v -> v != null && v > 0)
                .mapToInt(Integer::intValue).sum();

        List<Deviation> deviations = new ArrayList<>();
        java.util.Set<String> allRoutes = new java.util.LinkedHashSet<>(actualCounts.keySet());
        allRoutes.addAll(expectedWeights.keySet());
        for (String route : allRoutes) {
            long count = actualCounts.getOrDefault(route, 0L);
            double share = totalCalls == 0 ? 0 : (double) count / totalCalls;
            Integer w = expectedWeights.get(route);
            double expected = (w == null || w <= 0 || weightSum == 0) ? 0 : (double) w / weightSum;
            deviations.add(new Deviation(route, count, share, expected, share - expected));
        }
        deviations.sort(Comparator.comparingDouble((Deviation d) -> Math.abs(d.deviation())).reversed()
                .thenComparing(d -> d.route())); // |偏差| 打平按名典序——确定性序

        double gini = gini(actualCounts, totalCalls);
        Deviation dominant = deviations.stream()
                .max(Comparator.comparingLong(Deviation::count)).orElse(null);
        return new Report(List.copyOf(deviations), gini, totalCalls, allRoutes.size(),
                dominant == null ? null : dominant.route,
                dominant == null || totalCalls == 0 ? 0 : (double) dominant.count / totalCalls);
    }

    /** 实际分布基尼系数（离散式：0=均匀；全集中单路由=(n-1)/n；无流量=0）。 */
    public static double gini(Map<String, Long> actualCounts, long totalCalls) {
        if (totalCalls == 0) {
            return 0;
        }
        List<Long> sorted = actualCounts.values().stream()
                .filter(v -> v != null && v >= 0).sorted().toList();
        int n = sorted.size();
        if (n == 0) {
            return 0;
        }
        long cumulative = 0;
        double weighted = 0;
        for (int i = 0; i < n; i++) {
            cumulative += sorted.get(i);
            weighted += (double) (i + 1) * sorted.get(i);
        }
        return (2 * weighted) / ((double) n * cumulative) - (double) (n + 1) / n;
    }
}
