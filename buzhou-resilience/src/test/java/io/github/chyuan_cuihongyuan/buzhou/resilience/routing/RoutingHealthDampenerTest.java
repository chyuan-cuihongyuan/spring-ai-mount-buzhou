package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.CircuitState;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.CircuitTransitionJournal;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 703 / T1006–T1007：健康加权路由抑制——OPEN 压权至地板+邻路不变、
 * CLOSED 恢复声明权重、未匹配名忽略、floor 校验、listener 异常隔离。
 */
class RoutingHealthDampenerTest {

    private static final Consumer<SessionEvent> SINK = e -> {
    };

    private static final class StubModel implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.empty();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-12T00:00:00Z");

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static ModelCircuitBreaker breaker(Clock clock) {
        return new ModelCircuitBreaker(new ResilienceProperties.Circuit(
                null, 10, 3, 0.5, Duration.ofMillis(100), null, null, 1), null, clock);
    }

    private static WeightedChatModel router() {
        return new WeightedChatModel(
                Map.of("primary", (ChatModel) new StubModel(), "backup", (ChatModel) new StubModel()),
                Map.of("primary", 5, "backup", 2));
    }

    @Test
    void tripDampensToFloorAndRecoveryRestoresDeclared() {
        MutableClock clock = new MutableClock();
        WeightedChatModel router = router();
        ModelCircuitBreaker breaker = breaker(clock);
        RoutingHealthDampener dampener = RoutingHealthDampener.attach(router, breaker, 1);

        breaker.recordTerminal("primary", "NETWORK", SINK);
        breaker.recordTerminal("primary", "NETWORK", SINK);
        breaker.recordTerminal("primary", "NETWORK", SINK); // → OPEN
        assertThat(router.routes()).containsEntry("primary", 1).containsEntry("backup", 2);
        assertThat(dampener.dampened()).containsEntry("primary", 1);

        clock.advanceMillis(200);
        breaker.beforeCall("primary", SINK); // → HALF_OPEN（维持地板）
        assertThat(router.routes()).containsEntry("primary", 1);

        breaker.recordSuccess("primary", SINK); // 半开阈值 1 → CLOSED 恢复
        assertThat(breaker.state("primary")).isEqualTo(CircuitState.CLOSED);
        assertThat(router.routes()).containsEntry("primary", 5).containsEntry("backup", 2);
        assertThat(dampener.dampened()).isEmpty();
    }

    @Test
    void repeatedTripsAreIdempotentAndUnknownModelsIgnored() {
        MutableClock clock = new MutableClock();
        WeightedChatModel router = router();
        ModelCircuitBreaker breaker = breaker(clock);
        RoutingHealthDampener dampener = RoutingHealthDampener.attach(router, breaker, 1);

        breaker.recordTerminal("primary", "NETWORK", SINK);
        breaker.recordTerminal("primary", "NETWORK", SINK);
        breaker.recordTerminal("primary", "NETWORK", SINK); // → OPEN 压权
        breaker.recordTerminal("primary", "NETWORK", SINK); // OPEN 期样本丢弃——不重复触发变迁

        // 未注册路由的名字：变迁照记但不触碰路由
        breaker.recordTerminal("unrouted-model", "NETWORK", SINK);
        breaker.recordTerminal("unrouted-model", "NETWORK", SINK);
        breaker.recordTerminal("unrouted-model", "NETWORK", SINK);

        assertThat(router.routes()).containsEntry("primary", 1).containsEntry("backup", 2);
        assertThat(dampener.dampened()).containsOnlyKeys("primary");
    }

    @Test
    void listenerExceptionIsIsolatedAndFloorIsvalidated() {
        MutableClock clock = new MutableClock();
        WeightedChatModel router = router();
        ModelCircuitBreaker breaker = breaker(clock);
        breaker.addTransitionListener(t -> {
            throw new IllegalStateException("listener 炸了");
        });
        RoutingHealthDampener dampener = RoutingHealthDampener.attach(router, breaker, 1);

        breaker.recordTerminal("primary", "NETWORK", SINK);
        breaker.recordTerminal("primary", "NETWORK", SINK);
        breaker.recordTerminal("primary", "NETWORK", SINK); // 状态机照常 → OPEN
        assertThat(breaker.state("primary")).isEqualTo(CircuitState.OPEN);
        assertThat(router.routes()).containsEntry("primary", 1); // dampener 照常收到回调

        assertThatThrownBy(() -> RoutingHealthDampener.attach(router, breaker, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoutingHealthDampener.attach(null, breaker, 1))
                .isInstanceOf(NullPointerException.class);
        assertThat(dampener.dampened()).containsEntry("primary", 1);

        // journal 与监听缝同数据（702 回归）
        assertThat(breaker.transitionJournal().snapshot().tripsByModel()).containsEntry("primary", 1L);
        assertThat(breaker.transitionJournal().snapshot().recent().get(0))
                .isInstanceOf(CircuitTransitionJournal.Transition.class);
    }
}
