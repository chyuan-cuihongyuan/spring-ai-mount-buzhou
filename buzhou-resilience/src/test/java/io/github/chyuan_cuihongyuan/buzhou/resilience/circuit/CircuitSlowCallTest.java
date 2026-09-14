package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 熔断慢调用率测试（spec 1628 / T2407–T2408 / impl 1181）：未注入维度零行为；
 * 注入后慢样本（duration ≥ 阈值）窗口占比达阈即开闸（零失败前提）；
 * 快调用不触发；参数校验。resilience4j slow call rate 思想。
 */
class CircuitSlowCallTest {

    private static final Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> SINK = e -> { };

    private static ModelCircuitBreaker breaker() {
        return new ModelCircuitBreaker(new io.github.chyuan_cuihongyuan.buzhou.resilience.config
                .ResilienceProperties.Circuit(null, 20, 5, 0.5, Duration.ofSeconds(1), null),
                null, java.time.Clock.systemUTC());
    }

    @Test
    void withoutSlowPolicyZeroBehavior() {
        ModelCircuitBreaker breaker = breaker();
        for (int i = 0; i < 10; i++) {
            breaker.recordSuccess("m", SINK, Duration.ofSeconds(30)); // 全慢但维度未注入
        }
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    @Test
    void slowCallRateTripsOpenWithoutFailures() {
        ModelCircuitBreaker breaker = breaker()
                .withSlowCallPolicy(Duration.ofMillis(500), 0.8); // 慢阈值 500ms、慢率 80%
        for (int i = 0; i < 4; i++) {
            breaker.recordSuccess("m", SINK, Duration.ofSeconds(2)); // 4 慢
        }
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED); // 4 < minCalls 5
        breaker.recordSuccess("m", SINK, Duration.ofSeconds(2)); // 5/5 慢 → 慢率 1.0 ≥ 0.8 开闸
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
    }

    @Test
    void fastCallsDoNotTrip() {
        ModelCircuitBreaker breaker = breaker()
                .withSlowCallPolicy(Duration.ofMillis(500), 0.8);
        for (int i = 0; i < 10; i++) {
            breaker.recordSuccess("m", SINK, Duration.ofMillis(50)); // 全快
        }
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    @Test
    void durationlessSuccessCountsAsNonSlow() {
        ModelCircuitBreaker breaker = breaker()
                .withSlowCallPolicy(Duration.ofMillis(500), 0.8);
        for (int i = 0; i < 10; i++) {
            breaker.recordSuccess("m", SINK); // 无时长面（既有语义）——不计慢
        }
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);
    }

    @Test
    void invalidRateFailsFast() {
        assertThatThrownBy(() -> breaker().withSlowCallPolicy(Duration.ofMillis(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
