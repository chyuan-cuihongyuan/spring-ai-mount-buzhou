package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hook 链 per-hook 耗时观测测试（spec 646 / T942–T943 / impl 499）：count/
 * total/max 累计正确、慢快 hook 互不串账、多回调面同 hook 归并同键、stats()
 * 不可变、block 路径计时不改返回语义。
 */
class HookChainTimingTest {

    private final HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    /** 可控耗时 hook（beforeTurn 睡指定毫秒）。 */
    private static class SleepingHook implements BuzhouHook {
        private final String name;
        private final long sleepMillis;

        SleepingHook(String name, long sleepMillis) {
            this.name = name;
            this.sleepMillis = sleepMillis;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HookResult beforeTurn(TurnContext ctx) {
            try {
                TimeUnit.MILLISECONDS.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return HookResult.CONTINUE;
        }
    }

    @Test
    void slowAndFastHooksAccumulateIndependently() {
        SleepingHook slow = new SleepingHook("slow-hook", 30);
        SleepingHook fast = new SleepingHook("fast-hook", 0);
        HookChain chain = new HookChain(List.of(slow, fast), Set.of());

        chain.beforeTurn(new DefaultTurnContext(env, "in"));
        chain.beforeTurn(new DefaultTurnContext(env, "in"));

        Map<String, HookChain.HookTiming> stats = chain.stats();
        HookChain.HookTiming slowTiming = stats.get("slow-hook");
        assertThat(slowTiming.count()).isEqualTo(2);
        // total >= 2×30ms、max >= 30ms（sleep 下界；调度抖动只多不少）
        assertThat(slowTiming.totalNanos()).isGreaterThanOrEqualTo(2L * 30_000_000);
        assertThat(slowTiming.maxNanos()).isGreaterThanOrEqualTo(30_000_000L);
        assertThat(slowTiming.avgNanos()).isGreaterThanOrEqualTo(30_000_000.0);

        HookChain.HookTiming fastTiming = stats.get("fast-hook");
        assertThat(fastTiming.count()).isEqualTo(2);
        assertThat(fastTiming.totalNanos()).isLessThan(30_000_000L); // 快 hook 累计也远小于单次慢
        assertThat(fastTiming.avgNanos()).isLessThan(30_000_000.0);
    }

    /** 多回调面同 hook 归并同键（count 面数合计——hook 名为键非「名+面」）。 */
    @Test
    void multipleCallbacksMergeUnderHookName() {
        SleepingHook hook = new SleepingHook("h", 0) {
            @Override
            public HookResult afterTurn(TurnContext ctx) {
                return HookResult.CONTINUE;
            }
        };
        HookChain chain = new HookChain(List.of(hook), Set.of());
        chain.beforeTurn(new DefaultTurnContext(env, "in"));
        chain.afterTurn(new DefaultTurnContext(env, "in"));
        assertThat(chain.stats().get("h").count()).isEqualTo(2);
    }

    /** block 路径计时不改返回语义（HookResult.Block 原样返回）。 */
    @Test
    void blockPathStillReturnsBlockAndRecords() {
        BuzhouHook blocker = new SleepingHook("blocker", 0) {
            @Override
            public HookResult beforeTurn(TurnContext ctx) {
                return new HookResult.Block("gate closed");
            }
        };
        HookChain chain = new HookChain(List.of(blocker), Set.of());
        HookResult result = chain.beforeTurn(new DefaultTurnContext(env, "in"));
        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(chain.stats().get("blocker").count()).isEqualTo(1);
    }

    /** stats() 快照不可变（再跑一轮不改已取快照）。 */
    @Test
    void statsSnapshotIsImmutable() {
        SleepingHook hook = new SleepingHook("h", 0);
        HookChain chain = new HookChain(List.of(hook), Set.of());
        chain.beforeTurn(new DefaultTurnContext(env, "in"));
        Map<String, HookChain.HookTiming> snapshot = chain.stats();
        long countAtSnapshot = snapshot.get("h").count();
        chain.beforeTurn(new DefaultTurnContext(env, "in"));
        assertThat(snapshot.get("h").count()).isEqualTo(countAtSnapshot);
        assertThat(chain.stats().get("h").count()).isEqualTo(countAtSnapshot + 1);
    }
}
