package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 722 / T1044–T1045：MCP 并发占用视图——接口 default 零面、
 * UNSET 哨兵语义。
 */
class McpConcurrencyViewTest {

    @Test
    void unsetSentinelAndDefaults() {
        assertThat(McpConcurrencyView.UNSET).isEqualTo(-1);
        // 接口 default：实现未支持 = 空视图
        McpClientRegistry minimal = new McpClientRegistry() {
            @Override
            public java.util.List<org.springframework.ai.tool.ToolCallback> toolCallbacksFor(
                    String appId, String agentName) {
                return java.util.List.of();
            }

            @Override
            public void refresh(java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec> newSpecs) {
            }

            @Override
            public void shutdown() {
            }
        };
        assertThat(minimal.concurrencyViews()).isEmpty();
    }

    @Test
    void viewRecordCarriesValues() {
        McpConcurrencyView view = new McpConcurrencyView("fs-server", 4, 2, 2);
        assertThat(view.server()).isEqualTo("fs-server");
        assertThat(view.limit()).isEqualTo(4);
        assertThat(view.available()).isEqualTo(2);
        assertThat(view.inFlight()).isEqualTo(2);
    }
}
