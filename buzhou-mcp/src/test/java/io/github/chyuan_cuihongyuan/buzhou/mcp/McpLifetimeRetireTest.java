package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 连接最大寿命测试（spec 1601 / T2353–T2354 / impl 1154）：到寿退役重建走
 * 探活失败同口径（排水+原样重建）、未到寿不动、在飞推迟（归还时退役语义——
 * 不硬切在飞调用）、policy 关闭零行为。HikariCP maxLifetime 思想。
 */
class McpLifetimeRetireTest {

    /** 可变时钟（到寿判定测试基准）。 */
    static final class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-09-15T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
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

    static final class CountingConnection implements McpConnection {
        final String server;
        final AtomicInteger closeCount = new AtomicInteger();

        CountingConnection(String server) {
            this.server = server;
        }

        @Override
        public List<ToolCallback> toolCallbacks() {
            return List.of();
        }

        @Override
        public List<String> listToolNames() {
            return List.of("tool_" + server);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    static final class CountingFactory implements McpConnectionFactory {
        final AtomicInteger connectCount = new AtomicInteger();

        @Override
        public McpConnection connect(ToolSetSpec spec) {
            connectCount.incrementAndGet();
            return new CountingConnection(spec.name());
        }
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP,
                "http://localhost/" + name, Map.of(), null, null, Set.of());
    }

    private final CountingFactory factory = new CountingFactory();
    private final MutableClock clock = new MutableClock();
    private final Duration lifetimeValue = Duration.ofHours(1);
    private final DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(
            factory, Duration.ofMillis(50), Duration.ofMillis(100),
            null, null, List.of(), null, null, null,
            new DefaultMcpClientRegistry.LifetimePolicy(lifetimeValue, clock));

    @AfterEach
    void tearDown() {
        registry.close();
    }

    @Test
    void notExpiredConnectionIsUntouched() {
        registry.refresh(List.of(spec("s1")));
        assertThat(factory.connectCount.get()).isEqualTo(1);
        clock.advance(lifetimeValue.minusSeconds(1));
        registry.retireExpiredOnce();
        assertThat(registry.retiredCount()).isZero();
        assertThat(factory.connectCount.get()).isEqualTo(1);
        assertThat(registry.toolCallbacksFor(null, null)).isEmpty();
    }

    @Test
    void expiredIdleConnectionIsRetiredAndRebuilt() {
        registry.refresh(List.of(spec("s1")));
        clock.advance(lifetimeValue.plusSeconds(1));
        registry.retireExpiredOnce();
        assertThat(registry.retiredCount()).isEqualTo(1);
        assertThat(factory.connectCount.get()).isEqualTo(2);
        // 新条目 createdAt = 推进后的时钟——同样的剩余寿命，不会立刻再退役
        registry.retireExpiredOnce();
        assertThat(registry.retiredCount()).isEqualTo(1);
        clock.advance(lifetimeValue.plusSeconds(1));
        registry.retireExpiredOnce();
        assertThat(registry.retiredCount()).isEqualTo(2);
        assertThat(factory.connectCount.get()).isEqualTo(3);
    }

    @Test
    void inFlightCallDefersRetirementUntilReleased() throws Exception {
        // 阻塞型伪连接：唯一工具 call 期间持有引用计数（在飞语义）
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        McpConnectionFactory blockingFactory = specArgs -> new McpConnection() {
            @Override
            public List<ToolCallback> toolCallbacks() {
                return List.of(new ToolCallback() {
                    @Override
                    public String getName() {
                        return "blocking_tool";
                    }

                    @Override
                    public String call(String toolInput) {
                        entered.countDown();
                        try {
                            release.await(2, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return "ok";
                    }
                });
            }

            @Override
            public List<String> listToolNames() {
                return List.of("blocking_tool");
            }

            @Override
            public void close() {
            }
        };
        MutableClock c2 = new MutableClock();
        DefaultMcpClientRegistry reg = new DefaultMcpClientRegistry(
                blockingFactory, Duration.ofMillis(50), Duration.ofMillis(100),
                null, null, List.of(), null, null, null,
                new DefaultMcpClientRegistry.LifetimePolicy(Duration.ofSeconds(1), c2));
        try {
            reg.refresh(List.of(spec("s1")));
            List<ToolCallback> callbacks = reg.toolCallbacksFor(null, null);
            assertThat(callbacks).hasSize(1);
            Thread worker = new Thread(() -> callbacks.get(0).call("{}"));
            worker.setDaemon(true);
            worker.start();
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            // 到寿 + 在飞 → 只记推迟，不重建（绝不硬切在飞调用）
            c2.advance(Duration.ofSeconds(2));
            reg.retireExpiredOnce();
            assertThat(reg.deferredRetireCount()).isEqualTo(1);
            assertThat(reg.retiredCount()).isZero();
            // 释放后（在飞归零）再扫 → 重建发生
            release.countDown();
            worker.join(2000);
            reg.retireExpiredOnce();
            assertThat(reg.retiredCount()).isEqualTo(1);
        } finally {
            reg.close();
        }
    }

    @Test
    void nullPolicyIsZeroBehavior() {
        DefaultMcpClientRegistry reg = new DefaultMcpClientRegistry(
                factory, Duration.ofMillis(50), Duration.ofMillis(100),
                null, null, List.of(), null, null, null, null);
        try {
            reg.refresh(List.of(spec("s1")));
            reg.retireExpiredOnce();
            assertThat(reg.retiredCount()).isZero();
            assertThat(factory.connectCount.get()).isEqualTo(1);
        } finally {
            reg.close();
        }
    }
}
