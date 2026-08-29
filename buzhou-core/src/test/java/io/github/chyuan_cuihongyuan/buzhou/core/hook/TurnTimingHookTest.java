package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 191 / T564：轮次计时回归——时长下限 / 滚动窗 / 隔离 / 重入覆盖 /
 * 无起点安全。
 */
class TurnTimingHookTest {

    private HookEnvironment env(String sessionId) {
        return new HookEnvironment(sessionId, "agent", new InMemorySessionStateStore());
    }

    @Test
    void measuresEndToEndDurationWithLowerBound() throws Exception {
        TurnTimingHook hook = new TurnTimingHook();
        HookEnvironment env = env("s1");

        hook.beforeTurn(new DefaultTurnContext(env, "q"));
        Thread.sleep(60);
        hook.afterTurn(new DefaultTurnContext(env, "q"));

        TurnTimingHook.TurnStats stats = hook.stats("s1");
        assertThat(stats.count()).isEqualTo(1);
        assertThat(stats.lastMillis()).isGreaterThanOrEqualTo(55);
        assertThat(stats.maxMillis()).isEqualTo(stats.lastMillis());
    }

    @Test
    void rollingWindowKeepsLastSixtyFour() throws Exception {
        TurnTimingHook hook = new TurnTimingHook();
        HookEnvironment env = env("s1");
        for (int i = 0; i < 70; i++) {
            hook.beforeTurn(new DefaultTurnContext(env, "q"));
            Thread.sleep(1);
            hook.afterTurn(new DefaultTurnContext(env, "q"));
        }

        assertThat(hook.stats("s1").count()).isEqualTo(64);
    }

    @Test
    void sessionsAreIsolated() throws Exception {
        TurnTimingHook hook = new TurnTimingHook();
        HookEnvironment a = env("a");
        HookEnvironment b = env("b");

        hook.beforeTurn(new DefaultTurnContext(a, "q"));
        Thread.sleep(40);
        hook.afterTurn(new DefaultTurnContext(a, "q"));

        assertThat(hook.stats("a").count()).isEqualTo(1);
        assertThat(hook.stats("b").count()).isZero(); // 未计时不连坐
        assertThat(hook.stats("ghost").count()).isZero(); // 未知会话零值行
    }

    @Test
    void reentrantBeforeTurnOverwritesStartPoint() throws Exception {
        TurnTimingHook hook = new TurnTimingHook();
        HookEnvironment env = env("s1");

        hook.beforeTurn(new DefaultTurnContext(env, "q1"));
        Thread.sleep(40);
        hook.beforeTurn(new DefaultTurnContext(env, "q2")); // 异常路径重入
        Thread.sleep(20);
        hook.afterTurn(new DefaultTurnContext(env, "q2"));

        assertThat(hook.stats("s1").lastMillis()).isLessThan(60); // 按新起点计
    }

    @Test
    void afterWithoutBeforeIsSafe() {
        TurnTimingHook hook = new TurnTimingHook();
        HookEnvironment env = env("s1");

        assertThat(hook.afterTurn(new DefaultTurnContext(env, "orphan")))
                .isEqualTo(HookResult.CONTINUE);
        assertThat(hook.stats("s1").count()).isZero();
        assertThat(hook.beforeTurn(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.afterTurn(null)).isEqualTo(HookResult.CONTINUE);
    }
}
