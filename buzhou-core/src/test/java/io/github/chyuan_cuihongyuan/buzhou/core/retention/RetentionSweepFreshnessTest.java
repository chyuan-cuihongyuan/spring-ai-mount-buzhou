package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1427 / T2156：保留清扫新鲜度追踪器——计数/末次时刻/间隔水位/失败
 * 分桶/stale 哨兵；经既有 addSweepListener seam 零侵入挂载。
 */
class RetentionSweepFreshnessTest {

    private static RetentionSweepReport report(Instant at, int deleted, List<String> failures) {
        return new RetentionSweepReport(at, deleted, 0, 0, 0, 0,
                Map.of(), failures);
    }

    @Test
    void freshTrackerReportsNeverSweptSentinels() {
        RetentionSweepFreshness freshness = new RetentionSweepFreshness();
        var s = freshness.freshness(Instant.parse("2026-09-14T12:00:00Z"));
        assertThat(s.sweepCount()).isZero();
        assertThat(s.lastSweepAt()).isEqualTo(-1);
        assertThat(s.staleMillis()).isEqualTo(-1);
        assertThat(s.maxGapMillis()).isZero();
        assertThat(s.failureCount()).isZero();
    }

    @Test
    void sweepsUpdateCountLastAtAndGaps() {
        RetentionSweepFreshness freshness = new RetentionSweepFreshness();
        freshness.accept(report(Instant.parse("2026-09-14T12:00:00Z"), 3, List.of()));
        freshness.accept(report(Instant.parse("2026-09-14T12:10:00Z"), 0, List.of()));
        // 间隔 10 分钟；末次 12:10
        var s = freshness.freshness(Instant.parse("2026-09-14T12:12:00Z"));
        assertThat(s.sweepCount()).isEqualTo(2);
        assertThat(s.lastSweepAt()).isEqualTo(Instant.parse("2026-09-14T12:10:00Z").toEpochMilli());
        assertThat(s.staleMillis()).isEqualTo(Duration.ofMinutes(2).toMillis());
        assertThat(s.maxGapMillis()).isEqualTo(Duration.ofMinutes(10).toMillis());
        assertThat(s.failureCount()).isZero();
    }

    @Test
    void failedSweepsCountedSeparately() {
        RetentionSweepFreshness freshness = new RetentionSweepFreshness();
        freshness.accept(report(Instant.parse("2026-09-14T12:00:00Z"), 0,
                List.of("step-x 失败")));
        freshness.accept(report(Instant.parse("2026-09-14T12:05:00Z"), 1, List.of()));
        var s = freshness.freshness(Instant.parse("2026-09-14T12:06:00Z"));
        assertThat(s.sweepCount()).isEqualTo(2);
        assertThat(s.failureCount()).isEqualTo(1);
    }

    @Test
    void maxGapMonotonicAcrossOutliers() {
        RetentionSweepFreshness freshness = new RetentionSweepFreshness();
        freshness.accept(report(Instant.parse("2026-09-14T12:00:00Z"), 0, List.of()));
        freshness.accept(report(Instant.parse("2026-09-14T13:00:00Z"), 0, List.of())); // 间隔 60 分
        freshness.accept(report(Instant.parse("2026-09-14T13:01:00Z"), 0, List.of())); // 间隔 1 分
        // 水位不因后续短间隔回退
        assertThat(freshness.freshness(Instant.parse("2026-09-14T13:02:00Z"))
                .maxGapMillis()).isEqualTo(Duration.ofMinutes(60).toMillis());
    }

    @Test
    void resetForTestClearsAllState() {
        RetentionSweepFreshness freshness = new RetentionSweepFreshness();
        freshness.accept(report(Instant.parse("2026-09-14T12:00:00Z"), 0, List.of()));
        freshness.resetForTest();
        assertThat(freshness.freshness(Instant.parse("2026-09-14T12:00:00Z")))
                .isEqualTo(new RetentionSweepFreshness.Snapshot(0, -1, -1, 0, 0));
    }
}
