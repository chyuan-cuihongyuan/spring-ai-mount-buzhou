package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1812 / T2826：半消息审计——三态账目、超阈滞留、裁决率。 */
class HalfMessageAuditTest {

    /** 三态账目 + 超阈滞留计数 + 两比率。 */
    @Test
    void shouldCensusThreeStatesAndStaleHalves() {
        HalfMessageAudit.Census census = HalfMessageAudit.audit(5000L, List.of(
                new HalfMessageAudit.Intent("k1", HalfMessageAudit.IntentState.HALF, 6000L),
                new HalfMessageAudit.Intent("k2", HalfMessageAudit.IntentState.HALF, 100L),
                new HalfMessageAudit.Intent("k3", HalfMessageAudit.IntentState.COMMITTED, 0L),
                new HalfMessageAudit.Intent("k4", HalfMessageAudit.IntentState.ROLLED_BACK, 0L)));
        assertThat(census.total()).isEqualTo(4);
        assertThat(census.halves()).isEqualTo(2);
        assertThat(census.committed()).isEqualTo(1);
        assertThat(census.rolledBack()).isEqualTo(1);
        assertThat(census.staleHalves()).isEqualTo(1);
        assertThat(census.resolutionRatio()).isEqualTo(0.5d);
        assertThat(census.pendingRatio()).isEqualTo(0.5d);
    }

    /** 阈值边界：age == 阈即算超阈（≥）；已裁决意图不受阈影响。 */
    @Test
    void staleBoundaryIsInclusive() {
        HalfMessageAudit.Census census = HalfMessageAudit.audit(1000L, List.of(
                new HalfMessageAudit.Intent("k1", HalfMessageAudit.IntentState.HALF, 1000L),
                new HalfMessageAudit.Intent("k2", HalfMessageAudit.IntentState.HALF, 999L),
                new HalfMessageAudit.Intent("k3", HalfMessageAudit.IntentState.COMMITTED, 99999L)));
        assertThat(census.staleHalves()).isEqualTo(1);
        assertThat(census.halves()).isEqualTo(2);
    }

    /** 空表与 null 同口径：零账目 + 比率 -1 哨兵。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (HalfMessageAudit.Census census : List.of(
                HalfMessageAudit.audit(100L, List.of()),
                HalfMessageAudit.audit(100L, null))) {
            assertThat(census.total()).isZero();
            assertThat(census.resolutionRatio()).isEqualTo(-1d);
            assertThat(census.pendingRatio()).isEqualTo(-1d);
            assertThat(census.staleHalves()).isZero();
        }
    }

    /** 畸形入参 fail-fast：负阈、空 key、负年龄、null 状态。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> HalfMessageAudit.audit(-1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("staleThresholdMillis 不能为负");
        assertThatThrownBy(() -> new HalfMessageAudit.Intent(" ",
                HalfMessageAudit.IntentState.HALF, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HalfMessageAudit.Intent("k",
                HalfMessageAudit.IntentState.HALF, -1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HalfMessageAudit.Intent("k", null, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
