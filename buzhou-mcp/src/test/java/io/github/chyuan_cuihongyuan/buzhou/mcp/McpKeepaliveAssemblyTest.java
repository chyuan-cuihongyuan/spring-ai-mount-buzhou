package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP keepalive yml 装配测试（spec 724 / T999–T1000 / impl 527）：fromYml 键
 * keepalive-interval 声明即启用；缺省关（零变化）。
 */
class McpKeepaliveAssemblyTest {

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP,
                "http://localhost/" + name, Map.of(), null, null, Set.of());
    }

    /** 最小连接桩：listToolNames 成功返回（探活可计数）。 */
    private static final class ProbeConnection implements McpConnection {
        final AtomicInteger probes = new AtomicInteger();

        @Override
        public List<ToolCallback> toolCallbacks() {
            return List.of();
        }

        @Override
        public List<String> listToolNames() {
            probes.incrementAndGet();
            return List.of("t");
        }

        @Override
        public void close() {
        }
    }

    private static ToolSetProvider fixedProvider(List<ToolSetSpec> specs) {
        return new ToolSetProvider() {
            @Override
            public List<ToolSetSpec> currentToolSets() {
                return specs;
            }

            @Override
            public void addChangeListener(java.lang.Runnable onChange) {
            }
        };
    }

    @Test
    void ymlKeyEnablesKeepaliveAndProbeWorks() {
        SessionStateStore store = new InMemorySessionStateStore();
        ProbeConnection conn = new ProbeConnection();

        McpModule module = McpModule.builder()
                .fromYml(Map.of("keepalive-interval", "30s"))
                .factory(spec -> conn)
                .provider(fixedProvider(List.of(spec("a"))))
                .build();
        DefaultMcpClientRegistry registry = (DefaultMcpClientRegistry) module.registry();

        assertThat(registry).isNotNull();
        registry.probeOnce();
        assertThat(registry.probeSuccessCount()).isEqualTo(1);
        assertThat(conn.probes.get()).isEqualTo(2); // 建连基线 + 探活
        registry.shutdown();
    }

    @Test
    void defaultYmlKeepsKeepaliveOff() {
        SessionStateStore store = new InMemorySessionStateStore();
        ProbeConnection conn = new ProbeConnection();

        McpModule module = McpModule.builder()
                .fromYml(Map.of("servers", Map.of()))
                .factory(spec -> conn)
                .provider(fixedProvider(List.of()))
                .build();
        DefaultMcpClientRegistry registry = (DefaultMcpClientRegistry) module.registry();

        registry.probeOnce(); // 手动驱动不炸（调度未启动——零行为）
        assertThat(registry.probeSuccessCount()).isZero();
        assertThat(conn.probes.get()).isZero();
        registry.shutdown();
    }

    @Test
    void builderFluentFaceAlsoWired() {
        SessionStateStore store = new InMemorySessionStateStore();
        ProbeConnection conn = new ProbeConnection();

        McpModule module = McpModule.builder()
                .keepalive(Duration.ofSeconds(30))
                .factory(spec -> conn)
                .provider(fixedProvider(List.of(spec("a"))))
                .build();
        DefaultMcpClientRegistry registry = (DefaultMcpClientRegistry) module.registry();

        registry.probeOnce();
        assertThat(registry.probeSuccessCount()).isEqualTo(1);
        registry.shutdown();
    }
}
