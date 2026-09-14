package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 熔断启动宽限测试（spec 1602 / T2355–T2356 / impl 1155）：进程冷启动期失败不计
 * 开闸（K8s startupProbe 思想——startup 通过前 liveness 不生效）；宽限结束已积累
 * 样本立即恢复完整判定（真故障仍跳）；默认关零行为；负参 fail-fast。
 */
class CircuitWarmupTest {

    private static final Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> SINK = e -> { };

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

    /** 5 失败即满阈值（minCalls=5、rate 1.0 ≥ 0.5）。 */
    private static void fillWindow(ModelCircuitBreaker breaker) {
        for (int i = 0; i < 5; i++) {
            breaker.recordTerminal("m", "NETWORK", SINK);
        }
    }

    @Test
    void warmupSuppressesTripDuringGracePeriodThenTripsAfter() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-15T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new io.github.chyuan_cuihongyuan
                .buzhou.resilience.config.ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null, null,
                Duration.ofMinutes(30)), null, clock);
        fillWindow(breaker);
        // 宽限期内：判定满足但豁免——不开闸，计数可见
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
        assertThat(breaker.warmupSuppressedCount()).isEqualTo(1);
        // 宽限结束后：已积累样本恢复完整判定，下一次失败立即跳闸
        clock.advance(Duration.ofMinutes(31));
        breaker.recordTerminal("m", "NETWORK", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
    }

    @Test
    void warmupAllowsSuccessToHealWindowBeforeGraceEnds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-15T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new io.github.chyuan_cuihongyuan
                .buzhou.resilience.config.ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null, null,
                Duration.ofMinutes(30)), null, clock);
        for (int i = 0; i < 4; i++) {
            breaker.recordTerminal("m", "NETWORK", SINK);
        }
        // 宽限期内成功冲淡窗口：4 失败 + 6 成功 = 0.4 < 0.5，宽限结束后也不跳
        for (int i = 0; i < 6; i++) {
            breaker.recordSuccess("m", SINK);
        }
        clock.advance(Duration.ofMinutes(31));
        breaker.recordTerminal("m", "NETWORK", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    @Test
    void warmupOffKeepsExistingZeroChangeSemantics() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-15T00:00:00Z"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new io.github.chyuan_cuihongyuan
                .buzhou.resilience.config.ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null), null, clock);
        fillWindow(breaker);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
        assertThat(breaker.warmupSuppressedCount()).isZero();
    }

    @Test
    void negativeWarmupFailsFast() {
        assertThatThrownBy(() -> new io.github.chyuan_cuihongyuan.buzhou.resilience.config
                .ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null, null,
                Duration.ofMinutes(-1)))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class)
                .hasMessageContaining("circuit.warmup");
    }
}
