package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 845 / T1192：回滚使用读数回归——计数降序/最近版本对/溢出桶/脏入参/空真。
 */
class RollbackUsageStatsTest {

    @Test
    void countsSortedWithLastVersions() {
        RollbackUsageStats stats = new RollbackUsageStats();
        stats.record("search-prompt", 5, 4, 100);
        stats.record("search-prompt", 4, 3, 200);
        stats.record("report-prompt", 2, 1, 300);

        var report = stats.snapshot();
        assertThat(report.total()).isEqualTo(3);
        assertThat(report.prompts().get(0).name()).isEqualTo("search-prompt");
        assertThat(report.prompts().get(0).rollbacks()).isEqualTo(2);
        assertThat(report.prompts().get(0).lastFromVersion()).isEqualTo(4);
        assertThat(report.prompts().get(0).lastToVersion()).isEqualTo(3);
        assertThat(report.prompts().get(0).lastSeenMillis()).isEqualTo(200);
        assertThat(report.prompts().get(1).name()).isEqualTo("report-prompt");
    }

    @Test
    void overflowBucketBeyondCap() {
        RollbackUsageStats stats = new RollbackUsageStats();
        for (int i = 0; i < RollbackUsageStats.MAX_PROMPTS; i++) {
            stats.record("p" + i, 2, 1, i);
        }
        stats.record("new-prompt", 3, 2, 999);

        var report = stats.snapshot();
        assertThat(report.prompts()).hasSize(RollbackUsageStats.MAX_PROMPTS + 1);
        assertThat(report.prompts().stream()
                .anyMatch(p -> p.name().equals(RollbackUsageStats.OVERFLOW))).isTrue();
        assertThat(report.total()).isEqualTo(RollbackUsageStats.MAX_PROMPTS + 1);
    }

    @Test
    void dirtyInputsAndEmptyTruth() {
        RollbackUsageStats stats = new RollbackUsageStats();
        stats.record(null, 1, 0, 0);
        stats.record("  ", 1, 0, 0);
        stats.record("p", -1, 0, 0);
        var report = stats.snapshot();
        assertThat(report.total()).isZero();
        assertThat(report.prompts()).isEmpty();
    }
}
