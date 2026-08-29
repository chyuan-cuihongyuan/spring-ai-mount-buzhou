package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 167 / T532：同输入泛洪防护回归——阈值内放行 / 超阈值 block /
 * 窗口滑出复位 / 异文零影响 / 空白等价 / LRU 有界。
 */
class InputFloodGuardHookTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.now();

        void advanceSeconds(long s) {
            now = now.plusSeconds(s);
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

    /** 快测配置：窗口内最多 2 次相同输入 / 窗 10s。 */
    private static InputFloodGuardHook.Config fast() {
        return new InputFloodGuardHook.Config(2, Duration.ofSeconds(10));
    }

    @Test
    void repeatsWithinThresholdPassAndExcessBlocks() {
        MutableClock clock = new MutableClock();
        InputFloodGuardHook hook = new InputFloodGuardHook(fast(), clock);
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "同一句话"))).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "同一句话"))).isEqualTo(HookResult.CONTINUE);

        HookResult verdict = hook.beforeTurn(new DefaultTurnContext(env, "同一句话"));
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) verdict).reason()).contains("疑似循环").contains("3 次");
    }

    @Test
    void windowSlideResetsCounting() {
        MutableClock clock = new MutableClock();
        InputFloodGuardHook hook = new InputFloodGuardHook(fast(), clock);
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        hook.beforeTurn(new DefaultTurnContext(env, "q"));
        hook.beforeTurn(new DefaultTurnContext(env, "q"));
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "q")))
                .isInstanceOf(HookResult.Block.class);

        clock.advanceSeconds(11); // 窗口滑出
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "q")))
                .isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void differentAndWhitespaceVariantInputs() {
        InputFloodGuardHook hook = new InputFloodGuardHook(fast(), new MutableClock());
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        hook.beforeTurn(new DefaultTurnContext(env, "问题 A"));
        hook.beforeTurn(new DefaultTurnContext(env, "问题 B")); // 不同输入零影响
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "问题 A2")))
                .isEqualTo(HookResult.CONTINUE);

        // 空白等价：strip 后同键（第 2 次——仍未超）
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "  问题 A  ")))
                .isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void sessionsAreIsolated() {
        InputFloodGuardHook hook = new InputFloodGuardHook(fast(), new MutableClock());
        HookEnvironment a = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        HookEnvironment b = new HookEnvironment("s2", "agent", new InMemorySessionStateStore());

        hook.beforeTurn(new DefaultTurnContext(a, "同句"));
        hook.beforeTurn(new DefaultTurnContext(a, "同句"));
        assertThat(hook.beforeTurn(new DefaultTurnContext(a, "同句")))
                .isInstanceOf(HookResult.Block.class);
        assertThat(hook.beforeTurn(new DefaultTurnContext(b, "同句"))) // 别的会话不连坐
                .isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void lruBoundsSessionTable() {
        InputFloodGuardHook hook = new InputFloodGuardHook(fast(), new MutableClock());
        for (int i = 0; i < 1026; i++) {
            HookEnvironment env = new HookEnvironment("s" + i, "agent",
                    new InMemorySessionStateStore());
            hook.beforeTurn(new DefaultTurnContext(env, "q"));
        }
        // s0 最久未活跃被逐出——再发按首见计
        HookEnvironment first = new HookEnvironment("s0", "agent", new InMemorySessionStateStore());
        assertThat(hook.beforeTurn(new DefaultTurnContext(first, "q")))
                .isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void configValidatedAndNullContextSafe() {
        assertThatThrownBy(() -> new InputFloodGuardHook.Config(0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InputFloodGuardHook.Config(2, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new InputFloodGuardHook().beforeTurn(null))
                .isEqualTo(HookResult.CONTINUE);
    }
}
