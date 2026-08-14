package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * ToolSet 清单（spec 04）：一个 MCP server 连接的完整描述。
 *
 * <p>{@code name} 是清单内唯一键；{@code bindings} 为空集 = 全局生效，非空 = 仅列出的
 * {@code (appId, agentName)} 绑定可见（全局清单的裁剪视图）。
 *
 * <p><b>超时消费点</b>（impl-28 / spec 13 §core-2 勘察结论）：两项超时在 <b>buzhou-mcp
 * 连接层</b>被消费——{@code SpringAiMcpConnectionFactory} 把 {@code requestTimeout} 交给
 * {@code McpSyncClient}（MCP SDK 对每次 RPC 请求——含工具调用请求——施加超时），把
 * {@code connectTimeout} 交给 {@code HttpClientStreamableHttpTransport}（仅 STREAMABLE_HTTP
 * 建连；STDIO 无此消费点，进程启动由 MCP SDK 自管）。core 侧不重复消费：Spring AI
 * {@code ToolCallback} 接口无超时面，Harness 以 {@code min(单工具超时, Turn Deadline 剩余)}
 * 在派发层统一封顶，对本地工具与 MCP 工具一致生效（与 MCP 层超时叠加时取更早触发者）。
 *
 * @param name            清单内唯一键
 * @param transport       传输方式
 * @param endpoint        STDIO: 命令行（首词为命令、余为参数）; STREAMABLE_HTTP: URL
 * @param env             STDIO 环境变量 / HTTP 头
 * @param connectTimeout  连接超时（buzhou-mcp 在 HTTP 传输建连层消费；null = SDK 默认）
 * @param requestTimeout  请求超时（buzhou-mcp 在 McpSyncClient 层按 RPC 消费，含工具调用请求；
 *                        null = SDK 默认）
 * @param bindings        生效范围，空集 = 全局
 */
public record ToolSetSpec(
        String name,
        Transport transport,
        String endpoint,
        Map<String, String> env,
        Duration connectTimeout,
        Duration requestTimeout,
        Set<Binding> bindings) {

    /** 生效范围：{@code (appId, agentName)} 绑定。 */
    public record Binding(String appId, String agentName) {
    }

    public ToolSetSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ToolSetSpec.name must be non-blank");
        }
        if (transport == null) {
            throw new IllegalArgumentException("ToolSetSpec.transport must not be null");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("ToolSetSpec.endpoint must be non-blank");
        }
        env = env == null ? Map.of() : Map.copyOf(env);
        bindings = bindings == null ? Set.of() : Set.copyOf(bindings);
    }

    /**
     * 差量刷新比较（spec 04）：除 {@code bindings} 外全字段相等视为「保持」——
     * 绑定变更不动连接，只更新可见性映射；其余任何字段变化 = 删旧增新（换连接）。
     */
    public boolean sameConnection(ToolSetSpec other) {
        return other != null
                && name.equals(other.name)
                && transport == other.transport
                && endpoint.equals(other.endpoint)
                && env.equals(other.env)
                && java.util.Objects.equals(connectTimeout, other.connectTimeout)
                && java.util.Objects.equals(requestTimeout, other.requestTimeout);
    }

    /** 指定绑定是否可见：空 bindings = 全局可见。 */
    public boolean visibleTo(String appId, String agentName) {
        if (bindings.isEmpty()) {
            return true;
        }
        return bindings.contains(new Binding(appId, agentName));
    }
}
