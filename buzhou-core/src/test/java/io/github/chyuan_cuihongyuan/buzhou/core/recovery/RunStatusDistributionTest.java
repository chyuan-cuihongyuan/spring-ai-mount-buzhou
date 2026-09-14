package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1429 / T2160：运行状态分布与滞后审计——状态直方全枚举预置、
 * turn 滞后=崩溃暴露窗口、worst offenders 降序典序、空输入哨兵。
 */
class RunStatusDistributionTest {

    private static RunStateSnapshot snap(String sessionId, RunStatus status,
                                         int currentTurn, int lastCompletedTurn) {
        return new RunStateSnapshot(sessionId, "app", "ag", status,
                currentTurn, lastCompletedTurn, "owner", Instant.now());
    }

    @Test
    void emptyInputYieldsZeroHistogram() {
        var r = RunStatusDistribution.analyze(List.of());
        assertThat(r.statusHistogram().get(RunStatus.RUNNING)).isZero();
        assertThat(r.runningLagMax()).isZero();
        assertThat(r.worstOffenders()).isEmpty();
    }

    @Test
    void statusHistogramCountsAllEnumeratedStates() {
        var r = RunStatusDistribution.analyze(List.of(
                snap("s1", RunStatus.RUNNING, 5, 3),
                snap("s2", RunStatus.RUNNING, 2, 2),
                snap("s3", RunStatus.COMPLETED, 4, 4)));
        assertThat(r.statusHistogram().get(RunStatus.RUNNING)).isEqualTo(2);
        assertThat(r.statusHistogram().get(RunStatus.COMPLETED)).isEqualTo(1);
        assertThat(r.statusHistogram().get(RunStatus.INTERRUPTED)).isZero();
    }

    @Test
    void turnLagIsCrashExposureWindow() {
        var r = RunStatusDistribution.analyze(List.of(
                snap("s-lag", RunStatus.RUNNING, 9, 5)));
        // 9 轮进行、5 轮已持久化——崩溃丢 4 轮
        assertThat(r.runningLagMax()).isEqualTo(4);
        assertThat(r.worstOffenders()).hasSize(1);
        assertThat(r.worstOffenders().get(0).sessionId()).isEqualTo("s-lag");
        assertThat(r.worstOffenders().get(0).turnLag()).isEqualTo(4);
    }

    @Test
    void worstOffendersSortedDescWithTieBreak() {
        var r = RunStatusDistribution.analyze(List.of(
                snap("bbb", RunStatus.RUNNING, 8, 5),
                snap("aaa", RunStatus.RUNNING, 8, 5),
                snap("ccc", RunStatus.RUNNING, 3, 3)));
        var ids = r.worstOffenders().stream().map(RunStatusDistribution.TurnLag::sessionId).toList();
        // 同滞后 3 按会话典序；零滞后不入榜
        assertThat(ids).containsExactly("aaa", "bbb");
    }

    @Test
    void completedRunsContributeZeroLag() {
        var r = RunStatusDistribution.analyze(List.of(
                snap("s-done", RunStatus.COMPLETED, 10, 10)));
        assertThat(r.runningLagMax()).isZero(); // RUNNING 维不含 COMPLETED
        assertThat(r.worstOffenders()).isEmpty();
    }
}
