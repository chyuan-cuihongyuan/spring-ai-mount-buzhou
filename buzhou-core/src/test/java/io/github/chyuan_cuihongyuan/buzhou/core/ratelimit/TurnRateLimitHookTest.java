package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 425 §Testing / T741–T742：轮次限速——burst 扣完 block、满/部分
 * 回填放行、双 key 独立、常量键共享、block 不扣令牌、快照观测、yml
 * 双声明装配/缺席。
 */
class TurnRateLimitHookTest {

    private static final long SECOND = 1_000_000_000L;

    private static DefaultTurnContext turn(String sessionId) {
        return new DefaultTurnContext(new HookEnvironment(sessionId, "ag", new InMemorySessionStateStore()), "q");
    }

    private static boolean allowed(TurnRateLimitHook hook, String sessionId) {
        HookResult result = hook.beforeTurn(turn(sessionId));
        return result == HookResult.CONTINUE;
    }

    @Test
    void shouldBlockAfterBurstAndRefillOverTime() {
        AtomicLong nanos = new AtomicLong(0);
        TurnRateLimitHook hook = new TurnRateLimitHook(
                new TurnRateLimitHook.Policy(2, 1.0), ctx -> "tenant-a", nanos::get);

        assertThat(allowed(hook, "s1")).isTrue(); // 桶满扣 1 → 1
        assertThat(allowed(hook, "s1")).isTrue(); // 扣 1 → 0
        assertThat(allowed(hook, "s1")).isFalse(); // 空 → block（不扣）
        assertThat(hook.availableSnapshot()).containsEntry("tenant-a", 0.0);

        nanos.addAndGet(30 * SECOND); // 30s = 0.5 令牌——部分回填不足 1
        assertThat(allowed(hook, "s1")).isFalse();
        assertThat(hook.availableSnapshot().get("tenant-a")).isEqualTo(0.5);

        nanos.addAndGet(30 * SECOND); // 累计 60s = 1 令牌
        assertThat(allowed(hook, "s1")).isTrue();
        assertThat(hook.availableSnapshot()).containsEntry("tenant-a", 0.0);

        nanos.addAndGet(10 * 60 * SECOND); // 长时间回填——封顶 burst 不溢出
        assertThat(hook.availableSnapshot().get("tenant-a")).isEqualTo(2.0);
    }

    @Test
    void shouldKeepKeysIndependent_andShareConstantKey() {
        AtomicLong nanos = new AtomicLong(0);
        // 默认键=sessionId：两会话各自独立桶
        TurnRateLimitHook perSession = new TurnRateLimitHook(
                new TurnRateLimitHook.Policy(1, 0.001), io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext::sessionId, nanos::get);
        assertThat(allowed(perSession, "s1")).isTrue();
        assertThat(allowed(perSession, "s1")).isFalse(); // s1 打满
        assertThat(allowed(perSession, "s2")).isTrue(); // s2 不受牵连

        // 常量键：跨会话共享桶（租户整体帽）
        TurnRateLimitHook tenantWide = new TurnRateLimitHook(
                new TurnRateLimitHook.Policy(1, 0.001), ctx -> "tenant-all", nanos::get);
        assertThat(allowed(tenantWide, "s1")).isTrue();
        assertThat(allowed(tenantWide, "s2")).isFalse(); // 同桶已空
    }

    @Test
    void shouldBlockWithStructuredReason() {
        AtomicLong nanos = new AtomicLong(0);
        TurnRateLimitHook hook = new TurnRateLimitHook(
                new TurnRateLimitHook.Policy(1, 1.0),
                io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext::sessionId, nanos::get);
        assertThat(allowed(hook, "s1")).isTrue();
        var blocked = hook.beforeTurn(turn("s1"));
        assertThat(blocked).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) blocked).reason())
                .contains("轮次限速触发").contains("key=s1").contains("回填 1.0/分钟");
    }

    @Test
    void shouldAssembleOnlyWhenBothDeclared() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.ratelimit.turns.burst=5",
                        "buzhou.ratelimit.turns.permits-per-minute=10")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouTurnRateLimitRuntimeConfig");
                });
        // 缺 permits-per-minute：不装配
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.ratelimit.turns.burst=5")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouTurnRateLimitRuntimeConfig");
                });
    }
}
