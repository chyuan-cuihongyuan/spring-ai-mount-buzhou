package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 524 / T801：MCP 建连指数退避重试——失败后按 base×2^n 重排、重试耗尽
 * 收口失败语义、yml 装配、策略校验 fail-fast。
 */
class ConnectRetryTest {

    /** 前N次失败随后成功的工厂。 */
    static final class FlakyFactory implements McpConnectionFactory {
        final AtomicInteger failures = new AtomicInteger();
        final AtomicInteger connects = new AtomicInteger();

        @Override
        public McpConnection connect(ToolSetSpec spec) {
            connects.incrementAndGet();
            if (failures.getAndDecrement() > 0) {
                throw new IllegalStateException("connect refused");
            }
            return new McpConnection() {
                @Override
                public List<org.springframework.ai.tool.ToolCallback> toolCallbacks() {
                    return List.of();
                }

                @Override
                public void close() {
                }
            };
        }
    }

    private DefaultMcpClientRegistry registry;

    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.shutdown();
        }
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP, "http://localhost/" + name,
                Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30), Set.of());
    }

    @Test
    void retryEventuallySucceedsWithBackoff() throws Exception {
        FlakyFactory factory = new FlakyFactory();
        factory.failures.set(2); // 首建 + 2 次重试失败后第 3 次成功
        registry = new DefaultMcpClientRegistry(factory, Duration.ofMillis(100),
                Duration.ofSeconds(2), new RecordingSpanRecorder(), null, List.of(),
                null, new DefaultMcpClientRegistry.ConnectRetryPolicy(4, 20));

        // 首建失败（尝试 1）——策略排 3 次重试（20/40ms 退避）直至成功
        registry.refresh(List.of(spec("retry-srv")));
        long deadline = System.currentTimeMillis() + 5_000;
        while (factory.connects.get() < 3) {
            assertThat(System.currentTimeMillis()).isLessThan(deadline);
            Thread.sleep(20);
        }
        // 退避重试至成功：条目最终 ACTIVE（两次失败 + 一次成功 = ≥3 次建连）
        assertThat(factory.connects.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void retriesExhaustedFallBackToFailureSemantics() throws Exception {
        FlakyFactory factory = new FlakyFactory();
        factory.failures.set(99); // 永远失败
        registry = new DefaultMcpClientRegistry(factory, Duration.ofMillis(100),
                Duration.ofSeconds(2), new RecordingSpanRecorder(), null, List.of(),
                null, new DefaultMcpClientRegistry.ConnectRetryPolicy(2, 20));
        registry.refresh(List.of(spec("dead-srv")));

        Thread.sleep(300); // 重试排空
        assertThat(factory.connects.get()).isGreaterThanOrEqualTo(2);
        assertThat(registry.toolCallbacksFor("a", "a")).isEmpty(); // 未注册——既有失败语义
    }

    @Test
    void policyValidationFailFast() {
        assertThatThrownBy(() -> new DefaultMcpClientRegistry.ConnectRetryPolicy(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultMcpClientRegistry.ConnectRetryPolicy(3, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ymlAssemblyParsesConnectRetry() {
        McpModule module = McpModule.fromYml(Map.of(
                "connect-retry", Map.of("max-attempts", 5, "base-delay-ms", 250))).build();
        assertThat(module.connectRetryPolicy()).isNotNull();
        assertThat(module.connectRetryPolicy().maxAttempts()).isEqualTo(5);
        assertThat(module.connectRetryPolicy().baseDelayMillis()).isEqualTo(250);
        assertThat(McpModule.fromYml(Map.of()).build().connectRetryPolicy()).isNull();
    }
}
