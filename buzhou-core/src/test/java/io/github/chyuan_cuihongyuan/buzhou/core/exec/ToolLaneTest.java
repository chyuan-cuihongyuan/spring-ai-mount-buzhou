package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 173 / T542：泳道回归——并发封顶（观测最大并发）/ 异常归还 / 独立泳道 /
 * 超时抛错 / 透传校验。
 */
class ToolLaneTest {

    /** 记录进入/退出观测最大并发的慢工具。 */
    private ToolCallback slowTool(AtomicInteger inFlight, AtomicInteger maxInFlight,
                                  long holdMillis, boolean fail) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("slow").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max);
                try {
                    Thread.sleep(holdMillis);
                    if (fail) {
                        throw new IllegalStateException("boom");
                    }
                    return "done";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                } finally {
                    inFlight.decrementAndGet();
                }
            }
        };
    }

    @Test
    void laneCapsConcurrentExecutions() throws Exception {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                LaneLimitingToolCallback tool = LaneLimitingToolCallback.wrap(
                        slowTool(inFlight, maxInFlight, 80, false),
                        "slow-db", 1, registry, Duration.ofSeconds(5));
                futures.add(pool.submit(() -> tool.call("{}")));
            }
            for (Future<String> f : futures) {
                assertThat(f.get(5, TimeUnit.SECONDS)).isEqualTo("done");
            }
        }
        assertThat(maxInFlight.get()).isEqualTo(1); // 泳道 1 许可——串行
        assertThat(failures.get()).isZero();
        assertThat(registry.laneNames()).containsExactly("slow-db");
    }

    @Test
    void failingExecutionStillReleasesPermit() throws Exception {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();

        LaneLimitingToolCallback failing = LaneLimitingToolCallback.wrap(
                slowTool(inFlight, maxInFlight, 10, true),
                "flaky-lane", 1, registry, Duration.ofSeconds(2));
        assertThatThrownBy(() -> failing.call("{}"))
                .isInstanceOf(IllegalStateException.class);

        // 许可已归还——下一次可正常取得
        LaneLimitingToolCallback healthy = LaneLimitingToolCallback.wrap(
                slowTool(new AtomicInteger(), new AtomicInteger(), 10, false),
                "flaky-lane", 1, registry, Duration.ofMillis(500));
        assertThat(healthy.call("{}")).isEqualTo("done");
    }

    @Test
    void independentLanesDoNotInterfere() throws Exception {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            LaneLimitingToolCallback slow = LaneLimitingToolCallback.wrap(
                    slowTool(new AtomicInteger(), new AtomicInteger(), 150, false),
                    "slow-db", 1, registry, Duration.ofSeconds(5));
            LaneLimitingToolCallback fast = LaneLimitingToolCallback.wrap(
                    slowTool(new AtomicInteger(), new AtomicInteger(), 10, false),
                    "fast-cache", 8, registry, Duration.ofSeconds(5));

            Future<String> slowFuture = pool.submit(() -> slow.call("{}")); // 占住 slow-db 唯一许可
            Thread.sleep(30);
            long start = System.nanoTime();
            assertThat(fast.call("{}")).isEqualTo("done"); // fast 泳道不受慢泳道排队影响
            assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(400);
            assertThat(slowFuture.get(5, TimeUnit.SECONDS)).isEqualTo("done");
        }
    }

    @Test
    void acquireTimeoutThrowsReadableError() throws Exception {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            LaneLimitingToolCallback holder = LaneLimitingToolCallback.wrap(
                    slowTool(new AtomicInteger(), new AtomicInteger(), 400, false),
                    "busy", 1, registry, Duration.ofSeconds(5));
            Future<String> holding = pool.submit(() -> holder.call("{}"));
            Thread.sleep(50); // 让持有者拿到许可

            LaneLimitingToolCallback waiter = LaneLimitingToolCallback.wrap(
                    slowTool(new AtomicInteger(), new AtomicInteger(), 5, false),
                    "busy", 1, registry, Duration.ofMillis(50));
            assertThatThrownBy(() -> waiter.call("{}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("泳道许可等待超时");
            assertThat(holding.get(5, TimeUnit.SECONDS)).isEqualTo("done");
        }
    }

    @Test
    void definitionPassesThroughAndArgumentsValidate() {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        LaneLimitingToolCallback tool = LaneLimitingToolCallback.wrap(
                slowTool(new AtomicInteger(), new AtomicInteger(), 0, false),
                "l", 2, registry, Duration.ofSeconds(1));
        assertThat(tool.getToolDefinition().name()).isEqualTo("slow");

        assertThatThrownBy(() -> LaneLimitingToolCallback.wrap(null, "l", 1,
                registry, Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LaneLimitingToolCallback.wrap(
                slowTool(new AtomicInteger(), new AtomicInteger(), 0, false),
                "l", 1, registry, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.lane(" ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
