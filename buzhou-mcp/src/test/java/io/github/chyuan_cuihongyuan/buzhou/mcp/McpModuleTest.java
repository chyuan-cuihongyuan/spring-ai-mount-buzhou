package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模块装配测试：fromYml 解析、初始建连、DB 改配推送刷新、优雅关闭。
 */
class McpModuleTest {

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STDIO, "cmd-" + name,
                Map.of(), null, null, Set.of());
    }

    @Test
    void ymlServersDriveInitialConnections() {
        FakeMcp.Factory factory = new FakeMcp.Factory();
        McpModule module = McpModule.builder()
                .fromYml(Map.of(
                        "enabled", true,
                        "grace-period", "1s",
                        "servers", Map.of("a", Map.of(
                                "transport", "STDIO", "endpoint", "cmd-a"))))
                .factory(factory)
                .build();
        try {
            assertThat(module.enabled()).isTrue();
            assertThat(factory.connectCount("a")).isEqualTo(1);
            assertThat(module.registry().toolCallbacksFor("app", "agent"))
                    .extracting(c -> c.getToolDefinition().name())
                    .containsExactly("tool_a");
        } finally {
            module.close();
        }
    }

    @Test
    void dbStoreChangePushesRefresh() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        store.replaceAll(List.of(spec("a")));
        FakeMcp.Factory factory = new FakeMcp.Factory();
        McpModule module = McpModule.builder()
                .store(store)
                .pollInterval(Duration.ofSeconds(30))   // 依赖写通知而非轮询
                .gracePeriod(Duration.ofMillis(100))
                .factory(factory)
                .build();
        try {
            assertThat(factory.connectCount("a")).isEqualTo(1);

            // 后台改配：摘 a 增 b → 注册表差量刷新
            store.replaceAll(List.of(spec("b")));
            FakeMcp.await("b connected", 2000, () -> factory.connectCount("b") == 1);
            FakeMcp.await("a closed", 2000, () -> factory.latest("a").closed());
            assertThat(module.registry().toolCallbacksFor("app", "agent"))
                    .extracting(c -> c.getToolDefinition().name())
                    .containsExactly("tool_b");
        } finally {
            module.close();
        }
    }

    @Test
    void disabledModuleHasNoRegistry() {
        McpModule module = McpModule.builder().enabled(false).build();
        assertThat(module.enabled()).isFalse();
        assertThat(module.registry()).isNull();
        module.close();   // 不抛
    }

    @Test
    void closeShutsDownGracefully() {
        FakeMcp.Factory factory = new FakeMcp.Factory();
        McpModule module = McpModule.builder()
                .servers(Map.of("a", Map.of("transport", "STDIO", "endpoint", "cmd-a")))
                .gracePeriod(Duration.ofMillis(100))
                .factory(factory)
                .build();
        module.close();
        assertThat(factory.latest("a").closed()).isTrue();
    }

    @Test
    void badConfigPushRejectedWithErrorEvent() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        store.replaceAll(List.of(spec("a")));
        FakeMcp.Factory factory = new FakeMcp.Factory();
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        McpModule module = McpModule.builder()
                .store(store)
                .pollInterval(Duration.ofSeconds(30))
                .gracePeriod(Duration.ofMillis(100))
                .factory(factory)
                .recorder(recorder)
                .build();
        try {
            // 推送坏配置（清单重名）：整批拒绝、注册表保持旧清单、记 ERROR Event（phase=refresh）
            store.replaceAll(List.of(spec("a"), spec("a")));
            FakeMcp.await("refresh-rejected ERROR event", 2000, () -> recorder.events().stream()
                    .anyMatch(e -> e.type().equals("ERROR") && "refresh".equals(e.payload().get("phase"))));
            assertThat(module.registry().toolCallbacksFor("app", "agent"))
                    .extracting(c -> c.getToolDefinition().name())
                    .containsExactly("tool_a");
        } finally {
            module.close();
        }
    }
}
