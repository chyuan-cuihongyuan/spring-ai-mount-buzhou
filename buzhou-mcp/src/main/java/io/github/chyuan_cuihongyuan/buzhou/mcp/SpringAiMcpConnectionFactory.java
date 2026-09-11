package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认连接工厂（spec 04）：用 Spring AI / MCP SDK 公开类手工构建 client——
 * starter 只供协议/传输原材料，不重建其 Bean。
 *
 * <p>建连即初始化握手 + 工具发现（{@code listTools} 快照），失败抛异常由注册表记
 * ERROR Event 并跳过该条目（其余条目照常增删）。
 *
 * <p>endpoint 约定：STDIO 为命令行（首词命令、余为参数，按空白切分）；STREAMABLE_HTTP 为 URL。
 * {@code env} 在 STDIO 下为进程环境变量，在 HTTP 下为请求头。
 */
public class SpringAiMcpConnectionFactory implements McpConnectionFactory {

    private final McpJsonMapper jsonMapper;

    public SpringAiMcpConnectionFactory() {
        this(new JacksonMcpJsonMapper(tools.jackson.databind.json.JsonMapper.builder().build()));
    }

    public SpringAiMcpConnectionFactory(McpJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public McpConnection connect(ToolSetSpec spec) {
        return connect(spec, null);
    }

    /** spec 18 / T86：挂协议 tools/list_changed 订阅（SDK 2.0.0 toolsChangeConsumer），漂移透传注册表。 */
    @Override
    public McpConnection connect(ToolSetSpec spec,
            java.util.function.Consumer<List<io.modelcontextprotocol.spec.McpSchema.Tool>> toolsChangedListener) {
        McpClientTransport transport = switch (spec.transport()) {
            case STDIO -> stdioTransport(spec);
            case STREAMABLE_HTTP -> httpTransport(spec);
        };
        McpClient.SyncSpec clientSpec = McpClient.sync(transport)
                // builder(name, version)：连接名进 title，版本暂不设（clientInfo 仅作 server 侧展示）
                .clientInfo(McpSchema.Implementation.builder("buzhou-mcp", "unknown")
                        .title(spec.name()).build());
        if (spec.requestTimeout() != null) {
            clientSpec.requestTimeout(spec.requestTimeout());
        }
        if (toolsChangedListener != null) {
            clientSpec.toolsChangeConsumer(toolsChangedListener);
        }
        McpSyncClient client = clientSpec.build();
        List<ToolCallback> callbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(List.of(client));
        // spec 600：建连单次 listTools 快照同时供名字基线与注解基线（RPC 次数与此前持平）
        McpSchema.ListToolsResult toolsSnapshot;
        try {
            toolsSnapshot = client.listTools();
        } catch (RuntimeException e) {
            toolsSnapshot = null; // 基线取不到 = 漂移检测退化为「与空基线差量」，不阻断建连
        }
        return new SpringAiMcpConnection(client, callbacks, toolsSnapshot);
    }

    private McpClientTransport stdioTransport(ToolSetSpec spec) {
        String[] parts = spec.endpoint().trim().split("\\s+");
        ServerParameters.Builder params = ServerParameters.builder(parts[0])
                .args(Arrays.asList(parts).subList(1, parts.length));
        spec.env().forEach(params::addEnvVar);
        return new StdioClientTransport(params.build(), jsonMapper);
    }

    private McpClientTransport httpTransport(ToolSetSpec spec) {
        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport
                .builder(spec.endpoint());
        if (spec.connectTimeout() != null) {
            builder.connectTimeout(spec.connectTimeout());
        }
        if (!spec.env().isEmpty()) {
            builder.httpRequestCustomizer((requestBuilder, method, uri, body, context) ->
                    spec.env().forEach(requestBuilder::header));
        }
        return builder.build();
    }

    private record SpringAiMcpConnection(McpSyncClient client, List<ToolCallback> callbacks,
            McpSchema.ListToolsResult toolsSnapshot) implements McpConnection {

        @Override
        public List<ToolCallback> toolCallbacks() {
            return callbacks;
        }

        /** spec 18：SDK 原始口径基线（建连时快照，与 tools/list_changed 通知同名同源）。 */
        @Override
        public List<String> listToolNames() {
            return toolsSnapshot == null || toolsSnapshot.tools() == null
                    ? List.of()
                    : toolsSnapshot.tools().stream().map(McpSchema.Tool::name).toList();
        }

        /** spec 600：工具自报注解基线（建连同一次快照派生，零额外 RPC）。 */
        @Override
        public Map<String, McpToolHints> toolHints() {
            if (toolsSnapshot == null || toolsSnapshot.tools() == null) {
                return Map.of();
            }
            Map<String, McpToolHints> out = new LinkedHashMap<>();
            for (McpSchema.Tool tool : toolsSnapshot.tools()) {
                out.put(tool.name(), McpToolHints.from(tool));
            }
            return Collections.unmodifiableMap(out);
        }

        @Override
        public void close() {
            client.close();
        }
    }
}
