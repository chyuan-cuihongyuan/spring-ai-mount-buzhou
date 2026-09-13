package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 831 / T1164：准入拒绝分布回归——计数降序+dominant/lastSeen 取 max/
 * 原因键封顶 truncated/null 忽略/空报告。
 */
class SpawnRejectionDistributionTest {

    @Test
    void countsDescendingWithDominant() {
        SpawnRejectionDistribution dist = new SpawnRejectionDistribution();
        dist.record("timeout", 100);
        dist.record("fail-fast", 200);
        dist.record("timeout", 300);
        dist.record("drain", 400);
        dist.record("timeout", 150); // lastSeen 取 max=300

        var report = dist.snapshot();
        assertThat(report.total()).isEqualTo(5);
        assertThat(report.dominant()).isEqualTo("timeout");
        assertThat(report.counts().get(0).reason()).isEqualTo("timeout");
        assertThat(report.counts().get(0).count()).isEqualTo(3);
        assertThat(report.counts().get(0).lastSeenMillis()).isEqualTo(300);
        assertThat(report.counts().get(1).reason()).isEqualTo("fail-fast");
        assertThat(report.counts().get(2).reason()).isEqualTo("drain");
    }

    @Test
    void reasonsCappedWithTruncation() {
        SpawnRejectionDistribution dist = new SpawnRejectionDistribution();
        for (int i = 0; i < SpawnRejectionDistribution.MAX_REASONS + 4; i++) {
            dist.record("r" + i, i);
        }
        var report = dist.snapshot();
        assertThat(report.counts()).hasSize(SpawnRejectionDistribution.MAX_REASONS);
        assertThat(report.truncated()).isTrue();
        assertThat(report.total()).isEqualTo(SpawnRejectionDistribution.MAX_REASONS); // 超封顶不计数
    }

    @Test
    void nullAndBlankIgnoredEmptyReport() {
        SpawnRejectionDistribution dist = new SpawnRejectionDistribution();
        dist.record(null, 1);
        dist.record("  ", 2);
        var report = dist.snapshot();
        assertThat(report.total()).isZero();
        assertThat(report.counts()).isEmpty();
        assertThat(report.dominant()).isNull();
        assertThat(report.truncated()).isFalse();
    }
}
