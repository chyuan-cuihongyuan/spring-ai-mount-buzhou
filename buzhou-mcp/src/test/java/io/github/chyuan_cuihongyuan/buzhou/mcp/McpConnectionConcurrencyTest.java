package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MCP 每连接并发上限测试（spec 610 / T870–T871 / impl 463）：limit=1 同连接第二调用
 * 阻塞至第一释放、跨连接互不影响、默认不设真并发、上限校验。
 */
class McpConnectionConcurrencyTest {

    private static final Duration GRACE = Duration.ZERO;
    private static final Duration FORCE = Duration.ofSeconds(5);

    /** 可阻塞的工具回调（latch 控制完成）。 */
    private static ToolCallback gatedTool(String name, CountDownLatch gate, AtomicInteger entered) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                entered.incrementAndGet();
                try {
                    gate.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "interrupted";
                }
                return name + ":ok";
            }
        };
    }

    /** 每 server 一个连接、每连接一个工具。 */
    private static McpConnectionFactory factoryOf(Map<String, ToolCallback> toolsByServer) {
        return new McpConnectionFactory() {
            @Override
            public McpConnection connect(ToolSetSpec spec) {
                return connect(spec, null);
            }

            @Override
            public McpConnection connect(ToolSetSpec spec,
                    java.util.function.Consumer<List<io.modelcontextprotocol.spec.McpSchema.Tool>> l) {
                return new McpConnection() {
                    @Override
                    public List<ToolCallback> toolCallbacks() {
                        return List.of(toolsByServer.get(spec.name()));
                    }

                    @Override
                    public void close() {
                    }
                };
            }
        };
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP, "http://localhost/" + name,
                Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30), Set.of());
    }

    /** limit=1：同连接第二个调用阻塞，直至第一个释放许可。 */
    @Test
    void secondCallBlocksUntilFirstReleases() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger entered = new AtomicInteger();
        ToolCallback tool = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                entered.incrementAndGet();
                firstStarted.countDown();
                try {
                    releaseFirst.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "ok";
            }
        };
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(
                factoryOf(Map.of("srv", tool)), GRACE, FORCE, null);
        registry.setPerConnectionConcurrencyLimit(1);
        registry.refresh(List.of(spec("srv")));
        List<ToolCallback> callbacks = registry.toolCallbacksFor("app", "agent");
        assertThat(callbacks).hasSize(1);
        ToolCallback cb = callbacks.get(0);

        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> cb.call("{}"));
        assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue(); // 第一调用已持许可

        CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> cb.call("{}"));
        Thread.sleep(200); // 给第二调用时间尝试获取许可
        assertThat(entered.get()).isEqualTo(1); // 第二调用仍阻塞在许可上（未进入工具体）

        releaseFirst.countDown();
        assertThat(second.get(3, TimeUnit.SECONDS)).isEqualTo("ok"); // 释放后第二调用进入并完成
        registry.shutdown();
    }

    /** 跨连接互不影响：两 server 各自独立信号量，limit=1 也能并行。 */
    @Test
    void differentConnectionsDoNotInterfere() throws Exception {
        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ToolCallback tool = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                bothEntered.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "ok";
            }
        };
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(
                factoryOf(Map.of("srv-a", tool, "srv-b", tool)), GRACE, FORCE, null);
        registry.setPerConnectionConcurrencyLimit(1);
        registry.refresh(List.of(spec("srv-a"), spec("srv-b")));
        ToolCallback cbA = registry.toolCallbacksFor("app", "agent").get(0);
        ToolCallback cbB = registry.toolCallbacksFor("app", "agent").get(1);

        CompletableFuture.supplyAsync(() -> cbA.call("{}"));
        CompletableFuture.supplyAsync(() -> cbB.call("{}"));
        assertThat(bothEntered.await(2, TimeUnit.SECONDS)).isTrue(); // 两连接并行进入
        release.countDown();
        registry.shutdown();
    }

    /** 默认不设（null）：同连接两调用真并发（零行为变化）。 */
    @Test
    void unlimitedByDefaultAllowsTrueConcurrency() throws Exception {
        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger entered = new AtomicInteger();
        CountDownLatch gate = new CountDownLatch(1);
        ToolCallback tool = gatedToolWithEntry(entered, gate, bothEntered);
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(
                factoryOf(Map.of("srv", tool)), GRACE, FORCE, null);
        registry.refresh(List.of(spec("srv")));
        ToolCallback cb = registry.toolCallbacksFor("app", "agent").get(0);

        CompletableFuture.supplyAsync(() -> cb.call("{}"));
        CompletableFuture.supplyAsync(() -> cb.call("{}"));
        assertThat(bothEntered.await(2, TimeUnit.SECONDS)).isTrue();
        gate.countDown();
        registry.shutdown();
    }

    private static ToolCallback gatedToolWithEntry(AtomicInteger entered, CountDownLatch gate,
            CountDownLatch bothEntered) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("t").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                entered.incrementAndGet();
                bothEntered.countDown();
                try {
                    gate.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "ok";
            }
        };
    }

    /** 上限校验：0/负数拒绝、null 合法。 */
    @Test
    void limitValidation() {
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(
                factoryOf(Map.of()), GRACE, FORCE, null);
        assertThatThrownBy(() -> registry.setPerConnectionConcurrencyLimit(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.setPerConnectionConcurrencyLimit(-1))
                .isInstanceOf(IllegalArgumentException.class);
        registry.setPerConnectionConcurrencyLimit(null);
    }
}
