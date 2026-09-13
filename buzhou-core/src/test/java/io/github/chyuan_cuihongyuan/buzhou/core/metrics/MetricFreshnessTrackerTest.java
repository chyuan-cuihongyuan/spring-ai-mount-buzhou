package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 802 / T1106：指标新鲜度回归——写入刷新/陈旧判定/gauge 不追踪/
 * 名字封顶/委托透传/参数 fail-fast。
 */
class MetricFreshnessTrackerTest {

    /** 可拨动测试时钟。 */
    private static final class MutableClock extends Clock {
        long now = 1_000_000L;

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(now);
        }
    }

    private final MutableClock clock = new MutableClock();
    private final AtomicInteger counters = new AtomicInteger();
    private final AtomicInteger timers = new AtomicInteger();

    private BuzhouMetrics countingDelegate() {
        return new BuzhouMetrics() {
            @Override
            public void counter(String name, long delta, String... tagKeyValue) {
                counters.incrementAndGet();
            }

            @Override
            public void timer(String name, Duration duration, String... tagKeyValue) {
                timers.incrementAndGet();
            }
        };
    }

    @Test
    void writesRefreshTimestampsAndDelegateReceives() {
        MetricFreshnessTracker tracker = new MetricFreshnessTracker(countingDelegate(), clock);

        tracker.counter("buzhou.a.x", 1);
        clock.now += 100;
        tracker.timer("buzhou.a.y", Duration.ofMillis(5));

        assertThat(counters.get()).isEqualTo(1);
        assertThat(timers.get()).isEqualTo(1);
        assertThat(tracker.lastWrites()).containsOnlyKeys("buzhou.a.x", "buzhou.a.y");
        assertThat(tracker.lastWrites().get("buzhou.a.y")).isEqualTo(1_000_100L);

        clock.now += 100;
        tracker.counter("buzhou.a.x", 1); // 刷新不新增
        assertThat(tracker.lastWrites()).hasSize(2);
        assertThat(tracker.lastWrites().get("buzhou.a.x")).isEqualTo(1_000_200L);
    }

    @Test
    void auditFlagsStaleSortedByAgeDesc() {
        MetricFreshnessTracker tracker = new MetricFreshnessTracker(countingDelegate(), clock);

        tracker.counter("buzhou.old.one");
        clock.now += 5_000;
        tracker.counter("buzhou.old.two");
        clock.now += 5_000;
        tracker.counter("buzhou.fresh");

        MetricFreshnessTracker.FreshnessReport report = tracker.audit(clock.now, 3_000);
        assertThat(report.trackedNames()).isEqualTo(3);
        assertThat(report.stale()).hasSize(2);
        assertThat(report.stale().get(0).name()).isEqualTo("buzhou.old.one"); // 年龄最大在前
        assertThat(report.stale().get(0).ageMillis()).isEqualTo(10_000);
        assertThat(report.stale().get(1).ageMillis()).isEqualTo(5_000);
        assertThat(report.truncated()).isFalse();
    }

    @Test
    void gaugeIsNotTracked() {
        MetricFreshnessTracker tracker = new MetricFreshnessTracker(countingDelegate(), clock);
        tracker.gauge("buzhou.g.x", () -> 42);
        assertThat(tracker.lastWrites()).isEmpty();
    }

    @Test
    void trackedNamesAreCapped() {
        MetricFreshnessTracker tracker = new MetricFreshnessTracker(countingDelegate(), clock);
        for (int i = 0; i < MetricFreshnessTracker.MAX_NAMES + 20; i++) {
            tracker.counter("buzhou.n." + i);
        }
        assertThat(tracker.lastWrites()).hasSize(MetricFreshnessTracker.MAX_NAMES);
        assertThat(tracker.audit(clock.now, 1).truncated()).isTrue();
    }

    @Test
    void blankNamesIgnoredAndStaleListCapped() {
        MetricFreshnessTracker tracker = new MetricFreshnessTracker(countingDelegate(), clock);
        tracker.counter("", 1);
        tracker.counter(null, 1);
        assertThat(tracker.lastWrites()).isEmpty();

        MetricFreshnessTracker capped = new MetricFreshnessTracker(new BuzhouMetrics() {
            @Override
            public void counter(String name, long delta, String... tagKeyValue) {
            }

            @Override
            public void timer(String name, Duration duration, String... tagKeyValue) {
            }
        }, clock);
        for (int i = 0; i < MetricFreshnessTracker.STALE_LIST_LIMIT + 10; i++) {
            capped.counter("buzhou.s." + i);
        }
        assertThat(capped.audit(clock.now + 999_999, 1).stale())
                .hasSize(MetricFreshnessTracker.STALE_LIST_LIMIT);
    }

    @Test
    void failFastOnBadStaleAfter() {
        MetricFreshnessTracker tracker = new MetricFreshnessTracker(countingDelegate(), clock);
        assertThatThrownBy(() -> tracker.audit(clock.now, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MetricFreshnessTracker(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MetricFreshnessTracker(countingDelegate(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
