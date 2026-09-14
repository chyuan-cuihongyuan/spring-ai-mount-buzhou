package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话检疫 hook 行为测试（spec 1621 / T2393–T2394 / impl 1174）：连败达阈值后
 * beforeTurn block（可读理由含剩余冷却）、隔离期外放行、onModelError 计败
 * （spec 143 孤类接线的装配面语义钉住）。
 */
class SessionQuarantineHookTest {

    static final class MutableClock extends Clock {
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
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private static TurnContext turnOf(String sessionId) {
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
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
                return null;
            }

            @Override
            public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) {
            }

            @Override
            public String input() {
                return "";
            }

            @Override
            public String response() {
                return "";
            }

            @Override
            public void replaceInput(String newInput) {
            }

            @Override
            public void replaceResponse(String newResponse) {
            }
        };
    }

    @Test
    void consecutiveFailuresQuarantineBlocksBeforeTurn() {
        MutableClock clock = new MutableClock();
        SessionQuarantineHook hook = new SessionQuarantineHook(new SessionQuarantine(
                new SessionQuarantine.Config(3, Duration.ofSeconds(30), Duration.ofMinutes(10)),
                clock));

        // 三连败（onModelError 计败——直接调检疫器喂入；hook 面的 onModelError 需完整 ctx）
        for (int i = 0; i < 3; i++) {
            hook.quarantine().recordTurnFailure("s1");
        }
        assertThat(hook.quarantine().snapshot().get("s1").remainingMillis()).isPositive();

        // 隔离期 beforeTurn block——可读理由
        HookResult blocked = hook.beforeTurn(turnOf("s1"));
        assertThat(blocked).isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Block.class);

        // 冷却过后自动解除（到时放行）
        clock.advance(Duration.ofSeconds(31));
        assertThat(hook.beforeTurn(turnOf("s1"))).isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Continue.class);
    }

    @Test
    void healthySessionNeverQuarantined() {
        SessionQuarantineHook hook = new SessionQuarantineHook(null); // 默认配置
        assertThat(hook.beforeTurn(turnOf("s2"))).isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Continue.class);
        assertThat(hook.quarantine().snapshot().containsKey("s2")).isFalse(); // 未见过 = 零状态（诚实口径）
    }
}
