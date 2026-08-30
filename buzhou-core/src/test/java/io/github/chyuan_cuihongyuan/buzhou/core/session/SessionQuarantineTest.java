package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 143 / T496：会话检疫回归——阈值跳闸/隔离抛含剩余/到时解除/指数升级/
 * 成功复位/hook block 文案/onModelError 累计。
 */
class SessionQuarantineTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.now();

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

    /** 快测配置：阈值 3 / base 1s / max 4s。 */
    private static SessionQuarantine.Config fast() {
        return new SessionQuarantine.Config(3, Duration.ofSeconds(1), Duration.ofSeconds(4));
    }

    @Test
    void thresholdTripsAndAdmissionThrowsWithRemaining() {
        MutableClock clock = new MutableClock();
        SessionQuarantine quarantine = new SessionQuarantine(fast(), clock);

        quarantine.recordTurnFailure("s1");
        quarantine.recordTurnFailure("s1");
        assertThatCode(() -> quarantine.admitOrThrow("s1")).doesNotThrowAnyException();

        quarantine.recordTurnFailure("s1"); // 第 3 次 → 跳闸
        assertThatThrownBy(() -> quarantine.admitOrThrow("s1"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("隔离检疫")
                .hasMessageContaining("剩余冷却")
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.SESSION_QUARANTINED));
    }

    @Test
    void expiresAutomaticallyAndEscalates() {
        MutableClock clock = new MutableClock();
        SessionQuarantine quarantine = new SessionQuarantine(fast(), clock);

        for (int i = 0; i < 3; i++) {
            quarantine.recordTurnFailure("s1");
        }
        clock.advanceMillis(1_100); // 第 1 跳冷却 base=1s 过
        assertThatCode(() -> quarantine.admitOrThrow("s1")).doesNotThrowAnyException();

        // 解除后再败 1 次即再跳（连败计数保留）且冷却升级 2s
        quarantine.recordTurnFailure("s1");
        clock.advanceMillis(1_100);
        assertThatThrownBy(() -> quarantine.admitOrThrow("s1")) // 仍在 2s 冷却内
                .isInstanceOf(BuzhouException.class);
        clock.advanceMillis(1_100); // 累计 2.2s 过
        assertThatCode(() -> quarantine.admitOrThrow("s1")).doesNotThrowAnyException();
    }

    @Test
    void successResetsConsecutiveCount() {
        MutableClock clock = new MutableClock();
        SessionQuarantine quarantine = new SessionQuarantine(fast(), clock);

        quarantine.recordTurnFailure("s1");
        quarantine.recordTurnFailure("s1");
        quarantine.recordTurnSuccess("s1"); // 复位
        quarantine.recordTurnFailure("s1"); // 只算第 1 次
        assertThatCode(() -> quarantine.admitOrThrow("s1")).doesNotThrowAnyException();
    }

    @Test
    void snapshotShowsQuarantineState() {
        MutableClock clock = new MutableClock();
        SessionQuarantine quarantine = new SessionQuarantine(fast(), clock);
        for (int i = 0; i < 3; i++) {
            quarantine.recordTurnFailure("s1");
        }
        SessionQuarantine.View view = quarantine.snapshot().get("s1");
        assertThat(view).isNotNull();
        assertThat(view.trips()).isEqualTo(1);
        assertThat(view.consecutiveFailures()).isEqualTo(3);
        assertThat(view.remainingMillis()).isBetween(900L, 1_000L);
    }

    @Test
    void hookBlocksDuringQuarantineAndCountsModelErrors() {
        MutableClock clock = new MutableClock();
        SessionQuarantineHook hook = new SessionQuarantineHook(
                new SessionQuarantine(fast(), clock));
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        // onModelError 三次 → 跳闸
        for (int i = 0; i < 3; i++) {
            hook.onModelError(new FailedModelCall(env));
        }
        HookResult verdict = hook.beforeTurn(new DefaultTurnContext(env, "hi"));
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) verdict).reason()).contains("隔离检疫");

        clock.advanceMillis(1_100); // 到时解除
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "hi")))
                .isEqualTo(HookResult.CONTINUE);
    }

    /** 最小失败 ModelCallContext 桩（onModelError 只读 sessionId/error）。 */
    private record FailedModelCall(HookEnvironment env) implements
            io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext {
        @Override
        public String sessionId() {
            return env.sessionId();
        }

        @Override
        public String agentName() {
            return env.agentName();
        }

        @Override
        public int turn() {
            return 1;
        }

        @Override
        public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
            return env.stateHandle();
        }

        @Override
        public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) {
        }

        @Override
        public org.springframework.ai.chat.client.ChatClientRequest request() {
            return null;
        }

        @Override
        public org.springframework.ai.chat.client.ChatClientResponse response() {
            return null;
        }

        @Override
        public void replaceRequest(org.springframework.ai.chat.client.ChatClientRequest newRequest) {
        }

        @Override
        public void replaceResponse(org.springframework.ai.chat.client.ChatClientResponse newResponse) {
        }

        @Override
        public Throwable error() {
            return new IllegalStateException("boom");
        }
    }

    @Test
    void configValidatedFailFast() {
        assertThatThrownBy(() -> new SessionQuarantine.Config(0,
                Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SessionQuarantine.Config(3,
                Duration.ofSeconds(5), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
