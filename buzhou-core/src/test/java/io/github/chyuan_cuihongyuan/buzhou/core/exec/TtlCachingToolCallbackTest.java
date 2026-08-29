package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 183 / T556：TTL 工具缓存回归——窗内命中引用一致 / 过期重执行 /
 * LRU 逐出 / 失败不缓存 / 异参各缓存 / 计数与校验。
 */
class TtlCachingToolCallbackTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.now();

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static ToolCallback countingTool(String name, AtomicInteger calls) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return "out-" + calls.incrementAndGet();
            }
        };
    }

    @Test
    void withinWindowHitReturnsSameReferenceWithoutReexecution() {
        MutableClock clock = new MutableClock();
        AtomicInteger calls = new AtomicInteger();
        TtlCachingToolCallback tool = TtlCachingToolCallback.wrap(
                countingTool("weather", calls), Duration.ofMinutes(5), 16, clock);

        String first = tool.call("{}");
        String second = tool.call("{}");

        assertThat(calls.get()).isEqualTo(1);
        assertThat(second).isSameAs(first);
        assertThat(tool.stats().hits()).isEqualTo(1);
        assertThat(tool.stats().misses()).isEqualTo(1);
    }

    @Test
    void expiredEntryReexecutesAndRefreshesWindow() {
        MutableClock clock = new MutableClock();
        AtomicInteger calls = new AtomicInteger();
        TtlCachingToolCallback tool = TtlCachingToolCallback.wrap(
                countingTool("rate", calls), Duration.ofSeconds(30), 16, clock);

        String first = tool.call("{}");
        clock.advanceMillis(31_000); // 过期
        String second = tool.call("{}");

        assertThat(calls.get()).isEqualTo(2);
        assertThat(second).isNotEqualTo(first);

        clock.advanceMillis(10_000); // 新窗内
        assertThat(tool.call("{}")).isSameAs(second);
    }

    @Test
    void lruEvictsLeastRecentlyUsed() {
        MutableClock clock = new MutableClock();
        AtomicInteger calls = new AtomicInteger();
        TtlCachingToolCallback tool = TtlCachingToolCallback.wrap(
                countingTool("dir", calls), Duration.ofMinutes(5), 2, clock);

        tool.call("{k:1}");
        tool.call("{k:2}");
        tool.call("{k:3}"); // 容量 2 → k:1（最久未用）逐出

        assertThat(tool.size()).isEqualTo(2);
        assertThat(tool.stats().evictions()).isEqualTo(1);
        tool.call("{k:1}"); // 逐出者重执行
        assertThat(calls.get()).isEqualTo(4);
    }

    @Test
    void failureIsNotCachedAndRetryExecutes() {
        AtomicInteger calls = new AtomicInteger();
        ToolCallback flaky = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("flaky").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                if (calls.incrementAndGet() == 1) {
                    throw new IllegalStateException("transient");
                }
                return "recovered";
            }
        };
        TtlCachingToolCallback tool = TtlCachingToolCallback.wrap(
                flaky, Duration.ofMinutes(5), 16, new MutableClock());

        assertThatThrownBy(() -> tool.call("{}"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(tool.call("{}")).isEqualTo("recovered"); // 失败未缓存
        assertThat(calls.get()).isEqualTo(2);
        assertThat(tool.size()).isEqualTo(1);
    }

    @Test
    void differentArgsCachedSeparately() {
        AtomicInteger calls = new AtomicInteger();
        TtlCachingToolCallback tool = TtlCachingToolCallback.wrap(
                countingTool("q", calls), Duration.ofMinutes(5), 16, new MutableClock());

        tool.call("{x:1}");
        tool.call("{x:2}");
        tool.call("{x:1}");
        tool.call("{x:2}");

        assertThat(calls.get()).isEqualTo(2);
        assertThat(tool.size()).isEqualTo(2);
    }

    @Test
    void argumentsValidated() {
        assertThatThrownBy(() -> TtlCachingToolCallback.wrap(
                countingTool("t", new AtomicInteger()), Duration.ZERO, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TtlCachingToolCallback.wrap(
                countingTool("t", new AtomicInteger()), Duration.ofMinutes(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
