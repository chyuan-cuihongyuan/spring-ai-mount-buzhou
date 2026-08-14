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
import java.util.List;

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
        return new SpringAiMcpConnection(client, callbacks);
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

    private record SpringAiMcpConnection(McpSyncClient client, List<ToolCallback> callbacks)
            implements McpConnection {

        @Override
        public List<ToolCallback> toolCallbacks() {
            return callbacks;
        }

        /** spec 18：SDK 原始口径基线（与 tools/list_changed 通知同名同源）。 */
        @Override
        public List<String> listToolNames() {
            try {
                McpSchema.ListToolsResult result = client.listTools();
                return result == null || result.tools() == null
                        ? List.of()
                        : result.tools().stream().map(McpSchema.Tool::name).toList();
            } catch (RuntimeException e) {
                return List.of(); // 基线取不到 = 漂移检测退化为「与空基线差量」，不阻断建连
            }
        }

        @Override
        public void close() {
            client.close();
        }
    }
}
