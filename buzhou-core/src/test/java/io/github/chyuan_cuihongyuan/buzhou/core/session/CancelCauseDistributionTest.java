package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 824 / T1150：取消原因分布回归——计数降序+份额/dominant/lastSeen 取 max/
 * null 忽略/空报告。
 */
class CancelCauseDistributionTest {

    @Test
    void countsSharesDominantAndLastSeen() {
        CancelCauseDistribution dist = new CancelCauseDistribution();
        dist.record(CancelCause.USER, 100);
        dist.record(CancelCause.USER, 300);
        dist.record(CancelCause.DEADLINE, 200);
        dist.record(CancelCause.USER, 250); // 同因多次——lastSeen 取 max

        CancelCauseDistribution.Report report = dist.snapshot();
        assertThat(report.total()).isEqualTo(4);
        assertThat(report.dominant()).isEqualTo(CancelCause.USER);
        assertThat(report.counts().get(0).cause()).isEqualTo(CancelCause.USER);
        assertThat(report.counts().get(0).count()).isEqualTo(3);
        assertThat(report.counts().get(0).share()).isCloseTo(0.75, within(1e-9));
        assertThat(report.counts().get(0).lastSeenMillis()).isEqualTo(300);
        assertThat(report.counts().get(1).cause()).isEqualTo(CancelCause.DEADLINE);
        assertThat(report.counts().get(1).lastSeenMillis()).isEqualTo(200);
    }

    @Test
    void nullIgnoredAndEmptyReport() {
        CancelCauseDistribution dist = new CancelCauseDistribution();
        dist.record(null, 1);
        CancelCauseDistribution.Report report = dist.snapshot();
        assertThat(report.total()).isZero();
        assertThat(report.counts()).isEmpty();
        assertThat(report.dominant()).isNull();
    }

    @Test
    void tieStableByInsertionOrder() {
        CancelCauseDistribution dist = new CancelCauseDistribution();
        dist.record(CancelCause.RUNAWAY, 1);
        dist.record(CancelCause.LEASE_LOST, 2);
        var report = dist.snapshot();
        // 计数 1:1——EnumMap 按枚举声明序迭代，best 首达者保持（声明序 LEASE_LOST < RUNAWAY）
        assertThat(report.counts()).hasSize(2);
        assertThat(report.dominant()).isEqualTo(CancelCause.LEASE_LOST);
    }
}
