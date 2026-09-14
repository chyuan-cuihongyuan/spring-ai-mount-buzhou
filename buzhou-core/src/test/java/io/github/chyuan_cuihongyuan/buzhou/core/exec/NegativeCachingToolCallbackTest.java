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
 * 工具失败负缓存测试（spec 1616 / T2383–T2384 / impl 1169）：失败短 TTL 记忆
 * 窗内复读不再真调、过期放行真调、成功清除、异常路径同缓存。DNS negative
 * caching / NXDOMAIN 短 TTL 思想。
 */
class NegativeCachingToolCallbackTest {

    static final class MutableClock extends Clock {
        private volatile Instant instant = Instant.parse("2026-09-15T00:00:00Z");

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    /** 可切换成败的伪工具（计数真实调用）。 */
    static final class FlakyTool implements ToolCallback {
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean fail = true;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
        }

        @Override
        public String call(String input) {
            calls.incrementAndGet();
            return fail ? "[工具执行失败] 下游 503" : "ok:" + input;
        }
    }

    @Test
    void failureIsCachedAndRepeatsServedWithoutRealCall() {
        MutableClock clock = new MutableClock();
        FlakyTool flaky = new FlakyTool();
        NegativeCachingToolCallback wrapped =
                NegativeCachingToolCallback.wrap(flaky, Duration.ofSeconds(30), 16, clock);

        assertThat(wrapped.call("{}")).contains("503");
        assertThat(wrapped.call("{}")).contains("503");
        assertThat(wrapped.call("{}")).contains("503");
        assertThat(flaky.calls.get()).isEqualTo(1); // TTL 内只真调一次
        assertThat(wrapped.stats().negativeHits()).isEqualTo(2);
        assertThat(wrapped.stats().stored()).isEqualTo(1);

        clock.advance(Duration.ofSeconds(31)); // 负 TTL 过期
        assertThat(wrapped.call("{}")).contains("503");
        assertThat(flaky.calls.get()).isEqualTo(2); // 过期后放行真调
    }

    @Test
    void ttlExpiryIsTheRecoveryWindow() {
        MutableClock clock = new MutableClock();
        FlakyTool flaky = new FlakyTool();
        NegativeCachingToolCallback wrapped =
                NegativeCachingToolCallback.wrap(flaky, Duration.ofSeconds(30), 16, clock);

        assertThat(wrapped.call("{}")).contains("503"); // 失败入缓存
        flaky.fail = false; // 故障恢复（但 TTL 未到——DNS 负缓存语义：窗内仍命中）
        assertThat(wrapped.call("{}")).contains("503");
        assertThat(flaky.calls.get()).isEqualTo(1);
        clock.advance(Duration.ofSeconds(31)); // TTL 到期 = 恢复窗口
        assertThat(wrapped.call("{}")).isEqualTo("ok:{}"); // 放行真调且成功
        assertThat(wrapped.call("{}")).isEqualTo("ok:{}"); // 成功不入负缓存
        assertThat(flaky.calls.get()).isEqualTo(3); // 失败 1 + 恢复后 2（成功不入负缓存恒真调）
        assertThat(wrapped.size()).isZero();
    }

    @Test
    void thrownExceptionIsAlsoCached() {
        MutableClock clock = new MutableClock();
        AtomicInteger calls = new AtomicInteger();
        ToolCallback throwing = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String input) {
                calls.incrementAndGet();
                throw new IllegalStateException("连接拒绝");
            }
        };
        NegativeCachingToolCallback wrapped =
                NegativeCachingToolCallback.wrap(throwing, Duration.ofSeconds(30), 16, clock);
        assertThatThrownBy(() -> wrapped.call("{}")).hasMessage("连接拒绝");
        // 二次同参：负缓存命中——不再抛、返回缓存错误文本（真调仅一次）
        assertThat(wrapped.call("{}")).contains("连接拒绝");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(wrapped.stats().negativeHits()).isEqualTo(1);
    }

    @Test
    void distinctArgsAreIndependentKeys() {
        MutableClock clock = new MutableClock();
        FlakyTool flaky = new FlakyTool();
        NegativeCachingToolCallback wrapped =
                NegativeCachingToolCallback.wrap(flaky, Duration.ofSeconds(30), 16, clock);
        assertThat(wrapped.call("{\"q\":1}")).contains("503");
        assertThat(wrapped.call("{\"q\":2}")).contains("503"); // 不同参数独立真调
        assertThat(flaky.calls.get()).isEqualTo(2);
        assertThat(wrapped.size()).isEqualTo(2);
    }
}
