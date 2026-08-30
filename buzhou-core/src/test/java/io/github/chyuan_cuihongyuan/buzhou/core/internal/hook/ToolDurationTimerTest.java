package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 108 §B / T396：工具调用时长 timer 红队——ok/failed 双 outcome 各计时；
 * 时长非负；既有 counter 不变。Prometheus timer 语义（P95 慢工具可告警）。
 */
class ToolDurationTimerTest {

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> timers = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> counters = new ConcurrentLinkedQueue<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            counters.add(name + ":" + String.join("=", tagKeyValue));
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            timers.add(name + ":" + String.join("=", tagKeyValue)
                    + ":" + duration.toNanos());
        }
    }

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    @Test
    void toolDurationRecordedWithOutcomeTags() throws Exception {
        CapturingMetrics metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);

        HookChainTestHarness.toolCall("echo-ok", Map.of(), null).run();
        HookChainTestHarness.toolCall("boom", Map.of(), new IllegalStateException("炸了")).run();

        assertThat(metrics.timers).hasSize(2);
        assertThat(metrics.timers).anySatisfy(t -> assertThat(t)
                .startsWith("buzhou.tool.duration:outcome=ok:"));
        assertThat(metrics.timers).anySatisfy(t -> assertThat(t)
                .startsWith("buzhou.tool.duration:outcome=failed:"));
        // 时长非负（纳秒值 ≥ 0）
        metrics.timers.forEach(t -> {
            long nanos = Long.parseLong(t.substring(t.lastIndexOf(':') + 1));
            assertThat(nanos).isGreaterThanOrEqualTo(0);
        });
        // 既有 counter 面不变（ok/failed 各一）
        assertThat(metrics.counters).containsExactlyInAnyOrder(
                "buzhou.tool.calls:outcome=ok", "buzhou.tool.calls:outcome=failed");
    }

    private static final class HookChainTestHarness {
        static Runnable toolCall(String toolName, Map<String, Object> args, RuntimeException failure) {
            return () -> {
                try {
                    io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores =
                            io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
                    HookEnvironment env = new HookEnvironment("s1", "agent",
                            stores.sessionStateStore());
                    org.springframework.ai.tool.ToolCallback delegate =
                            new org.springframework.ai.tool.ToolCallback() {
                                @Override
                                public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                                    return org.springframework.ai.tool.definition.ToolDefinition
                                            .builder().name(toolName)
                                            .description("timer-test")
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
                    new HookedToolCallback(delegate,
                            io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain.of(List.of()),
                            env).call(new com.fasterxml.jackson.databind.ObjectMapper()
                                    .writeValueAsString(args), null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }
}
