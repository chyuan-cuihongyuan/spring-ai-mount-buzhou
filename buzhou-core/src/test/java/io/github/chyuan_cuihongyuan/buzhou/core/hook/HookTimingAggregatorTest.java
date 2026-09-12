package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hook 计时进程级聚合测试（spec 647 / T944–T945 / impl 500）：跨链同 hook
 * 合并、未开启零共享（链内私有口径不变）、健康面 details 与聚合同源。
 */
class HookTimingAggregatorTest {

    private final HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    @AfterEach
    void tearDown() {
        HookTimingAggregator.Holder.reset(); // 测试隔离——恢复「未开启」基线
    }

    @Test
    void enabledAggregatorMergesAcrossChains() {
        HookTimingAggregator.Holder.reset(); // 全新聚合器——不受同 JVM 早先装配/用例计数污染
        HookTimingAggregator.Holder.enable();
        HookTimingAggregator aggregator = HookTimingAggregator.Holder.current();

        HookChain chainA = HookChain.of(List.of(new NoopHook("shared-hook")));
        HookChain chainB = HookChain.of(List.of(new NoopHook("shared-hook")));
        chainA.beforeTurn(new DefaultTurnContext(env, "in"));
        chainA.beforeTurn(new DefaultTurnContext(env, "in"));
        chainB.beforeTurn(new DefaultTurnContext(env, "in"));

        // 进程级合并两条链；链内私有口径不变
        assertThat(aggregator.stats().get("shared-hook").count()).isEqualTo(3);
        assertThat(chainA.stats().get("shared-hook").count()).isEqualTo(2);
        assertThat(chainB.stats().get("shared-hook").count()).isEqualTo(1);
    }

    @Test
    void disabledAggregatorKeepsChainsIsolated() {
        // 显式归零基线：同 JVM 早先的装配测试（buzhouHookTimingHealth bean）
        // enable 过静态 Holder 且 context 关闭不回退——生产单进程无此叠加，测试需自隔离
        HookTimingAggregator.Holder.reset();
        HookChain chainA = HookChain.of(List.of(new NoopHook("iso-hook")));
        HookChain chainB = HookChain.of(List.of(new NoopHook("iso-hook")));
        chainA.beforeTurn(new DefaultTurnContext(env, "in"));
        chainB.beforeTurn(new DefaultTurnContext(env, "in"));
        assertThat(chainA.stats().get("iso-hook").count()).isEqualTo(1);
        assertThat(chainB.stats().get("iso-hook").count()).isEqualTo(1);
        assertThat(HookTimingAggregator.Holder.current()).isNull();
    }

    @Test
    void healthDetailsMirrorAggregator() {
        HookTimingAggregator aggregator = new HookTimingAggregator();
        aggregator.record("h", 2_000_000L); // 2ms
        HookTimingHealth health = new HookTimingHealth(aggregator);
        assertThat(health.mechanism()).isEqualTo("hook-timing");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) health.details().get("h");
        assertThat(row.get("count")).isEqualTo(1L);
        assertThat(row.get("maxMicros")).isEqualTo(2_000L);
        assertThat(row.get("totalMicros")).isEqualTo(2_000L);
    }

    /** 零耗时 hook 桩。 */
    private static final class NoopHook implements BuzhouHook {
        private final String name;

        NoopHook(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HookResult beforeTurn(TurnContext ctx) {
            return HookResult.CONTINUE;
        }
    }
}
