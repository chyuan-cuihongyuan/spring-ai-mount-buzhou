package io.github.chyuan_cuihongyuan.buzhou.mcp.config;

import io.github.chyuan_cuihongyuan.buzhou.mcp.McpClientRegistry;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpToolHints;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 健康面注解聚合测试（spec 618 / T886–T887 / impl 471）：
 * selfReportedDestructiveToolCount 进 details（观测口径），与客户端风险分类 dangerousToolCount 分列。
 */
class McpHealthHintsTest {

    /** 带 hints 聚合与危险名登记的注册表替身。 */
    private static final class StubRegistry implements McpClientRegistry {
        private final Map<String, Map<String, McpToolHints>> hints;
        private final Set<String> dangerous;

        StubRegistry(Map<String, Map<String, McpToolHints>> hints, Set<String> dangerous) {
            this.hints = hints;
            this.dangerous = dangerous;
        }

        @Override
        public List<ToolCallback> toolCallbacksFor(String appId, String agentName) {
            return List.of();
        }

        @Override
        public void refresh(List<io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec> newSpecs) {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public Set<String> dangerousToolNames() {
            return dangerous;
        }

        @Override
        public Map<String, Map<String, McpToolHints>> toolHints() {
            return hints;
        }
    }

    /** 自报 destructive 计数进 details；与客户端危险分类（独立口径）分列。 */
    @Test
    void selfReportedDestructiveCountInDetails() {
        Map<String, Map<String, McpToolHints>> hints = Map.of(
                "srv-a", Map.of(
                        "drop_table", new McpToolHints("删表", false, true, false, false),
                        "query", new McpToolHints("查询", true, false, true, false)),
                "srv-b", Map.of(
                        "purge", new McpToolHints("清库", false, true, false, true)));
        McpClientRegistry registry = new StubRegistry(hints, Set.of("drop_table"));

        var health = new BuzhouMcpHealthAutoConfiguration.McpHealth(true, registry);
        assertThat(health.status()).isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP);
        Map<String, Object> details = health.details();
        assertThat(details).containsEntry("dangerousToolCount", 1);      // 客户端风险分类
        assertThat(details).containsEntry("selfReportedDestructiveToolCount", 2L); // server 自报（观测）
    }

    /** 空 hints（伪连接/旧 server）与禁用路径零计数安全。 */
    @Test
    void emptyAndDisabledSafe() {
        var empty = new BuzhouMcpHealthAutoConfiguration.McpHealth(true,
                new StubRegistry(Map.of(), Set.of()));
        assertThat(empty.details()).containsEntry("selfReportedDestructiveToolCount", 0L);

        var disabled = new BuzhouMcpHealthAutoConfiguration.McpHealth(false, null);
        assertThat(disabled.details()).containsEntry("enabled", false);
    }
}
