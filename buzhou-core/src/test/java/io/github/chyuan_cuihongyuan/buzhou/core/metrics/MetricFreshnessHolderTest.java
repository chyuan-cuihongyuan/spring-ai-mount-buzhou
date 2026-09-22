package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 指标新鲜度 Holder 接线测试（spec 1614 / T2379–T2380 / impl 1167）：
 * 装配链包装语义（tracker 装饰 counter/timer 写入 touch）+ audit 静态便捷面
 * （spec 802 孤类接线）。
 */
class MetricFreshnessHolderTest {

    @BeforeEach
    @AfterEach
    void reset() {
        // 前置清场：套件里更早的 Spring 上下文测试（BuzhouCoreAutoConfiguration
        // 装配链）会往全局 Holder 注入 tracker——隔离断言须顺序无关
        MetricFreshnessHolder.install(null);
    }

    static final class MutableClock extends Clock {
        private volatile Instant instant = Instant.parse("2026-09-15T00:00:00Z");

        void advanceMillis(long ms) {
            instant = instant.plusMillis(ms);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void wrappedWritesRefreshAndAuditReportsStale() {
        MutableClock clock = new MutableClock();
        BuzhouMetrics delegate = BuzhouMetrics.noop();
        MetricFreshnessTracker tracker = new MetricFreshnessTracker(delegate, clock);
        MetricFreshnessHolder.install(tracker);

        tracker.counter("buzhou.active.counter", 1);
        tracker.counter("buzhou.dead.counter", 1);
        clock.advanceMillis(60_000);
        tracker.counter("buzhou.active.counter", 1); // 持续写入保持新鲜
        clock.advanceMillis(20_000); // active 年龄 20s < 30s 新鲜；dead 年龄 80s 陈旧

        MetricFreshnessTracker.FreshnessReport report =
                MetricFreshnessHolder.audit(clock.millis(), 30_000).orElseThrow();
        assertThat(report.trackedNames()).isEqualTo(2);
        assertThat(report.stale()).hasSize(1);
        assertThat(report.stale().get(0).name()).isEqualTo("buzhou.dead.counter");
        assertThat(report.stale().get(0).ageMillis()).isEqualTo(80_000);
    }

    @Test
    void auditWithoutTrackerIsEmpty() {
        assertThat(MetricFreshnessHolder.audit(0, 1000)).isEmpty();
        assertThat(MetricFreshnessHolder.tracker()).isEmpty();
    }
}
