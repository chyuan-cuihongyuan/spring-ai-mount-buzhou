package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * BuzhouMetrics.compose 聚合广播直测（spec 1200 / T1801 / K 会话 R1 补测——
 * CompositeBuzhouMetrics 此前零覆盖）。
 *
 * <p>记录型 fake 断言 counter/timer/gauge 全量广播、tag 数组原样透传、空聚合无操作、
 * default counter(name, tags) 委托 delta=1 与 noop 零开销面。
 */
class BuzhouMetricsComposeTest {

    private static final String METRIC_NAME = "buzhou.eventbus.dropped";
    private static final String[] TAGS = {"reason", "block-timeout"};

    /** 记录型 BuzhouMetrics fake：三个面全部记录（gauge 覆写 default no-op）。 */
    private static final class RecordingMetrics implements BuzhouMetrics {
        private final String id;
        private final List<String> seen = new ArrayList<>();

        private RecordingMetrics(String id) {
            this.id = id;
        }

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            seen.add(id + "|counter|" + name + "|" + delta + "|" + String.join(",", tagKeyValue));
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            seen.add(id + "|timer|" + name + "|" + duration.toMillis() + "|" + String.join(",", tagKeyValue));
        }

        @Override
        public void gauge(String name, java.util.function.Supplier<Number> value,
                          String... tagKeyValue) {
            seen.add(id + "|gauge|" + name + "|" + value.get() + "|" + String.join(",", tagKeyValue));
        }
    }

    @Test
    void counterAndTimerBroadcastToAllDelegates() {
        RecordingMetrics a = new RecordingMetrics("A");
        RecordingMetrics b = new RecordingMetrics("B");
        BuzhouMetrics composed = BuzhouMetrics.compose(a, b);

        composed.counter(METRIC_NAME, 3, TAGS);
        composed.timer(METRIC_NAME, Duration.ofMillis(25), TAGS);
        composed.gauge(METRIC_NAME, () -> 7, TAGS);

        assertThat(a.seen).containsExactly(
                "A|counter|" + METRIC_NAME + "|3|reason,block-timeout",
                "A|timer|" + METRIC_NAME + "|25|reason,block-timeout",
                "A|gauge|" + METRIC_NAME + "|7|reason,block-timeout");
        assertThat(b.seen).containsExactly(
                "B|counter|" + METRIC_NAME + "|3|reason,block-timeout",
                "B|timer|" + METRIC_NAME + "|25|reason,block-timeout",
                "B|gauge|" + METRIC_NAME + "|7|reason,block-timeout");
    }

    @Test
    void defaultCounterWithoutDeltaDelegatesToOne() {
        RecordingMetrics a = new RecordingMetrics("A");
        BuzhouMetrics.compose(a).counter(METRIC_NAME, TAGS);
        assertThat(a.seen).containsExactly("A|counter|" + METRIC_NAME + "|1|reason,block-timeout");
    }

    @Test
    void emptyComposeIsSafeNoop() {
        BuzhouMetrics composed = BuzhouMetrics.compose();
        assertThatCode(() -> {
            composed.counter(METRIC_NAME, 1, TAGS);
            composed.timer(METRIC_NAME, Duration.ZERO, TAGS);
            composed.gauge(METRIC_NAME, () -> 0, TAGS);
        }).doesNotThrowAnyException();
    }

    @Test
    void noopImplementationAcceptsAllFaces() {
        BuzhouMetrics noop = BuzhouMetrics.noop();
        assertThatCode(() -> {
            noop.counter(METRIC_NAME, 1, TAGS);
            noop.timer(METRIC_NAME, Duration.ofSeconds(1), TAGS);
            noop.gauge(METRIC_NAME, () -> 1, TAGS);
        }).doesNotThrowAnyException();
    }
}
