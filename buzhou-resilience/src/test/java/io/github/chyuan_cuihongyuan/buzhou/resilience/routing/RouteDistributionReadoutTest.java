package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 805 / T1112：路由分布倾斜读数回归——份额/期望偏差/基尼系数精确值/
 * 零流量与缺权口径/收集器封顶。
 */
class RouteDistributionReadoutTest {

    @Test
    void sharesAndDeviationsComputedAndSorted() {
        // a=90/100 vs 期望 50% → +0.40；b=9/100 vs 30% → -0.21；c=1/100 vs 20% → -0.19
        var report = RouteDistributionReadout.analyze(
                Map.of("a", 90L, "b", 9L, "c", 1L),
                Map.of("a", 50, "b", 30, "c", 20));

        assertThat(report.totalCalls()).isEqualTo(100);
        assertThat(report.deviations().get(0).route()).isEqualTo("a");
        assertThat(report.deviations().get(0).share()).isCloseTo(0.90, within(1e-9));
        assertThat(report.deviations().get(0).expectedShare()).isCloseTo(0.5, within(1e-9));
        assertThat(report.deviations().get(0).deviation()).isCloseTo(0.40, within(1e-9));
        assertThat(report.deviations().get(1).route()).isEqualTo("b");
        assertThat(report.deviations().get(2).route()).isEqualTo("c");
        assertThat(report.dominantRoute()).isEqualTo("a");
        assertThat(report.dominantShare()).isCloseTo(0.90, within(1e-9));
    }

    @Test
    void missingWeightAndMissingTrafficAreDeviations() {
        // a 声明权重但零流量；c 有流量但未声明权重（期望=0）
        var report = RouteDistributionReadout.analyze(
                Map.of("c", 10L),
                Map.of("a", 30, "b", 70));

        assertThat(report.routes()).isEqualTo(3);
        var byRoute = report.deviations().stream()
                .collect(java.util.stream.Collectors.toMap(
                        RouteDistributionReadout.Deviation::route, d -> d));
        assertThat(byRoute.get("a").expectedShare()).isCloseTo(0.3, within(1e-9));
        assertThat(byRoute.get("a").share()).isCloseTo(0, within(1e-9));
        assertThat(byRoute.get("c").expectedShare()).isCloseTo(0, within(1e-9));
        assertThat(byRoute.get("c").share()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void giniKnownValues() {
        // 均匀 → 0
        assertThat(RouteDistributionReadout.gini(Map.of("a", 5L, "b", 5L), 10))
                .isCloseTo(0, within(1e-9));
        // 全集中单路由（2 路由）→ (n-1)/n = 0.5
        assertThat(RouteDistributionReadout.gini(Map.of("a", 10L, "b", 0L), 10))
                .isCloseTo(0.5, within(1e-9));
        // 无流量 → 0
        assertThat(RouteDistributionReadout.gini(Map.of(), 0)).isZero();
    }

    @Test
    void emptyAnalysisYieldsEmptyReport() {
        var report = RouteDistributionReadout.analyze(Map.of(), Map.of());
        assertThat(report.deviations()).isEmpty();
        assertThat(report.totalCalls()).isZero();
        assertThat(report.dominantRoute()).isNull();
        assertThat(report.gini()).isZero();
    }

    @Test
    void collectorCountsAndCaps() {
        RouteDistributionReadout.Collector collector = new RouteDistributionReadout.Collector();
        collector.record("a");
        collector.record("a");
        collector.record("b");
        collector.record(null);
        collector.record("  ");
        assertThat(collector.counts()).containsEntry("a", 2L).containsEntry("b", 1L);
        assertThat(collector.totalRecorded()).isEqualTo(3); // null/空白计入总数但不计数

        RouteDistributionReadout.Collector capped = new RouteDistributionReadout.Collector();
        for (int i = 0; i < RouteDistributionReadout.MAX_ROUTES + 5; i++) {
            capped.record("r" + i);
        }
        assertThat(capped.counts()).hasSize(RouteDistributionReadout.MAX_ROUTES);
        assertThat(capped.truncated()).isTrue();
        assertThat(capped.totalRecorded()).isEqualTo(RouteDistributionReadout.MAX_ROUTES + 5);

        // 收集器 → 分析联动
        var report = RouteDistributionReadout.analyze(collector.counts(), Map.of("a", 1, "b", 1));
        assertThat(report.totalCalls()).isEqualTo(3);
    }

    @Test
    void nullMapsFailFast() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> RouteDistributionReadout.analyze(null, Map.of()));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> RouteDistributionReadout.analyze(Map.of(), null));
    }
}
