package io.github.chyuan_cuihongyuan.buzhou.mcp.breaker;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker;
import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

/**
 * MCP 服务器级聚合熔断（spec 504 / T759，resilience4j + Envoy per-host
 * 聚合思想）：一台 server 一个键的断路状态机（复用 core
 * {@link ToolCircuitBreaker}——不复制 resilience4j 语义）；server 宕机时
 * 其全部工具快速失败，模型立即得到结构化信号改道而非反复撞超时。
 *
 * <p>诚实边界：只记**异常**失败（MCP result 内协议级 isErrorCode 不在
 * ToolCallback 面上不计数）；与 per-tool 131 正交两层（server 级聚合、
 * 工具级细粒度互不替代）。
 */
public final class McpServerBreaker {

    private final ToolCircuitBreaker breaker;

    public McpServerBreaker(ToolCircuitBreaker.Config config) {
        this.breaker = new ToolCircuitBreaker(config, null);
    }

    /**
     * 包装回调：熔断拒绝 → {@link IllegalStateException}（标准工具错误路径
     * 回模型——server 名 + 改道指引，不碰网络）；放行 → 真调并记成败。
     */
    public ToolCallback decorate(String serverName, ToolCallback delegate) {
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public String call(String toolInput) {
                if (!breaker.tryAcquirePermission(serverName)) {
                    throw new IllegalStateException("MCP server [" + serverName
                            + "] 熔断 OPEN（连续失败率超阈）——该 server 工具暂时不可用，"
                            + "请改用其他工具或稍后重试");
                }
                try {
                    String result = delegate.call(toolInput);
                    breaker.recordSuccess(serverName);
                    return result;
                } catch (RuntimeException e) {
                    breaker.recordFailure(serverName);
                    throw e;
                }
            }
        };
    }

    /** per-server 熔断状态快照（state/窗内成败/blocked/冷却剩余）。 */
    public Map<String, ToolCircuitBreaker.View> snapshot() {
        return breaker.snapshot();
    }
}
