package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 806 / T1114：预算分位推荐回归——最近秩分位精确值/headroom 推导/
 * 样本不足哨兵/环滑窗 FIFO/负样本忽略/fail-fast。
 */
class BudgetRecommendationTest {

    @Test
    void nearestRankPercentilesExact() {
        // 1..100 升序：P50=50(⌈0.5·100⌉=50), P95=95, P99=99
        List<Long> samples = java.util.stream.LongStream.rangeClosed(1, 100)
                .boxed().toList();
        BudgetRecommendation.Report report = BudgetRecommendation.recommend(samples, 20);
        assertThat(report.samples()).isEqualTo(100);
        assertThat(report.p50()).isEqualTo(50);
        assertThat(report.p95()).isEqualTo(95);
        assertThat(report.p99()).isEqualTo(99);
        assertThat(report.max()).isEqualTo(100);
        assertThat(report.sufficient()).isTrue();
        // 推荐 = ⌈95 × 1.2⌉ = 114
        assertThat(report.recommended()).isEqualTo(114);
    }

    @Test
    void headroomZeroMeansP95() {
        List<Long> samples = List.of(10L, 20L, 30L, 40L, 100L);
        // P95 最近秩：⌈0.95·5⌉=5 → 100
        BudgetRecommendation.Report zero = BudgetRecommendation.recommend(samples, 0);
        assertThat(zero.p95()).isEqualTo(100);
        assertThat(zero.recommended()).isEqualTo(100);
        // P50: ⌈0.5·5⌉=3 → 30
        assertThat(zero.p50()).isEqualTo(30);
    }

    @Test
    void insufficientSamplesSentinel() {
        BudgetRecommendation.Report few = BudgetRecommendation.recommend(List.of(1L, 2L, 3L, 4L), 10);
        assertThat(few.sufficient()).isFalse();
        assertThat(few.recommended()).isEqualTo(-1);
        assertThat(few.samples()).isEqualTo(4);

        assertThat(BudgetRecommendation.recommend(List.of(), 10).sufficient()).isFalse();
        assertThat(BudgetRecommendation.recommend(null, 10).samples()).isZero();
        // MIN_SAMPLES 边界：恰 5 个 → sufficient
        assertThat(BudgetRecommendation.recommend(List.of(1L, 2L, 3L, 4L, 5L), 10).sufficient()).isTrue();
    }

    @Test
    void negativeAndNullSamplesIgnored() {
        BudgetRecommendation.Report report = BudgetRecommendation.recommend(
                java.util.Arrays.asList(10L, null, -5L, 20L, 30L, 40L, 50L), 0);
        assertThat(report.samples()).isEqualTo(5);
        assertThat(report.max()).isEqualTo(50);
    }

    @Test
    void ringEvictsOldestFifo() {
        BudgetRecommendation.Ring ring = new BudgetRecommendation.Ring();
        ring.record(1);
        ring.record(2);
        ring.record(-3); // 忽略
        assertThat(ring.snapshot()).containsExactly(1L, 2L);

        for (long i = 10; i < 10 + BudgetRecommendation.Ring.CAPACITY; i++) {
            ring.record(i);
        }
        assertThat(ring.size()).isEqualTo(BudgetRecommendation.Ring.CAPACITY);
        assertThat(ring.snapshot().get(0)).isEqualTo(10L); // 旧样本仍在

        ring.record(9999); // 挤掉 10
        assertThat(ring.snapshot().get(0)).isEqualTo(11L);
        assertThat(ring.snapshot().get(ring.size() - 1)).isEqualTo(9999L);
        assertThat(ring.dropped()).isEqualTo(3); // 环已有 2 条：1024 次灌入挤 2 + 9999 挤 1
    }

    @Test
    void failFastOnBadHeadroom() {
        assertThatThrownBy(() -> BudgetRecommendation.recommend(List.of(1L), -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BudgetRecommendation.recommend(List.of(1L), 501))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
