package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1424 / T2150：轮次限速 per-key 拒绝榜——被拦 key 计数、放行 key 不入榜、
 * 次数降序同次数字典序、reset 清表不清桶。
 */
class TurnRateLimitBlockedSnapshotTest {

    private final AtomicLong nanos = new AtomicLong();

    private DefaultTurnContext turn(String sessionId) {
        return new DefaultTurnContext(new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment(
                sessionId, "ag", new InMemorySessionStateStore()), "q");
    }

    @Test
    void blockedKeysCountedAndSorted() {
        TurnRateLimitHook hook = new TurnRateLimitHook(
                new TurnRateLimitHook.Policy(1, 60.0), ctx -> ctx.sessionId(), nanos::get);
        // tenant-a 消费唯一令牌后被拦 3 次；tenant-b 放行 1 次
        for (int i = 0; i < 4; i++) {
            hook.beforeTurn(turn("tenant-a"));
        }
        assertThat(hook.beforeTurn(turn("tenant-b"))).isEqualTo(HookResult.CONTINUE);
        var snapshot = hook.blockedSnapshot();
        assertThat(snapshot.get("tenant-a")).isEqualTo(3);
        assertThat(snapshot).doesNotContainKey("tenant-b");
    }

    @Test
    void sortedByCountDescThenName() {
        TurnRateLimitHook hook = new TurnRateLimitHook(
                new TurnRateLimitHook.Policy(1, 60.0), ctx -> ctx.sessionId(), nanos::get);
        for (int i = 0; i < 3; i++) {
            hook.beforeTurn(turn("hot"));
        }
        for (int i = 0; i < 2; i++) {
            hook.beforeTurn(turn("warm"));
        }
        for (int i = 0; i < 2; i++) {
            hook.beforeTurn(turn("cool"));
        }
        var keys = hook.blockedSnapshot().keySet().stream().toList();
        // 次数降序：hot=3；warm/cool 同 2 次平名典序
        assertThat(keys).containsExactly("hot", "cool", "warm");
    }

    @Test
    void resetClearsBoardOnly() {
        TurnRateLimitHook hook = new TurnRateLimitHook(
                new TurnRateLimitHook.Policy(1, 60.0), ctx -> ctx.sessionId(), nanos::get);
        hook.beforeTurn(turn("a"));
        hook.beforeTurn(turn("a")); // 第 2 次被拦
        assertThat(hook.blockedSnapshot()).hasSize(1);
        hook.resetBlockedForTest();
        assertThat(hook.blockedSnapshot()).isEmpty();
        // 桶状态不动：时间推进回填（60/分钟 → 10s ≈ 10 个令牌）后照常放行
        nanos.addAndGet(10_000_000_000L);
        assertThat(hook.beforeTurn(turn("a"))).isEqualTo(HookResult.CONTINUE);
    }
}
