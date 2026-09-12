package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 熔断时间窗衰减测试（spec 620 / T890–T891 / impl 473，resilience4j TIME-based
 * sliding window 思想）：老样本按时间出率计算与 min-calls 门；timeWindow=0 = count 窗零变化。
 */
class CircuitTimeWindowTest {

    private static final Consumer<SessionEvent> SINK = e -> {
    };

    /** 可推进时钟（ClockInjectionTest 同款）。 */
    static final class MutableClock extends java.time.Clock {
        private volatile Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    /** timeWindow=60s：t0 四失败陈旧化后，t=61s 的一失败只见 1 新样本（< minCalls 不跳闸）。 */
    @Test
    void agedFailuresDropOutOfRateWithTimeWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-12T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null,
                Duration.ofMinutes(1)), null, clock);

        for (int i = 0; i < 4; i++) {
            breaker.recordTerminal("m", "NETWORK", SINK); // t0：四失败（4 < minCalls 不跳）
        }
        clock.advance(Duration.ofSeconds(61));            // 四失败陈旧出窗
        breaker.recordTerminal("m", "NETWORK", SINK);     // 新样本仅 1——< minCalls 不跳闸
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    /** timeWindow=0（默认）：同序列走 count 窗——5 样本 5 失败 ≥ 0.5 照跳（零变化）。 */
    @Test
    void countWindowSemanticsUnchangedWithoutTimeWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-12T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null), null, clock);

        for (int i = 0; i < 4; i++) {
            breaker.recordTerminal("m", "NETWORK", SINK);
        }
        clock.advance(Duration.ofSeconds(61)); // 陈旧性不影响 count 窗
        breaker.recordTerminal("m", "NETWORK", SINK); // 5/5 = 1.0 ≥ 0.5
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
    }

    /** 时间窗内的新失败仍正常积累跳闸（衰减不是豁免）。 */
    @Test
    void freshFailuresStillTripWithinWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-12T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null,
                Duration.ofMinutes(10)), null, clock);

        for (int i = 0; i < 5; i++) {
            breaker.recordTerminal("m", "NETWORK", SINK); // 窗内五失败
        }
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
    }
}
