package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 工具注解观测面测试（spec 600 / T851 / impl 453）：聚合快照、注解漂移独立事件、
 * 名字+注解双漂移各发各的、空差量静默、无注解基线退化跳过、from 映射 null 安全。
 */
class McpToolHintsTest {

    private static final Duration GRACE = Duration.ZERO;
    private static final Duration FORCE = Duration.ofSeconds(5);

    /** 捕获 listener 的工厂 + 可分别设定名字/注解基线的伪连接。 */
    static final class HintsFactory implements McpConnectionFactory {
        final Map<String, Consumer<List<McpSchema.Tool>>> listeners = new ConcurrentHashMap<>();
        final Map<String, List<String>> baselineNames = new ConcurrentHashMap<>();
        final Map<String, Map<String, McpToolHints>> baselineHints = new ConcurrentHashMap<>();

        @Override
        public McpConnection connect(ToolSetSpec spec) {
            throw new UnsupportedOperationException("用 connect(spec, listener)");
        }

        @Override
        public McpConnection connect(ToolSetSpec spec,
                Consumer<List<McpSchema.Tool>> toolsChangedListener) {
            listeners.put(spec.name(), toolsChangedListener);
            List<String> names = baselineNames.getOrDefault(spec.name(),
                    List.copyOf(baselineHints.getOrDefault(spec.name(), Map.of()).keySet()));
            Map<String, McpToolHints> hints = baselineHints.getOrDefault(spec.name(), Map.of());
            return new McpConnection() {
                @Override
                public List<ToolCallback> toolCallbacks() {
                    return List.of();
                }

                @Override
                public List<String> listToolNames() {
                    return names;
                }

                @Override
                public Map<String, McpToolHints> toolHints() {
                    return hints;
                }

                @Override
                public void close() {
                }
            };
        }

        void notify(String server, McpSchema.Tool... tools) {
            Consumer<List<McpSchema.Tool>> listener = listeners.get(server);
            if (listener != null) {
                listener.accept(List.of(tools));
            }
        }
    }

    private static McpSchema.Tool tool(String name, Boolean readOnly, Boolean destructive) {
        McpSchema.ToolAnnotations annotations = McpSchema.ToolAnnotations.builder()
                .readOnlyHint(readOnly)
                .destructiveHint(destructive)
                .build();
        return McpSchema.Tool.builder(name).annotations(annotations).build();
    }

    /** 聚合快照：ACTIVE 条目可见，摘除（DRAINING）即不可见。 */
    @Test
    void aggregatesActiveEntriesOnly() {
        HintsFactory factory = new HintsFactory();
        factory.baselineHints.put("srv", Map.of("t1",
                new McpToolHints("查询", true, false, false, false)));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));

        Map<String, Map<String, McpToolHints>> aggregated = registry.toolHints();
        assertThat(aggregated).containsKey("srv");
        assertThat(aggregated.get("srv").get("t1"))
                .isEqualTo(new McpToolHints("查询", true, false, false, false));

        registry.refresh(List.of()); // srv 下线 → DRAINING 即不可见
        assertThat(registry.toolHints()).doesNotContainKey("srv");
        registry.shutdown();
    }

    /** 同名工具注解翻转：只发注解漂移事件，不发名字差量事件。 */
    @Test
    void hintFlipEmitsHintsDriftWithoutNameDrift() {
        HintsFactory factory = new HintsFactory();
        factory.baselineHints.put("srv", Map.of("t1", new McpToolHints("", true, false, false, false)));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));

        factory.notify("srv", tool("t1", false, true)); // readOnly 翻转 + destructive 翻转

        assertThat(recorder.events()).anyMatch(e -> "mcp.tool-hints-drift".equals(e.type())
                && Integer.valueOf(1).equals(e.payload().get("changedCount"))
                && List.of("t1").equals(e.payload().get("changed")));
        assertThat(recorder.events()).noneMatch(e -> "mcp.tools-drift".equals(e.type()));
        registry.shutdown();
    }

    /** 名字与注解同时漂移：两条独立事件各发各的。 */
    @Test
    void nameAndHintDriftEmitBothEvents() {
        HintsFactory factory = new HintsFactory();
        factory.baselineHints.put("srv", Map.of("t1", new McpToolHints("", true, false, false, false)));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));

        factory.notify("srv", tool("t1", false, null), tool("t2", true, null));

        assertThat(recorder.events()).anyMatch(e -> "mcp.tool-hints-drift".equals(e.type())
                && List.of("t1").equals(e.payload().get("changed")));
        assertThat(recorder.events()).anyMatch(e -> "mcp.tools-drift".equals(e.type())
                && List.of("t2").equals(e.payload().get("added")));
        registry.shutdown();
    }

    /** 注解不变的重复通知：静默（两种漂移事件都不发）。 */
    @Test
    void identicalHintsStaySilent() {
        HintsFactory factory = new HintsFactory();
        factory.baselineHints.put("srv", Map.of("t1", new McpToolHints("", true, false, false, false)));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));

        factory.notify("srv", tool("t1", true, false));
        long driftCount = driftEventCount(recorder);

        factory.notify("srv", tool("t1", true, false)); // 完全一致 → 静默

        assertThat(driftEventCount(recorder)).isEqualTo(driftCount);
        registry.shutdown();
    }

    /** 无注解基线（伪连接/旧 server）的名字差量：不误报价载注解漂移。 */
    @Test
    void baselineWithoutHintsSkipsHintDiff() {
        HintsFactory factory = new HintsFactory();
        factory.baselineNames.put("srv", List.of("a"));
        RecordingSpanRecorder recorder = new RecordingSpanRecorder();
        DefaultMcpClientRegistry registry = new DefaultMcpClientRegistry(factory, GRACE, FORCE, recorder);
        registry.refresh(List.of(spec("srv")));

        factory.notify("srv", tool("a", false, true), tool("b", null, null));

        assertThat(recorder.events()).anyMatch(e -> "mcp.tools-drift".equals(e.type()));
        assertThat(recorder.events()).noneMatch(e -> "mcp.tool-hints-drift".equals(e.type()));
        registry.shutdown();
    }

    /** from 映射：annotations 缺失/null → 空画像；Boolean null → false。 */
    @Test
    void fromMapsNullSafety() {
        assertThat(McpToolHints.from(McpSchema.Tool.builder("x").build())).isEqualTo(McpToolHints.empty());
        assertThat(McpToolHints.from(tool("x", null, null)))
                .isEqualTo(new McpToolHints("", false, false, false, false));
    }

    private static long driftEventCount(RecordingSpanRecorder recorder) {
        return recorder.events().stream()
                .filter(e -> "mcp.tool-hints-drift".equals(e.type()) || "mcp.tools-drift".equals(e.type()))
                .count();
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STREAMABLE_HTTP, "http://localhost/" + name,
                Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(30), Set.of());
    }
}
