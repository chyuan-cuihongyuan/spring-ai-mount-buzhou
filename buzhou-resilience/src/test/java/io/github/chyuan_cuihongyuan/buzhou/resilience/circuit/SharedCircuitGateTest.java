package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.CircuitBreakerStateBackend;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 共享熔断闸红队（spec 57 §B / T256）：双 breaker 共享后端——A 跳闸 B 拒（本地 CLOSED
 * 分支被共享标记拦截）；A 半开探测达标恢复 → 共享清除 → B 放行；冷却期满首见方转探测；
 * 后端故障降级本地语义；无后端行为与现状全同。
 */
class SharedCircuitGateTest {

    /** TTL 语义伪后端：expiry 到点即失效（毫秒时钟驱动，免真等待）。 */
    static final class FakeTtlBackend implements CircuitBreakerStateBackend {
        record Entry(String value, long expiresAtMs) {
        }

        final ConcurrentHashMap<String, Entry> keys = new ConcurrentHashMap<>();
        final AtomicInteger failures = new AtomicInteger(); // 注入故障计数
        volatile boolean broken = false;
        private final java.util.function.LongSupplier nowMs;

        FakeTtlBackend(java.util.function.LongSupplier nowMs) {
            this.nowMs = nowMs;
        }

        @Override
        public void recordTrip(String modelName, Instant openedAt, long cooldownMs, int consecutiveTrips) {
            if (broken) {
                failures.incrementAndGet();
                throw new IllegalStateException("backend down");
            }
            keys.put(modelName, new Entry(consecutiveTrips + "@" + openedAt.toEpochMilli(),
                    nowMs.getAsLong() + cooldownMs));
        }

        @Override
        public Optional<TripMarker> activeTrip(String modelName) {
            if (broken) {
                failures.incrementAndGet();
                throw new IllegalStateException("backend down");
            }
            Entry e = keys.get(modelName);
            if (e == null || nowMs.getAsLong() >= e.expiresAtMs()) {
                return Optional.empty();
            }
            int sep = e.value().indexOf('@');
            return Optional.of(new TripMarker(Instant.ofEpochMilli(Long.parseLong(e.value().substring(sep + 1))),
                    e.expiresAtMs() - nowMs.getAsLong(), Integer.parseInt(e.value().substring(0, sep))));
        }

        @Override
        public void clear(String modelName) {
            if (broken) {
                failures.incrementAndGet();
                throw new IllegalStateException("backend down");
            }
            keys.remove(modelName);
        }

        @Override
        public String kind() {
            return "fake";
        }
    }

    private static ResilienceProperties.Circuit circuit(int window, int minCalls, double threshold,
            long cooldownMs, int halfOpenThreshold) {
        return new ResilienceProperties.Circuit(null, window, minCalls, threshold,
                Duration.ofMillis(cooldownMs), null, null, halfOpenThreshold);
    }

    /** A 跳闸 → B（本地 CLOSED）按共享 OPEN 拒绝；冷却期满 B 放行探测。 */
    @Test
    void instanceBTripConductsFromA() {
        FakeTtlBackend backend = new FakeTtlBackend(System::currentTimeMillis);
        ResilienceProperties.Circuit cfg = circuit(10, 2, 0.5, 60_000, 1);
        ModelCircuitBreaker a = new ModelCircuitBreaker(cfg, new ResilienceStats(),
                java.time.Clock.systemUTC(), backend);
        ModelCircuitBreaker b = new ModelCircuitBreaker(cfg, new ResilienceStats(),
                java.time.Clock.systemUTC(), backend);

        // A 本地跳闸（2 连败 ≥ 50%）
        a.recordTerminal("m", "NETWORK", null);
        a.recordTerminal("m", "NETWORK", null);
        assertThat(a.state("m")).isEqualTo(CircuitState.OPEN);
        assertThat(backend.keys).containsKey("m"); // 共享标记已写

        // B 本地仍 CLOSED，但共享闸拦截（OPEN 口径 + 剩余冷却）
        assertThat(b.state("m")).isEqualTo(CircuitState.CLOSED);
        assertThatThrownBy(() -> b.beforeCall("m", null))
                .isInstanceOf(ModelCircuitOpenException.class)
                .hasMessageContaining("m");
        assertThat(backend.keys).containsKey("m");

        // 冷却期满（移除标记模拟 TTL 到点）：B 放行（首见实例正常通行）
        backend.keys.remove("m");
        b.beforeCall("m", null); // 不抛
    }

    /** 可推进测试时钟（冷却/半开零真实等待；backend TTL 与 breaker 同源）。 */
    static final class MutableClock extends java.time.Clock {
        private Instant now = Instant.now();

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    /** A 半开探测达标恢复 → 共享清除 → B 放行（全实例同步恢复）。 */
    @Test
    void recoveryClearsSharedMarker() {
        MutableClock clock = new MutableClock();
        FakeTtlBackend backend = new FakeTtlBackend(clock::millis);
        ResilienceProperties.Circuit cfg = circuit(10, 2, 0.5, 60_000, 1);
        ModelCircuitBreaker a = new ModelCircuitBreaker(cfg, new ResilienceStats(), clock, backend);
        ModelCircuitBreaker b = new ModelCircuitBreaker(cfg, new ResilienceStats(), clock, backend);

        a.recordTerminal("m", "NETWORK", null);
        a.recordTerminal("m", "NETWORK", null);
        assertThat(backend.keys).containsKey("m");

        // A 冷却期满（时钟推进）→ 半开探测成功 → CLOSED（halfOpenSuccessThreshold=1）→ 共享清除
        clock.advanceMillis(60_001);
        a.beforeCall("m", null); // 转 HALF_OPEN 放行探测
        a.recordSuccess("m", null);
        assertThat(a.state("m")).isEqualTo(CircuitState.CLOSED);
        assertThat(backend.keys).doesNotContainKey("m"); // 共享标记已清

        b.beforeCall("m", null); // B 同步放行
    }

    /** 后端故障降级：activeTrip 抛异常 → 按本地语义裁决（CLOSED 放行），不放大为服务故障。 */
    @Test
    void backendFailureDegradesToLocalSemantics() {
        FakeTtlBackend backend = new FakeTtlBackend(System::currentTimeMillis);
        ResilienceStats statsA = new ResilienceStats();
        ModelCircuitBreaker a = new ModelCircuitBreaker(circuit(10, 2, 0.5, 60_000, 1), statsA,
                java.time.Clock.systemUTC(), backend);

        a.recordTerminal("m", "NETWORK", null);
        a.recordTerminal("m", "NETWORK", null);
        backend.broken = true; // 共享面故障注入

        // 本地 OPEN 语义照常（本地状态机优先）
        assertThatThrownBy(() -> a.beforeCall("m", null)).isInstanceOf(ModelCircuitOpenException.class);

        // 冷却期满后本地恢复路径也不受共享面故障影响（clear 失败仅 WARN）
        ModelCircuitBreaker fresh = new ModelCircuitBreaker(circuit(10, 2, 0.5, 60_000, 1),
                new ResilienceStats(), java.time.Clock.systemUTC(), backend);
        fresh.beforeCall("m", null); // activeTrip 故障 → 降级无标记 → CLOSED 放行
        assertThat(backend.failures.get()).isGreaterThan(0); // 故障确实发生并被吞掉
    }

    /** 无后端（null）= 进程语义零变化：跳闸不写共享面（无可写对象），行为回归既有测试面。 */
    @Test
    void nullBackendKeepsProcessSemantics() {
        ModelCircuitBreaker solo = new ModelCircuitBreaker(circuit(10, 2, 0.5, 60_000, 1),
                new ResilienceStats(), java.time.Clock.systemUTC(), null);
        solo.recordTerminal("m", "NETWORK", null);
        solo.recordTerminal("m", "NETWORK", null);
        assertThatThrownBy(() -> solo.beforeCall("m", null)).isInstanceOf(ModelCircuitOpenException.class);
        assertThat(solo.state("m")).isEqualTo(CircuitState.OPEN);
    }

    /** 连续跳闸数跨实例延续观测：A 二次跳闸后共享标记 trips=2（退避倍数跨实例可见）。 */
    @Test
    void consecutiveTripsSharedForBackoffVisibility() {
        MutableClock clock = new MutableClock();
        FakeTtlBackend backend = new FakeTtlBackend(clock::millis);
        ResilienceProperties.Circuit cfg = circuit(10, 2, 0.5, 60_000, 1);
        ModelCircuitBreaker a = new ModelCircuitBreaker(cfg, new ResilienceStats(), clock, backend);

        a.recordTerminal("m", "NETWORK", null);
        a.recordTerminal("m", "NETWORK", null); // trips=1
        // 冷却期满（时钟推进）→ HALF_OPEN 探测失败 → OPEN，trips=2
        clock.advanceMillis(60_001);
        a.beforeCall("m", null); // HALF_OPEN 探测放行
        a.recordTerminal("m", "NETWORK", null); // 探测失败 → OPEN，trips=2
        assertThat(backend.activeTrip("m")).isPresent()
                .get().extracting(CircuitBreakerStateBackend.TripMarker::consecutiveTrips).isEqualTo(2);
    }
}
