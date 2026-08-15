package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 熔断器状态机单测（spec 15「熔断器」，impl-56）：只测外部行为——状态迁移、窗口口径、半开探测。
 */
class ModelCircuitBreakerTest {

    private static final Consumer<SessionEvent> SINK = e -> {
    };

    private static ResilienceProperties.Circuit circuit(int window, int minCalls, double threshold,
            long cooldownMs) {
        return new ResilienceProperties.Circuit(null, window, minCalls, threshold,
                Duration.ofMillis(cooldownMs), null);
    }

    private static ResilienceProperties.Circuit circuit(int window, int minCalls, double threshold,
            long cooldownMs, int backoffCap) {
        return new ResilienceProperties.Circuit(null, window, minCalls, threshold,
                Duration.ofMillis(cooldownMs), null, backoffCap);
    }

    /** 首次跳闸冷却 = base（倍数 1，既有行为不变）。 */
    private static long firstTripRemainingMs(ModelCircuitBreaker breaker) {
        try {
            breaker.beforeCall("m", SINK);
            return 0;
        } catch (ModelCircuitOpenException e) {
            return e.retryIn().toMillis();
        }
    }

    /** min-calls=3 配置的跳闸 helper（3 次失败样本）。 */
    private static void tripMin3(ModelCircuitBreaker breaker) {
        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
    }

    private static long remainingMs(ModelCircuitBreaker breaker) {
        try {
            breaker.beforeCall("m", SINK);
            return 0;
        } catch (ModelCircuitOpenException e) {
            return e.retryIn() == null ? -1 : e.retryIn().toMillis();
        }
    }

    // ---- 冷却自适应（spec 25 / T104 / impl-79） ----

    /** 连续探测失败：第二次跳闸冷却翻倍（base 100ms → 200ms），事件 payload 携带 trips/生效时长。 */
    @Test
    void repeatedProbeFailureDoublesCooldown() throws Exception {
        java.util.List<SessionEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        Consumer<SessionEvent> capture = events::add;
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(10, 3, 0.5, 100, 8), null);

        tripMin3(breaker); // trips=1 → 冷却 100ms
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
        Thread.sleep(120);
        breaker.beforeCall("m", capture); // 冷却过半 → HALF_OPEN 探测
        breaker.recordTerminal("m", "NETWORK", capture); // 探测失败 → trips=2 → 冷却 200ms

        long remaining = remainingMs(breaker);
        assertThat(remaining).isBetween(150L, 200L);

        SessionEvent openEvent = events.stream()
                .filter(e -> e.type().equals(ModelCircuitBreaker.EVENT_STATE_CHANGED))
                .filter(e -> "OPEN".equals(e.payload().get("to")))
                .reduce((a, b) -> b).orElseThrow();
        assertThat(openEvent.payload().get("consecutiveTrips")).isEqualTo(2);
        assertThat(openEvent.payload().get("openDurationMs")).isEqualTo(200L);
    }

    /** 半开探测成功回 CLOSED：trips 复位，再跳闸冷却回 base。 */
    @Test
    void probeSuccessResetsBackoff() throws Exception {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(10, 3, 0.5, 100, 8), null);

        tripMin3(breaker);
        Thread.sleep(120);
        breaker.beforeCall("m", SINK);
        breaker.recordSuccess("m", SINK); // 探测成功 → CLOSED，trips=0
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);

        tripMin3(breaker); // trips=1 → 冷却回 base 100ms
        assertThat(firstTripRemainingMs(breaker)).isBetween(50L, 100L);
    }

    /** backoff-cap 封顶：cap=2 时第三次跳闸冷却仍 ×2（不 ×4）。 */
    @Test
    void backoffCapLimitsMultiplier() throws Exception {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(10, 3, 0.5, 100, 2), null);

        tripMin3(breaker); // trips=1 → 100ms
        Thread.sleep(120);
        breaker.beforeCall("m", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK); // trips=2 → 200ms（min(2, cap=2)）
        Thread.sleep(210);
        breaker.beforeCall("m", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK); // trips=3 → min(4, 2)=2 → 200ms

        assertThat(remainingMs(breaker)).isBetween(150L, 200L);
    }


    // ---- 跳闸：失败率口径 ----

    /** 失败样本 ≥ min-calls 且失败率 ≥ 阈值 → OPEN；后续调用前置闸直接拒绝。 */
    @Test
    void tripsOnFailureRateThreshold() {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(10, 3, 0.5, 60_000), null);

        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED); // 样本不足 3，不跳

        breaker.recordTerminal("m", "SERVER", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);

        assertThatThrownBy(() -> breaker.beforeCall("m", SINK))
                .isInstanceOf(ModelCircuitOpenException.class);
    }

    /** IGNORED 类别（RATE_LIMIT/CONTENT/AUTH/UNKNOWN）不进窗口：再多也不跳闸。 */
    @Test
    void ignoredCategoriesDoNotTrip() {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 60_000), null);

        for (int i = 0; i < 10; i++) {
            breaker.recordTerminal("m", "AUTH", SINK);
            breaker.recordTerminal("m", "RATE_LIMIT", SINK);
            breaker.recordTerminal("m", "CONTENT", SINK);
            breaker.recordTerminal("m", "UNKNOWN", SINK);
        }
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
        breaker.beforeCall("m", SINK); // 不抛
    }

    /** 满窗后移出最老样本：阈值 0.9 下早期失败被成功稀释，失败率回落不跳闸。 */
    @Test
    void slidingWindowEvictsOldestSamples() {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(3, 3, 0.9, 60_000), null);

        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordSuccess("m", SINK); // [F,F,S] rate 0.67 < 0.9，不跳
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);

        breaker.recordSuccess("m", SINK); // [F,S,S]（最老 F 移出）rate 0.33
        breaker.recordSuccess("m", SINK); // [S,S,S] rate 0.0
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    // ---- 半开探测 ----

    /** 冷却期满：放行单探测进 HALF_OPEN，探测成功回 CLOSED（窗口重置）。 */
    @Test
    void halfOpenProbeSuccessCloses() throws InterruptedException {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 30), null);
        trip(breaker);

        Thread.sleep(60); // 过冷却
        breaker.beforeCall("m", SINK); // 探测放行
        assertThat(breaker.state("m")).isEqualTo(CircuitState.HALF_OPEN);
        breaker.recordSuccess("m", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    /** 探测失败回 OPEN（冷却重计）。 */
    @Test
    void halfOpenProbeFailureReopens() throws InterruptedException {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 30), null);
        trip(breaker);

        Thread.sleep(60);
        breaker.beforeCall("m", SINK); // 探测放行 → HALF_OPEN
        breaker.recordTerminal("m", "TIMEOUT", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
        assertThatThrownBy(() -> breaker.beforeCall("m", SINK))
                .isInstanceOf(ModelCircuitOpenException.class);
    }

    /** 探测非可用性失败（AUTH）不阻断恢复：IGNORED 结果也关闸。 */
    @Test
    void halfOpenProbeIgnoredOutcomeCloses() throws InterruptedException {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 30), null);
        trip(breaker);

        Thread.sleep(60);
        breaker.beforeCall("m", SINK);
        breaker.recordTerminal("m", "AUTH", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    /** 跳闸前在飞调用的迟到结果不污染新世代窗口（OPEN 期样本丢弃）。 */
    @Test
    void lateSampleAfterTripIsDropped() {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 60_000), null);
        trip(breaker);
        breaker.recordSuccess("m", SINK); // 迟到成功
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN); // 仍 OPEN，未被「成功」翻盘
    }

    /** 按模型分桶：A 跳闸不影响 B。 */
    @Test
    void bucketsArePerModel() {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 60_000), null);
        breaker.recordTerminal("model-a", "NETWORK", SINK);
        breaker.recordTerminal("model-a", "NETWORK", SINK);
        assertThat(breaker.state("model-a")).isEqualTo(CircuitState.OPEN);
        assertThat(breaker.state("model-b")).isEqualTo(CircuitState.CLOSED);
        breaker.beforeCall("model-b", SINK); // 不抛
    }

    /** 运维计数与 states 详情入账。 */
    @Test
    void statsCountersAndStatesRecorded() {
        ResilienceStats stats = new ResilienceStats();
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 60_000), stats);

        trip(breaker);
        assertThatThrownBy(() -> breaker.beforeCall("m", SINK))
                .isInstanceOf(ModelCircuitOpenException.class);

        assertThat(stats.details()).containsEntry("circuitStates", java.util.Map.of("m", "OPEN"));
        assertThat(stats.details().get("circuitRejections")).isEqualTo(1L);
        assertThat(stats.details().get("circuitTrips")).isEqualTo(1L);
        assertThat(stats.status()).isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP);
    }

    /** 配置 fail-fast：min-calls 超 window-size 启动即失败。 */
    @Test
    void configFailFastOnMinCallsExceedingWindow() {
        assertThatThrownBy(() -> circuit(4, 5, 0.5, 30_000))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class);
        assertThatThrownBy(() -> new ResilienceProperties.Circuit(null, 4, 2, 1.5, Duration.ofSeconds(30), null))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class);
        assertThatThrownBy(() -> new ResilienceProperties.Circuit(null, 4, 2, 0.5, Duration.ZERO, null))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class);
    }

    // ---- helpers ----

    private static void trip(ModelCircuitBreaker breaker) {
        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
    }

    /** 占位期并发拒绝（30ms 冷却实例，跳闸后睡过冷却放行首个探测，第二个并发调用被拒）。 */
    @Test
    void halfOpenRejectsSecondCallWhileProbeInFlightShortCooldown() throws InterruptedException {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(circuit(4, 2, 0.5, 30), null);
        trip(breaker);
        Thread.sleep(60);

        breaker.beforeCall("m", SINK); // 首个探测放行，占位
        assertThatThrownBy(() -> breaker.beforeCall("m", SINK)) // 并发调用被拒
                .isInstanceOf(ModelCircuitOpenException.class)
                .satisfies(e -> assertThat(((ModelCircuitOpenException) e).state())
                        .isEqualTo(CircuitState.HALF_OPEN));
        // 探测完成后放行恢复
        breaker.recordSuccess("m", SINK);
        breaker.beforeCall("m", SINK); // CLOSED，不抛
    }

    /** 失败类别可配置：把 AUTH 计入失败后 AUTH 也跳闸。 */
    @Test
    void failureCategoriesConfigurable() {
        ResilienceProperties.Circuit cfg = new ResilienceProperties.Circuit(
                null, 4, 2, 0.5, Duration.ofSeconds(60), List.of("AUTH"));
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(cfg, null);

        breaker.recordTerminal("m", "AUTH", SINK);
        breaker.recordTerminal("m", "AUTH", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
    }
}
