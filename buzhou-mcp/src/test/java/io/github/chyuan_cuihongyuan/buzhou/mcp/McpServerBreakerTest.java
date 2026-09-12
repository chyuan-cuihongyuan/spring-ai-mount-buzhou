package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.breaker.McpServerBreaker;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 504 / T759–T760：MCP 服务器级聚合熔断——满窗跳闸、OPEN 快速失败
 * （delegate 未触）、冷却耗尽半开探测、一败重开、registry 接线。
 */
class McpServerBreakerTest {

    private static final Duration COOLDOWN = Duration.ofMillis(80);

    /** 可控成败的假工具。 */
    static final class FlakyTool implements ToolCallback {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("tool_x").description("x")
                    .inputSchema("{\"type\":\"object\"}").build();
        }

        @Override
        public String call(String toolInput) {
            calls.incrementAndGet();
            if (failures.get() > 0) {
                failures.decrementAndGet();
                throw new IllegalStateException("server boom");
            }
            return "ok";
        }
    }

    private DefaultMcpClientRegistry registry;

    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.shutdown();
        }
    }

    @Test
    void tripsFastFailsThenHalfOpenProbeCloses() {
        McpServerBreaker breaker = new McpServerBreaker(
                new ToolCircuitBreaker.Config(2, 50.0, COOLDOWN, 1));
        FlakyTool tool = new FlakyTool();
        ToolCallback decorated = breaker.decorate("srv", tool);

        // 满窗全败 → OPEN
        tool.failures.set(2);
        assertThatThrownBy(() -> decorated.call("{}")).hasMessageContaining("boom");
        assertThatThrownBy(() -> decorated.call("{}")).hasMessageContaining("boom");
        assertThat(breaker.snapshot().get("srv").state())
                .isEqualTo(ToolCircuitBreaker.State.OPEN);

        // OPEN 快速失败：结构化文案 + delegate 未触
        assertThatThrownBy(() -> decorated.call("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("熔断 OPEN")
                .hasMessageContaining("srv");
        assertThat(tool.calls.get()).isEqualTo(2);

        // 冷却耗尽 → 半开探测成功 → CLOSED
        try {
            Thread.sleep(COOLDOWN.toMillis() + 60);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(decorated.call("{}")).isEqualTo("ok");
        assertThat(breaker.snapshot().get("srv").state())
                .isEqualTo(ToolCircuitBreaker.State.CLOSED);
    }

    @Test
    void halfOpenProbeFailureReopens() {
        McpServerBreaker breaker = new McpServerBreaker(
                new ToolCircuitBreaker.Config(2, 50.0, COOLDOWN, 1));
        FlakyTool tool = new FlakyTool();
        ToolCallback decorated = breaker.decorate("srv", tool);
        tool.failures.set(2);
        assertThatThrownBy(() -> decorated.call("{}")).hasMessageContaining("boom");
        assertThatThrownBy(() -> decorated.call("{}")).hasMessageContaining("boom");
        assertThat(breaker.snapshot().get("srv").state())
                .isEqualTo(ToolCircuitBreaker.State.OPEN);
        try {
            Thread.sleep(COOLDOWN.toMillis() + 60);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        tool.failures.set(1); // 半开探测即败 → 重开
        assertThatThrownBy(() -> decorated.call("{}")).hasMessageContaining("boom");
        assertThat(breaker.snapshot().get("srv").state())
                .isEqualTo(ToolCircuitBreaker.State.OPEN);
    }

    @Test
    void registryWrapsCallbacksWhenBreakerConfigured() {
        FakeMcp.Factory factory = new FakeMcp.Factory();
        McpServerBreaker breaker = new McpServerBreaker(
                new ToolCircuitBreaker.Config(2, 50.0, COOLDOWN, 1));
        registry = new DefaultMcpClientRegistry(factory, Duration.ofMillis(200),
                Duration.ofSeconds(2), new RecordingSpanRecorder(), null, List.of(), breaker);
        registry.refresh(List.of(new ToolSetSpec("a", Transport.STREAMABLE_HTTP,
                "http://localhost/a", Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30),
                Set.of())));

        List<ToolCallback> callbacks = registry.toolCallbacksFor("app", "agent");
        assertThat(callbacks).hasSize(1);
        assertThat(callbacks.get(0).getToolDefinition().name()).isEqualTo("tool_a");

        // 同一 breaker 状态机直接把 server a 打到 OPEN
        FlakyTool flaky = new FlakyTool();
        ToolCallback direct = breaker.decorate("a", flaky);
        flaky.failures.set(2);
        assertThatThrownBy(() -> direct.call("{}")).hasMessageContaining("boom");
        assertThatThrownBy(() -> direct.call("{}")).hasMessageContaining("boom");
        assertThat(breaker.snapshot().get("a").state())
                .isEqualTo(ToolCircuitBreaker.State.OPEN);
        // 注册表回调（同 breaker 同服务器键）快速失败——接线生效证明，未触网络
        assertThatThrownBy(() -> callbacks.get(0).call("{}"))
                .hasMessageContaining("熔断 OPEN");
    }

    @Test
    void ymlAssemblyParsesServerBreakerConfig() {
        ToolCircuitBreaker.Config config = McpModule.fromYml(Map.of(
                "server-breaker", Map.of("enabled", true))).build().serverBreakerConfig();
        assertThat(config).isNotNull();
        // 未声明 → 不装配
        assertThat(McpModule.fromYml(Map.of()).build().serverBreakerConfig()).isNull();
    }
}
