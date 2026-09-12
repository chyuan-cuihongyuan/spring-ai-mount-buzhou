package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 工具泳道排队时延观测测试（spec 722 / T995–T996 / impl 525）：阻塞计量、
 * 超时计量+语义不变、无竞争微耗时、既有零回归。
 */
class LaneLimitingToolCallbackWaitTest {

    private static LaneLimitingToolCallback callback(ToolLaneRegistry registry,
            String name, int permits) {
        return LaneLimitingToolCallback.wrap(
                new NamedTool(), name, permits, registry, Duration.ofMillis(500));
    }

    /** 最小工具桩（ToolCallback 非函数接口——显式桩）。 */
    private static final class NamedTool implements org.springframework.ai.tool.ToolCallback {
        @Override
        public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
            return org.springframework.ai.tool.definition.ToolDefinition.builder()
                    .name("wait-test").description("lane-wait").inputSchema("{}").build();
        }

        @Override
        public String call(String toolInput) {
            return "ok";
        }
    }

    @Test
    void acquireCountedEvenWithoutContention() {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        LaneLimitingToolCallback first = callback(registry, "db", 1);

        assertThat(first.call("{}")).isEqualTo("ok");

        LaneLimitingToolCallback.WaitStats stats = first.waitStats();
        assertThat(stats.waited()).isEqualTo(1);
        assertThat(stats.totalWaitNanos()).isGreaterThanOrEqualTo(0);
        assertThat(stats.timeouts()).isZero();
    }

    @Test
    void contentionAndTimeoutMeasured() throws Exception {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        LaneLimitingToolCallback limited = callback(registry, "db", 1);
        // 占住许可（底层信号量直接 acquire，不释放——制造超时）
        registry.lane("db", 1).acquire();

        long start = System.nanoTime();
        assertThatThrownBy(() -> limited.call("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("泳道满");
        long blocked = System.nanoTime() - start;

        LaneLimitingToolCallback.WaitStats stats = limited.waitStats();
        assertThat(stats.timeouts()).isEqualTo(1);
        assertThat(stats.maxWaitNanos()).isGreaterThanOrEqualTo(blocked - 1_000_000); // 容差 1ms
        assertThat(stats.waited()).isEqualTo(1);
        registry.lane("db", 1).release();
    }

    @Test
    void noContentionStillCounted() {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        LaneLimitingToolCallback fast = callback(registry, "fast", 4);

        assertThat(fast.call("{}")).isEqualTo("ok");
        LaneLimitingToolCallback.WaitStats stats = fast.waitStats();
        assertThat(stats.waited()).isEqualTo(1);
        assertThat(stats.maxWaitNanos()).isLessThan(Duration.ofSeconds(1).toNanos());
        assertThat(stats.timeouts()).isZero();
    }

    @Test
    void timeoutEmitsLaneTimeoutMetric() throws Exception {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        LaneLimitingToolCallback limited = callback(registry, "db", 1);
        registry.lane("db", 1).acquire();

        CapturingMetrics metrics = new CapturingMetrics();
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(metrics);
        try {
            assertThatThrownBy(() -> limited.call("{}")).isInstanceOf(IllegalStateException.class);
        } finally {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
        }
        assertThat(metrics.timers).anyMatch(t -> t.startsWith("buzhou.lane.wait:lane=db"));
        assertThat(metrics.counters).contains("buzhou.lane.timeout:lane=db");
        registry.lane("db", 1).release();
    }

    static final class CapturingMetrics implements io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics {
        final java.util.List<String> counters = new java.util.concurrent.CopyOnWriteArrayList<>();
        final java.util.List<String> timers = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            counters.add(name + (tagKeyValue.length > 0 ? ":" + String.join("=", tagKeyValue) : ""));
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            timers.add(name + (tagKeyValue.length > 0 ? ":" + String.join("=", tagKeyValue) : ""));
        }
    }
}
