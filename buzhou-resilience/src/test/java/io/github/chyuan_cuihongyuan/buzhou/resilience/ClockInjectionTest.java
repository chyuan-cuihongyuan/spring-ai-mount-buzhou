package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.CircuitState;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.quota.SessionQuotaHook;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 41 §B / T154 / impl-125：时钟注入面——熔断冷却与配额 UTC 日窗的时间行为经可推进
 * Clock 驱动，测试零真实等待（既往只能 Thread.sleep 真等冷却/翻日）。
 */
class ClockInjectionTest {

    private static final Consumer<SessionEvent> SINK = e -> {
    };

    /** 可推进时钟（测试替身）：instant 随 advance 前移。 */
    static final class MutableClock extends Clock {
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
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Test
    void circuitCooldownAdvancesByClockWithoutRealWaiting() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-16T00:00:00Z"));
        // 冷却 60s、min-calls=3：三失败样本跳闸 OPEN
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new ResilienceProperties.Circuit(
                null, 10, 3, 0.5, Duration.ofSeconds(60), null), null, clock);

        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);
        assertThatThrownBy(() -> breaker.beforeCall("m", SINK))
                .isInstanceOf(ModelCircuitOpenException.class);

        // 时钟推进 61s（零真实等待）：冷却完毕 → 半开探测放行
        clock.advance(Duration.ofSeconds(61));
        breaker.beforeCall("m", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.HALF_OPEN);
    }

    @Test
    void quotaDayWindowRollsByClockWithoutRealWaiting() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-16T12:00:00Z"));
        ResilienceProperties.SessionQuota quota =
                new ResilienceProperties.SessionQuota(1, null, null); // turnsPerDay=1
        SessionQuotaHook hook = new SessionQuotaHook(quota, null, clock);

        // 第一轮放行、第二轮拦截（同 UTC 日窗口内，计数落共享会话 state）
        Map<String, Object> state = new ConcurrentHashMap<>();
        assertThat(hook.beforeTurn(turnCtx("clock-quota", state)))
                .isEqualTo(HookResult.CONTINUE);
        assertThat(hook.beforeTurn(turnCtx("clock-quota", state)))
                .isInstanceOf(HookResult.Block.class);

        // 时钟推进到下一 UTC 日（零真实等待）：窗口读时重置、新一轮放行
        clock.advance(Duration.ofHours(13));
        assertThat(hook.beforeTurn(turnCtx("clock-quota", state)))
                .isEqualTo(HookResult.CONTINUE);
    }

    /** 共享 state 的 TurnContext 替身（配额计数落会话 state）。 */
    private static TurnContext turnCtx(String sessionId, Map<String, Object> state) {
        return new TurnContext() {
            @Override
            public String sessionId() {
                return sessionId;
            }

            @Override
            public String agentName() {
                return "agent";
            }

            @Override
            public int turn() {
                return 1;
            }

            @Override
            public SessionStateHandle state() {
                return new SessionStateHandle() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> Optional<T> get(String key, Class<T> type) {
                        return Optional.ofNullable((T) state.get(key));
                    }

                    @Override
                    public void put(String key, Object value) {
                        state.put(key, value);
                    }

                    @Override
                    public void delete(String key) {
                        state.remove(key);
                    }
                };
            }

            @Override
            public void emitEvent(SessionEvent event) {
            }

            @Override
            public void replaceResponse(String response) {
            }

            @Override
            public void replaceInput(String input) {
            }

            @Override
            public String input() {
                return "turn";
            }

            @Override
            public String response() {
                return null;
            }
        };
    }
}
