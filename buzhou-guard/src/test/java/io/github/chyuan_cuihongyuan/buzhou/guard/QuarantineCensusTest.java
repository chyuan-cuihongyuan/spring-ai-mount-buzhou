package io.github.chyuan_cuihongyuan.buzhou.guard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1842 / T2886：隔离区普查——待审积压、最老旧、占比。 */
class QuarantineCensusTest {

    /** 待审/已审分账 + 最老旧取待审侧最大。 */
    @Test
    void shouldCensusPendingAndReviewed() {
        QuarantineCensus.Census census = QuarantineCensus.census(List.of(
                new QuarantineCensus.Quarantined("q1", "疑似注入", 100L, false),
                new QuarantineCensus.Quarantined("q2", "疑似泄漏", 900L, false),
                new QuarantineCensus.Quarantined("q3", "误报放行", 5000L, true)));
        assertThat(census.items()).isEqualTo(3);
        assertThat(census.pendingReview()).isEqualTo(2);
        assertThat(census.reviewedCount()).isEqualTo(1);
        assertThat(census.oldestPendingAgeMillis()).isEqualTo(900L);
        assertThat(census.pendingRatio()).isEqualTo(2.0 / 3.0);
    }

    /** 全已审（健康）与全待审（拥堵）两极。 */
    @Test
    void extremesReadDistinctly() {
        QuarantineCensus.Census healthy = QuarantineCensus.census(List.of(
                new QuarantineCensus.Quarantined("q", "r", 1L, true)));
        assertThat(healthy.pendingReview()).isZero();
        assertThat(healthy.oldestPendingAgeMillis()).isEqualTo(-1L);
        assertThat(healthy.pendingRatio()).isZero();

        QuarantineCensus.Census jammed = QuarantineCensus.census(List.of(
                new QuarantineCensus.Quarantined("q", "r", 50L, false)));
        assertThat(jammed.pendingRatio()).isEqualTo(1.0d);
        assertThat(jammed.oldestPendingAgeMillis()).isEqualTo(50L);
    }

    /** 空表与 null 哨兵。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (QuarantineCensus.Census census : List.of(
                QuarantineCensus.census(List.of()),
                QuarantineCensus.census(null))) {
            assertThat(census.items()).isZero();
            assertThat(census.oldestPendingAgeMillis()).isEqualTo(-1L);
            assertThat(census.pendingRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形条目 fail-fast：空白 id、负龄期。 */
    @Test
    void malformedEntryFailsFast() {
        assertThatThrownBy(() -> new QuarantineCensus.Quarantined("", "r", 1L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id 非空白");
        assertThatThrownBy(() -> new QuarantineCensus.Quarantined("q", "r", -1L, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
