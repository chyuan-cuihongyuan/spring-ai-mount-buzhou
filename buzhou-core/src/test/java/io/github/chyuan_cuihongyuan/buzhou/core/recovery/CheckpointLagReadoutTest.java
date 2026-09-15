package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1806 / T2814：检查点滞后账目——总滞后/最坏会话/越限计数/追平率。 */
class CheckpointLagReadoutTest {

    /** 账目正确：total/max/laggiest 聚合 + 越限计数 + 追平率。 */
    @Test
    void shouldAggregateLagAccounting() {
        CheckpointLagReadout.LagReport report = CheckpointLagReadout.analyze(List.of(
                new CheckpointLagReadout.SessionLag("s1", 100, 100),
                new CheckpointLagReadout.SessionLag("s2", 100, 90),
                new CheckpointLagReadout.SessionLag("s3", 50, 10)));
        assertThat(report.totalLag()).isEqualTo(50L);
        assertThat(report.maxLag()).isEqualTo(40L);
        assertThat(report.laggiestUser()).isEqualTo("s3");
        assertThat(report.sessionsBeyond(5)).isEqualTo(2L);
        assertThat(report.caughtUpRatio()).isEqualTo(1.0 / 3.0);
    }

    /** 并列最坏取首个（入参序，稳定可复现）。 */
    @Test
    void tieOnMaxTakesFirstInOrder() {
        CheckpointLagReadout.LagReport report = CheckpointLagReadout.analyze(List.of(
                new CheckpointLagReadout.SessionLag("a", 10, 0),
                new CheckpointLagReadout.SessionLag("b", 10, 0)));
        assertThat(report.laggiestUser()).isEqualTo("a");
    }

    /** 全追平：max=0、越限 0、追平率 1——健康态可读数。 */
    @Test
    void fullyCaughtUpReadsHealthy() {
        CheckpointLagReadout.LagReport report = CheckpointLagReadout.analyze(List.of(
                new CheckpointLagReadout.SessionLag("s1", 10, 10),
                new CheckpointLagReadout.SessionLag("s2", 0, 0)));
        assertThat(report.totalLag()).isZero();
        assertThat(report.maxLag()).isZero();
        assertThat(report.laggiestUser()).isEqualTo("s1");
        assertThat(report.sessionsBeyond(0)).isZero();
        assertThat(report.caughtUpRatio()).isEqualTo(1.0d);
    }

    /** 空表与 null 同口径：max -1 哨兵、追平率 -1 哨兵、越限 0。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (CheckpointLagReadout.LagReport report : List.of(
                CheckpointLagReadout.analyze(List.of()),
                CheckpointLagReadout.analyze(null))) {
            assertThat(report.totalLag()).isZero();
            assertThat(report.maxLag()).isEqualTo(-1L);
            assertThat(report.laggiestUser()).isNull();
            assertThat(report.caughtUpRatio()).isEqualTo(-1d);
            assertThat(report.sessionsBeyond(0)).isZero();
        }
    }

    /** 畸形会话事实 fail-fast：负计数、检查点超前、空 id。 */
    @Test
    void malformedSessionLagFailsFast() {
        assertThatThrownBy(() -> new CheckpointLagReadout.SessionLag("s", 5, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ≤ checkpointed ≤ produced");
        assertThatThrownBy(() -> new CheckpointLagReadout.SessionLag("s", -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CheckpointLagReadout.SessionLag(" ", 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
