package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 慢调用维度配置装配测试（spec 1637 / T2425–T2426 / impl 1190）：
 * Circuit 组 slow-call-duration 声明即启用（withSlowCallPolicy 语义经
 * Module 装配面传导——此处钉配置组归一与校验）。
 */
class CircuitSlowCallAssemblyTest {

    private static final Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> SINK = e -> { };

    @Test
    void circuitGroupCarriesSlowCallConfig() {
        ResilienceProperties.Circuit circuit = new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null, null, null,
                Duration.ofMillis(500), 0.8);
        assertThat(circuit.slowCallDuration()).isEqualTo(Duration.ofMillis(500));
        assertThat(circuit.effectiveSlowCallRateThreshold()).isEqualTo(0.8);

        // 缺省（11 参构造）：维度关 + rate 默认 0.5
        ResilienceProperties.Circuit off = new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null);
        assertThat(off.slowCallDuration()).isNull();
        assertThat(off.effectiveSlowCallRateThreshold()).isEqualTo(0.5);
    }

    @Test
    void invalidSlowCallConfigFailsFast() {
        assertThatThrownBy(() -> new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null, null, null,
                Duration.ofMillis(-1), 0.8))
                .hasMessageContaining("circuit.slow-call-duration");
        assertThatThrownBy(() -> new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(30), null, null, null, null, null,
                Duration.ofMillis(500), 1.5))
                .hasMessageContaining("circuit.slow-call-rate-threshold");
    }

    @Test
    void slowPolicyWiredThroughBreakerSemantics() {
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new ResilienceProperties.Circuit(
                null, 20, 5, 0.5, Duration.ofSeconds(1), null), null, java.time.Clock.systemUTC())
                .withSlowCallPolicy(Duration.ofMillis(100), 0.8);
        for (int i = 0; i < 5; i++) {
            breaker.recordSuccess("m", SINK, Duration.ofMillis(300)); // 全慢
        }
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN); // 装配语义端到端
    }
}
