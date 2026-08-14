package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-41 / spec 13 §T66 指标面：noop 零开销、micrometer 实现计数/时长/gauge、
 * binder 预注册标准集、holder 装配生命周期、tag 成对校验。
 */
class BuzhouMetricsHolderTest {

    @AfterEach
    void reset() {
        BuzhouMetricsHolder.reset();
    }

    @Test
    void noopByDefault() {
        BuzhouMetrics noop = BuzhouMetricsHolder.metrics();
        noop.counter("buzhou.any"); // 不抛即零开销
        noop.timer("buzhou.any.timer", Duration.ofMillis(5));
        assertThat(BuzhouMetricsHolder.metrics()).isSameAs(noop);
    }

    @Test
    void micrometerImplementationRecordsCounterAndTimer() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BuzhouMetrics metrics = new MicrometerBuzhouMetrics(registry);

        metrics.counter("buzhou.eventbus.dropped");
        metrics.counter("buzhou.eventbus.dropped");
        metrics.timer("buzhou.turn.duration", Duration.ofMillis(120), "outcome", "ok");

        assertThat(registry.counter("buzhou.eventbus.dropped").count()).isEqualTo(2.0);
        assertThat(registry.timer("buzhou.turn.duration", "outcome", "ok").count()).isEqualTo(1);
        assertThat(registry.timer("buzhou.turn.duration", "outcome", "ok").totalTime(
                java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(120);
    }

    @Test
    void holderInstallWiresGlobalAccess() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BuzhouMetricsHolder.install(new MicrometerBuzhouMetrics(registry));
        BuzhouMetricsHolder.metrics().counter("buzhou.guard.checks", "outcome", "blocked");
        assertThat(registry.counter("buzhou.guard.checks", "outcome", "blocked").count())
                .isEqualTo(1.0);
    }

    @Test
    void binderPreRegistersStandardSetAtZero() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new BuzhouMetricsBinder().bindTo(registry);
        // 启动即零值可见（面板完整序列），且 outcome 枚举组合齐全
        assertThat(registry.counter("buzhou.tool.calls", "outcome", "ok").count()).isZero();
        assertThat(registry.counter("buzhou.tool.calls", "outcome", "failed").count()).isZero();
        assertThat(registry.counter("buzhou.spill.requests", "outcome", "spilled").count()).isZero();
        assertThat(registry.counter("buzhou.guard.checks", "outcome", "escalated").count()).isZero();
        assertThat(registry.counter("buzhou.store.write.failures", "policy", "degrade").count())
                .isZero();
        assertThat(registry.find("buzhou.turn.duration").timer()).isNotNull();
    }

    @Test
    void oddTagPairsRejected() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BuzhouMetrics metrics = new MicrometerBuzhouMetrics(registry);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> metrics.counter("buzhou.x", "only-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("成对");
    }
}
