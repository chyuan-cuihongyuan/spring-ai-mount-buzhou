package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 842 / T1186：认证决策分布回归——五态计数/占比降序/null 忽略/空真。
 */
class AuthDecisionStatsTest {

    @Test
    void countsAndShareSorted() {
        AuthDecisionStats stats = new AuthDecisionStats();
        stats.record(AuthDecisionStats.Outcome.GRANTED);
        stats.record(AuthDecisionStats.Outcome.GRANTED);
        stats.record(AuthDecisionStats.Outcome.GRANTED);
        stats.record(AuthDecisionStats.Outcome.DENIED);
        stats.record(AuthDecisionStats.Outcome.EXPIRED);

        var report = stats.snapshot();
        assertThat(report.total()).isEqualTo(5);
        assertThat(report.outcomes().get(0).outcome()).isEqualTo(AuthDecisionStats.Outcome.GRANTED);
        assertThat(report.outcomes().get(0).share()).isCloseTo(0.6, within(1e-9));
        assertThat(report.outcomes()).hasSize(3);
    }

    @Test
    void credentialManagementSignals() {
        AuthDecisionStats stats = new AuthDecisionStats();
        stats.record(AuthDecisionStats.Outcome.CONSUMED);
        stats.record(AuthDecisionStats.Outcome.EXPIRED);
        stats.record(AuthDecisionStats.Outcome.UNKNOWN);

        var report = stats.snapshot();
        // 凭据管理类（EXPIRED+CONSUMED+UNKNOWN）占比可从行数据推导
        long credIssues = report.outcomes().stream()
                .filter(o -> o.outcome() != AuthDecisionStats.Outcome.GRANTED
                        && o.outcome() != AuthDecisionStats.Outcome.DENIED)
                .mapToLong(AuthDecisionStats.OutcomeCount::count).sum();
        assertThat(credIssues).isEqualTo(3);
    }

    @Test
    void nullIgnoredEmptyTruth() {
        AuthDecisionStats stats = new AuthDecisionStats();
        stats.record(null);
        var report = stats.snapshot();
        assertThat(report.total()).isZero();
        assertThat(report.outcomes()).isEmpty();
    }
}
