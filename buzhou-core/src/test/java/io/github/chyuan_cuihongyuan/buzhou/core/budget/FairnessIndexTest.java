package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1409 / T2120：多租户配额公平指数——Jain 公式精确断言、均匀=1、
 * 单点吃满→主导占比、全零/空哨兵、份额降序与公平判定。
 */
class FairnessIndexTest {

    @Test
    void perfectlyEqualUsageScoresOne() {
        var report = FairnessIndex.of(Map.of(
                "t1", 100L, "t2", 100L, "t3", 100L));
        assertThat(report.jain()).isEqualTo(1.0d, within(1e-9));
        assertThat(report.dominantShare()).isEqualTo(1.0d / 3, within(1e-9));
        assertThat(report.isFair()).isTrue();
    }

    @Test
    void singleTenantHoggingScoresLow() {
        // 990 vs 10：J = (1000)²/(2·(980100+100)) = 1000000/1960400 ≈ 0.5101
        var report = FairnessIndex.of(Map.of("hog", 990L, "quiet", 10L));
        assertThat(report.jain()).isCloseTo(0.5101d, within(1e-3));
        assertThat(report.dominantShare()).isEqualTo(0.99d, within(1e-9));
        assertThat(report.isFair()).isFalse();
    }

    @Test
    void allZeroAndEmptySamplesAreSentinels() {
        var zeros = FairnessIndex.of(Map.of("a", 0L, "b", 0L));
        assertThat(zeros.jain()).isEqualTo(-1d);
        assertThat(zeros.isFair()).isFalse();
        assertThat(zeros.total()).isZero();

        var empty = FairnessIndex.of(Map.of());
        assertThat(empty.consumers()).isZero();
        assertThat(empty.jain()).isEqualTo(-1d);
        assertThat(empty.shares()).isEmpty();
    }

    @Test
    void sharesAreSortedDescendingWithStableTieBreak() {
        var report = FairnessIndex.of(Map.of(
                "b-api", 300L, "a-api", 600L, "c-api", 100L));
        assertThat(report.shares()).extracting(FairnessIndex.TenantShare::tenant)
                .containsExactly("a-api", "b-api", "c-api");
        assertThat(report.shares().get(0).share()).isEqualTo(0.6d, within(1e-9));
        // 平局按名典序
        var tied = FairnessIndex.of(Map.of("y", 5L, "x", 5L));
        assertThat(tied.shares()).extracting(FairnessIndex.TenantShare::tenant)
                .containsExactly("x", "y");
    }

    @Test
    void longArrayOverloadNamesConsumersPositionally() {
        var report = FairnessIndex.of(new long[]{4, 4, 4, 4});
        assertThat(report.consumers()).isEqualTo(4);
        assertThat(report.jain()).isEqualTo(1.0d, within(1e-9));
        assertThat(report.total()).isEqualTo(16);
    }
}
