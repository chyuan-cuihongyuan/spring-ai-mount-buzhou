package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 工具集漂移检测测试（spec 18 / T86 / impl-61）：捕获工厂 listener 注入 tools/list_changed，
 * 断言基线差量事件（added/removed）、空差量静默、基线推进（连续漂移各记各的）。
 */
class McpToolsDriftTest {

    private static final Duration GRACE = Duration.ZERO;
    private static final Duration FORCE = Duration.ofSeconds(5);

    /** 捕获 tools/list_changed listener 的工厂 + 带工具名基线的伪连接。 */
    static final class DriftFactory implements McpConnectionFactory {
        final Map<String, Consumer<List<McpSchema.Tool>>> listeners = new ConcurrentHashMap<>();
        final Map<String, List<String>> baselineNames = new ConcurrentHashMap<>();

        @Override
        public McpConnection connect(ToolSetSpec spec) {
            throw new UnsupportedOperationException("用 connect(spec, listener)");
        }

        @Override
        public McpConnection connect(ToolSetSpec spec,
                Consumer<List<McpSchema.Tool>> toolsChangedListener) {
            listeners.put(spec.name(), toolsChangedListener);
            List<String> names = baselineNames.getOrDefault(spec.name(), List.of());
            // 直接实现（FakeMcp.Connection 为 final）：仅暴露基线工具名。
            return new McpConnection() {
                @Override
                public List<org.springframework.ai.tool.ToolCallback> toolCallbacks() {
                    return List.of();
                }

                @Override
                public List<String> listToolNames() {
                    return names;
                }

                @Override
                public void close() {
                }
            };
        }

        void notify(String server, String... toolNames) {
            List<McpSchema.Tool> tools = java.util.Arrays.stream(toolNames)
                    .map(n -> McpSchema.Tool.builder(n).build())
                    .toList();
            Consumer<List<McpSchema.Tool>> listener = listeners.get(server);
            if (listener != null) {
                listener.accept(tools);
            }
        }
    }

    /** 首次漂移：added/removed 全量差量事件；随后同列表再通知 = 空差量静默。 */
    @Test
    void firstDriftEmitsDiffThenSilentOnSameList() {
        DriftFactory factory = new DriftFactory();
        factory.baselineNames.put("srv", List.of("old_a", "keep"));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));

        factory.notify("srv", "keep", "new_b");

        assertThat(recorder.events()).anyMatch(e -> "mcp.tools-drift".equals(e.type())
                && Integer.valueOf(1).equals(e.payload().get("addedCount"))
                && Integer.valueOf(1).equals(e.payload().get("removedCount"))
                && List.of("new_b").equals(e.payload().get("added"))
                && List.of("old_a").equals(e.payload().get("removed")));

        // 基线已推进：同列表重复通知 = 空差量，无新漂移事件（close 等异步事件不在此口径）
        long driftsBefore = driftCount(recorder);
        factory.notify("srv", "keep", "new_b");
        assertThat(driftCount(recorder)).isEqualTo(driftsBefore);
        registry.shutdown();
    }

    /** 连续漂移各记各的：第二次通知只差量第二次的变更。 */
    @Test
    void consecutiveDriftsAdvanceBaseline() {
        DriftFactory factory = new DriftFactory();
        factory.baselineNames.put("srv", List.of("a"));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));

        factory.notify("srv", "a", "b"); // +b
        factory.notify("srv", "b");      // -a

        List<RecordingSpanRecorder.EventView> drifts = recorder.events().stream()
                .filter(e -> "mcp.tools-drift".equals(e.type())).toList();
        assertThat(drifts).hasSize(2);
        assertThat(drifts.get(0).payload().get("added")).isEqualTo(List.of("b"));
        assertThat(drifts.get(1).payload().get("removed")).isEqualTo(List.of("a"));
        registry.shutdown();
    }

    /** 条目下线后到达的迟到通知：丢弃（无归属，不误报）。 */
    @Test
    void lateNotificationAfterRemovalDropped() {
        DriftFactory factory = new DriftFactory();
        factory.baselineNames.put("srv", List.of("a"));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));
        registry.refresh(List.of()); // srv 下线
        long driftsBefore = driftCount(recorder);

        factory.notify("srv", "zzz");

        assertThat(driftCount(recorder)).isEqualTo(driftsBefore); // 迟到通知丢弃，不误报
        registry.shutdown();
    }

    private static long driftCount(RecordingSpanRecorder recorder) {
        return recorder.events().stream().filter(e -> "mcp.tools-drift".equals(e.type())).count();
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP, "http://localhost/" + name,
                Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30), Set.of());
    }
}
