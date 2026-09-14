package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import io.github.chyuan_cuihongyuan.buzhou.resilience.CircuitCrashLoopDetector;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.HalfOpenProbeStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 熔断旁路遥测接线测试（spec 1611 / T2373–T2374 / impl 1164）：跳闸喂
 * crash-loop recordOpen、半开恢复喂 recordRecovery（spec 811 指定挂点）；
 * 半开探测成败喂 probeStats（spec 836 指定挂点）——两个孤类自装配侧复活。
 */
class CircuitTelemetryWiringTest {

    private static final Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> SINK = e -> { };

    static final class MutableClock extends java.time.Clock {
        private volatile Instant instant = Instant.parse("2026-09-15T00:00:00Z");

        void advance(Duration d) {
            instant = instant.plus(d);
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
        public java.time.Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    /** 5 失败即满阈值跳闸。 */
    private static void trip(ModelCircuitBreaker breaker, String model) {
        for (int i = 0; i < 5; i++) {
            breaker.recordTerminal(model, "NETWORK", SINK);
        }
    }

    @Test
    void openTransitionsFeedCrashLoopDetector() {
        MutableClock clock = new MutableClock();
        CircuitCrashLoopDetector detector = new CircuitCrashLoopDetector(2, 10 * 60 * 1000L);
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new io.github.chyuan_cuihongyuan
                .buzhou.resilience.config.ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(1), null), null, clock)
                .withTelemetry(detector, null);

        trip(breaker, "m");
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
        assertThat(detector.isLooping("m")).isFalse(); // 1 次跳闸未达循环阈

        clock.advance(Duration.ofSeconds(2)); // 冷却过 → 半开 → 再跳（探测失败）
        // 触发半开探测并失败：直接在 HALF_OPEN 状态记终态失败
        breaker.beforeCall("m", SINK); // OPEN 冷却过 → 转 HALF_OPEN 放行
        breaker.recordTerminal("m", "NETWORK", SINK); // 探测失败 → 回 OPEN
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);

        clock.advance(Duration.ofSeconds(2));
        breaker.beforeCall("m", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK); // 窗口内第 3 次 OPEN → 循环闩锁
        assertThat(detector.isLooping("m")).isTrue();
    }

    @Test
    void halfOpenProbeOutcomesFeedProbeStats() {
        MutableClock clock = new MutableClock();
        HalfOpenProbeStats probeStats = new HalfOpenProbeStats();
        CircuitCrashLoopDetector detector = new CircuitCrashLoopDetector(2, 10 * 60 * 1000L);
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new io.github.chyuan_cuihongyuan
                .buzhou.resilience.config.ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(1), null), null, clock)
                .withTelemetry(detector, probeStats);

        trip(breaker, "m"); // OPEN
        clock.advance(Duration.ofSeconds(2));
        breaker.beforeCall("m", SINK); // HALF_OPEN
        breaker.recordTerminal("m", "NETWORK", SINK); // 探测失败 → OPEN，probeStats 记败
        // minOpens=2：窗口内两次 OPEN（首跳 + 探测失败回跳）即循环闩锁——crash-loop 语义
        assertThat(breaker.crashLoopDetector().isLooping("m")).isTrue();

        clock.advance(Duration.ofSeconds(2));
        breaker.beforeCall("m", SINK); // HALF_OPEN
        breaker.recordSuccess("m", SINK); // 探测成功 → CLOSED（阈值 1），probeStats 记成
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);

        HalfOpenProbeStats.ProbeStats stats = probeStats.stats("m");
        assertThat(stats.failures()).isEqualTo(1);
        assertThat(stats.successes()).isEqualTo(1);
        // 半开恢复同时清 crash-loop 闩锁（spec 811：恢复=容器成功运行）
        assertThat(detector.isLooping("m")).isFalse();
    }

    @Test
    void withoutTelemetryZeroBehaviorUnchanged() {
        MutableClock clock = new MutableClock();
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new io.github.chyuan_cuihongyuan
                .buzhou.resilience.config.ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(1), null), null, clock);
        assertThat(breaker.crashLoopDetector()).isNull();
        assertThat(breaker.halfOpenProbeStats()).isNull();
        trip(breaker, "m");
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
    }
}
