package io.github.chyuan_cuihongyuan.buzhou.core.leak;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 839 / T1180：泄漏疑似聚合回归——同键计数+最大龄/排行/溢出桶/脏报告/空真。
 */
class LeakSuspectAggregatorTest {

    private static ResourceLeakDetector.LeakReport report(String desc, long age) {
        return new ResourceLeakDetector.LeakReport(desc, age, null);
    }

    @Test
    void aggregatesByKeyWithMaxAge() {
        LeakSuspectAggregator agg = new LeakSuspectAggregator();
        agg.onLeak(report("spill-handle", 100));
        agg.onLeak(report("spill-handle", 500));
        agg.onLeak(report("span-recorder", 50));

        var snap = agg.snapshot();
        assertThat(snap.totalLeaks()).isEqualTo(3);
        assertThat(snap.suspects().get(0).key()).isEqualTo("spill-handle");
        assertThat(snap.suspects().get(0).count()).isEqualTo(2);
        assertThat(snap.suspects().get(0).maxAgeMillis()).isEqualTo(500); // 最大龄
        assertThat(snap.suspects().get(1).key()).isEqualTo("span-recorder");
        assertThat(snap.truncated()).isFalse();
    }

    @Test
    void longDescriptionsTruncatedToStableKey() {
        LeakSuspectAggregator agg = new LeakSuspectAggregator();
        String longDesc = "leak:".concat("x".repeat(200));
        agg.onLeak(report(longDesc, 10));
        agg.onLeak(report(longDesc, 20));
        var snap = agg.snapshot();
        assertThat(snap.suspects()).hasSize(1);
        assertThat(snap.suspects().get(0).key()).hasSize(LeakSuspectAggregator.KEY_MAX);
        assertThat(snap.suspects().get(0).count()).isEqualTo(2);
    }

    @Test
    void overflowBucketAfterCap() {
        LeakSuspectAggregator agg = new LeakSuspectAggregator();
        for (int i = 0; i < LeakSuspectAggregator.MAX_KEYS; i++) {
            agg.onLeak(report("type" + i, i));
        }
        agg.onLeak(report("new-type", 5));

        var snap = agg.snapshot();
        assertThat(snap.suspects()).hasSize(LeakSuspectAggregator.MAX_KEYS + 1);
        assertThat(snap.suspects().stream()
                .anyMatch(s -> s.key().equals(LeakSuspectAggregator.OVERFLOW))).isTrue();
        assertThat(snap.truncated()).isTrue();
    }

    @Test
    void dirtyReportsAndEmptyTruth() {
        LeakSuspectAggregator agg = new LeakSuspectAggregator();
        agg.onLeak(null);
        agg.onLeak(report(null, 1));
        agg.onLeak(report("  ", 1));
        var snap = agg.snapshot();
        assertThat(snap.totalLeaks()).isZero();
        assertThat(snap.suspects()).isEmpty();
        assertThat(snap.truncated()).isFalse();
    }
}
