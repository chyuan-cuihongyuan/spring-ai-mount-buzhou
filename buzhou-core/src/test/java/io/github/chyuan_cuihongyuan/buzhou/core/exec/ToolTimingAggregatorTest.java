package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookedToolCallback;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具执行耗时进程级聚合测试（spec 700 / T951–T952 / impl 503）：per-tool
 * 累计、失败计数、未开启零镜像（既有行为零回归）、健康面 details 与聚合同源、
 * HookedToolCallback 统一执行点镜像。
 */
class ToolTimingAggregatorTest {

    @AfterEach
    void tearDown() {
        ToolTimingAggregator.Holder.reset(); // 测试隔离——恢复「未开启」基线
    }

    @Test
    void recordAccumulatesPerTool() {
        ToolTimingAggregator aggregator = new ToolTimingAggregator();
        aggregator.record("db-query", 1_000L, false);
        aggregator.record("db-query", 3_000L, false);
        aggregator.record("web-fetch", 500L, false);

        Map<String, ToolTimingAggregator.ToolTiming> stats = aggregator.stats();
        assertThat(stats.get("db-query").count()).isEqualTo(2);
        assertThat(stats.get("db-query").totalNanos()).isEqualTo(4_000L);
        assertThat(stats.get("db-query").maxNanos()).isEqualTo(3_000L);
        assertThat(stats.get("db-query").avgNanos()).isEqualTo(2_000L);
        assertThat(stats.get("web-fetch").count()).isEqualTo(1);
        // 不可变快照
        assertThat(stats).isUnmodifiable();
    }

    @Test
    void failedPathCountedSeparately() {
        ToolTimingAggregator aggregator = new ToolTimingAggregator();
        aggregator.record("boom", 100L, true);
        aggregator.record("boom", 200L, true);
        aggregator.record("boom", 50L, false);

        ToolTimingAggregator.ToolTiming t = aggregator.stats().get("boom");
        assertThat(t.count()).isEqualTo(3);
        assertThat(t.failed()).isEqualTo(2);
        assertThat(t.maxNanos()).isEqualTo(200L);
    }

    @Test
    void disabledHolderSkipsMirror() {
        // 显式归零基线：同 JVM 早先的装配测试 enable 过静态 Holder——生产单进程
        // 无此叠加，测试需自隔离（HookTimingAggregatorTest 同款注记）
        ToolTimingAggregator.Holder.reset();
        assertThat(ToolTimingAggregator.Holder.current()).isNull();

        harnessCall("echo-ok", null);
        // 未开启 = 无镜像可查；行为零变化由既有 HookedToolCallback 用例（ToolDurationTimerTest）保障
    }

    @Test
    void windowedMaxExposedForToolSide() {
        ToolTimingAggregator aggregator = new ToolTimingAggregator();
        aggregator.record("t", 5_000L, false);
        assertThat(aggregator.windowedMax()).containsEntry("t", 5_000L); // spec 738：滚动 max 可读
    }

    @Test
    void hookedToolCallbackMirrorsWhenEnabled() throws Exception {
        ToolTimingAggregator.Holder.reset();
        ToolTimingAggregator.Holder.enable();

        harnessCall("slow-tool", null);
        harnessCall("slow-tool", null);
        harnessCall("boom-tool", new IllegalStateException("炸了"));

        ToolTimingAggregator aggregator = ToolTimingAggregator.Holder.current();
        assertThat(aggregator.stats().get("slow-tool").count()).isEqualTo(2);
        assertThat(aggregator.stats().get("slow-tool").failed()).isZero();
        assertThat(aggregator.stats().get("boom-tool").failed()).isEqualTo(1);
        assertThat(aggregator.stats().get("boom-tool").count()).isEqualTo(1);
    }

    @Test
    void healthDetailsBoundedAndHomogeneous() {
        ToolTimingAggregator aggregator = new ToolTimingAggregator();
        for (int i = 0; i < ToolTimingHealth.TOP_LIMIT + 5; i++) {
            aggregator.record("tool-" + i, 1_000L * (i + 1), i % 2 == 0);
        }
        ToolTimingHealth health = new ToolTimingHealth(aggregator);

        assertThat(health.mechanism()).isEqualTo("tool-timing");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        // top 语义：按总耗时降序——最耗时的 tool-24（25μs）在首，截断裁掉的恰是最不耗时行
        Map<String, Object> details = health.details();
        assertThat(details).containsKey("_truncated");
        assertThat(details.containsKey("tool-24")).isTrue();
        assertThat(details.containsKey("tool-5")).isTrue();
        assertThat(details.containsKey("tool-4")).isFalse();
        assertThat(details.containsKey("tool-0")).isFalse();
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) details.get("tool-24");
        assertThat(row.get("count")).isEqualTo(1L);
        assertThat(row.get("totalMicros")).isEqualTo(25L);
        assertThat(row.get("maxMicros")).isEqualTo(25L);
        assertThat(row.get("avgMicros")).isEqualTo(25L);
        assertThat(row.get("failed")).isEqualTo(1L);
    }

    /** 复用 ToolDurationTimerTest 同款最小 harness：经 HookedToolCallback 统一执行点调一次工具。 */
    private static void harnessCall(String toolName, RuntimeException failure) {
        try {
            BuzhouStores stores = Buzhou.inMemoryStores();
            HookEnvironment env = new HookEnvironment("s1", "agent", stores.sessionStateStore());
            ToolCallback delegate = new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder().name(toolName)
                            .description("tool-timing-test")
                            .inputSchema("{}").build();
                }

                @Override
                public String call(String toolInput) {
                    if (failure != null) {
                        throw failure;
                    }
                    return "ok";
                }
            };
            new HookedToolCallback(delegate, HookChain.of(List.of()), env)
                    .call("{}", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
