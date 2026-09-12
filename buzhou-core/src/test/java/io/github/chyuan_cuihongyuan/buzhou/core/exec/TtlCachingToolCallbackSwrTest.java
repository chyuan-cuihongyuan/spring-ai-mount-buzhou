package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 缓存 stale-while-revalidate 测试（spec 701 / T953–T954 / impl 504）：grace 窗
 * stale 回程零等待、后台刷新落表、刷新失败保旧值、并发单飞、grace 窗外硬过期、
 * 默认 wrap（grace=0）零回归。
 */
class TtlCachingToolCallbackSwrTest {

    private static final Duration MAX_AGE = Duration.ofMillis(100);
    private static final int MAX_ENTRIES = 16;

    /** Mutable clock：测试手动推进。 */
    private static final class MutableClock extends Clock {
        private volatile Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static ToolCallback countingDelegate(AtomicInteger calls, CountDownLatch slowStart,
                                                 CountDownLatch release) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("dir-list")
                        .description("swr-test").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                if (slowStart != null) {
                    slowStart.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return "fresh-" + calls.get();
            }
        };
    }

    @Test
    void staleServedThenBackgroundRefresh() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-13T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        TtlCachingToolCallback swr = TtlCachingToolCallback.wrap(
                countingDelegate(calls, null, null), MAX_AGE, MAX_ENTRIES, clock,
                Duration.ofSeconds(10));

        assertThat(swr.call("{}")).isEqualTo("fresh-1");   // 冷路径
        clock.advance(MAX_AGE.plusMillis(1));              // 过期，仍在 grace 窗内
        long t0 = System.nanoTime();
        assertThat(swr.call("{}")).isEqualTo("fresh-1");   // stale 立即回程
        assertThat(System.nanoTime() - t0).isLessThan(TimeUnit.SECONDS.toNanos(2));
        assertThat(swr.staleServedCount()).isEqualTo(1);

        // 等待虚拟线程后台刷新真执行 delegate 并落表（轮询到命中新值——put 晚于 delegate 返回）
        long deadline = System.currentTimeMillis() + 5000;
        String result = swr.call("{}");
        while (!"fresh-2".equals(result) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
            result = swr.call("{}");
        }
        assertThat(result).isEqualTo("fresh-2"); // 刷新落表后命中新值，不再是 stale
        assertThat(calls.get()).isEqualTo(2); // 恰两次：冷路径 + 后台刷新（轮询调用未触发重执行）
        assertThat(swr.refreshFailureCount()).isZero();
    }

    @Test
    void refreshFailureKeepsStaleValue() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-13T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        ToolCallback flaky = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("flaky")
                        .description("swr-test").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                if (calls.incrementAndGet() > 1) {
                    throw new IllegalStateException("刷新炸了");
                }
                return "good";
            }
        };
        TtlCachingToolCallback swr = TtlCachingToolCallback.wrap(
                flaky, MAX_AGE, MAX_ENTRIES, clock, Duration.ofSeconds(10));

        assertThat(swr.call("{}")).isEqualTo("good");
        clock.advance(MAX_AGE.plusMillis(1));
        assertThat(swr.call("{}")).isEqualTo("good"); // stale 回程 + 后台刷新失败
        long deadline = System.currentTimeMillis() + 5000;
        while (swr.refreshFailureCount() == 0 && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        assertThat(swr.refreshFailureCount()).isEqualTo(1);
        // 刷新失败后 grace 窗内仍回旧值（use_stale error 语义——绝不回 null）
        assertThat(swr.call("{}")).isEqualTo("good");
        assertThat(swr.staleServedCount()).isEqualTo(2);
    }

    @Test
    void beyondGraceHardExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-13T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        TtlCachingToolCallback swr = TtlCachingToolCallback.wrap(
                countingDelegate(calls, null, null), MAX_AGE, MAX_ENTRIES, clock,
                Duration.ofSeconds(10));

        swr.call("{}");
        clock.advance(MAX_AGE.plusSeconds(11)); // grace 窗外
        assertThat(swr.call("{}")).isEqualTo("fresh-2"); // 硬过期同步重执行
        assertThat(swr.stats().misses()).isEqualTo(2); // 冷路径 + 硬过期各一次
        assertThat(swr.staleServedCount()).isZero();
    }

    @Test
    void concurrentStaleHitsTriggerSingleRefresh() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-13T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch slowStart = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TtlCachingToolCallback swr = TtlCachingToolCallback.wrap(
                countingDelegate(calls, slowStart, release), MAX_AGE, MAX_ENTRIES, clock,
                Duration.ofSeconds(10));

        assertThat(swr.call("{}")).isEqualTo("fresh-1");
        clock.advance(MAX_AGE.plusMillis(1));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch done = new CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        assertThat(swr.call("{}")).isEqualTo("fresh-1");
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
        assertThat(swr.staleServedCount()).isEqualTo(threads);
        // 单飞：N 个并发 stale 命中只触发一次后台刷新
        long deadline = System.currentTimeMillis() + 5000;
        while (calls.get() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void defaultWrapAndInvalidGraceGuard() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-13T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        // 默认 wrap（grace=0）：过期即硬过期——现行为
        TtlCachingToolCallback plain = TtlCachingToolCallback.wrap(
                countingDelegate(calls, null, null), MAX_AGE, MAX_ENTRIES, clock);
        plain.call("{}");
        clock.advance(MAX_AGE.plusMillis(1));
        plain.call("{}");
        assertThat(plain.stats().misses()).isEqualTo(2); // 冷路径 + 硬过期各一次
        assertThat(plain.staleServedCount()).isZero();

        assertThatThrownBy(() -> TtlCachingToolCallback.wrap(
                countingDelegate(calls, null, null), MAX_AGE, MAX_ENTRIES, clock,
                Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
