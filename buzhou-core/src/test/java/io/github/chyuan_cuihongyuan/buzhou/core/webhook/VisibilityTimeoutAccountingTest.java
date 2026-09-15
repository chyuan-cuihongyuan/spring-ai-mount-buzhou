package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1864 / T2930：可见性超时——重投边界、死信边界、三段普查。 */
class VisibilityTimeoutAccountingTest {

    /** 重投判定边界含上：到点即回队；已确认永不重投。 */
    @Test
    void redeliverBoundaryIsInclusiveAndAckHolds() {
        assertThat(VisibilityTimeoutAccounting.shouldRedeliver(
                1000L, 300L, 1299L, false)).isFalse();
        assertThat(VisibilityTimeoutAccounting.shouldRedeliver(
                1000L, 300L, 1300L, false)).isTrue();
        assertThat(VisibilityTimeoutAccounting.shouldRedeliver(
                1000L, 300L, 9999L, true)).isFalse();
    }

    /** 死信边界含上：第 max 次重投后仍失败即死信；零容忍合法。 */
    @Test
    void deadLetterBoundaryIsInclusive() {
        assertThat(VisibilityTimeoutAccounting.shouldDeadLetter(2, 3)).isFalse();
        assertThat(VisibilityTimeoutAccounting.shouldDeadLetter(3, 3)).isTrue();
        assertThat(VisibilityTimeoutAccounting.shouldDeadLetter(0, 0)).isTrue();
    }

    /** 三段普查：在飞/超时重投/死信分账 + 最老在飞龄 + 占比。 */
    @Test
    void censusSplitsThreeSegments() {
        VisibilityTimeoutAccounting.Census census = VisibilityTimeoutAccounting.census(
                1000L, 300L, 3, List.of(
                        new VisibilityTimeoutAccounting.DeliveryFact(900L, false, 0),
                        new VisibilityTimeoutAccounting.DeliveryFact(500L, false, 0),
                        new VisibilityTimeoutAccounting.DeliveryFact(900L, true, 0),
                        new VisibilityTimeoutAccounting.DeliveryFact(800L, false, 3)));
        assertThat(census.messages()).isEqualTo(4);
        assertThat(census.inFlight()).isEqualTo(1);   // 900 未确认未超时
        assertThat(census.overdueRedeliver()).isEqualTo(1); // 500 超时
        assertThat(census.deadLetterCandidates()).isEqualTo(1); // 重投 3/3
        assertThat(census.oldestInFlightAgeMillis()).isEqualTo(100L);
        assertThat(census.inFlightRatio()).isEqualTo(0.25d);
    }

    /** 空表与 null 哨兵；畸形 fail-fast。 */
    @Test
    void sentinelsAndMalformed() {
        for (VisibilityTimeoutAccounting.Census c : List.of(
                VisibilityTimeoutAccounting.census(0, 1, 1, List.of()),
                VisibilityTimeoutAccounting.census(0, 1, 1, null))) {
            assertThat(c.inFlightRatio()).isEqualTo(-1d);
            assertThat(c.oldestInFlightAgeMillis()).isEqualTo(-1L);
        }
        assertThatThrownBy(() -> VisibilityTimeoutAccounting.shouldRedeliver(
                -1, 1, 1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时间入参不能为负");
        assertThatThrownBy(() -> VisibilityTimeoutAccounting.shouldDeadLetter(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VisibilityTimeoutAccounting.DeliveryFact(
                -1, false, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
