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
    private final McpBreakerTransitionJournal journal;
    private final Map<String, ToolCircuitBreaker.State> lastStates = new java.util.concurrent.ConcurrentHashMap<>();

    public McpServerBreaker(ToolCircuitBreaker.Config config) {
        this(config, null);
    }

    /**
     * spec 814 / T1129：带变迁台账构造——成败记录后检测状态变化，变迁入账
     * （旁路只读，不碰断路器语义）；journal 可空（原行为）。
     */
    public McpServerBreaker(ToolCircuitBreaker.Config config, McpBreakerTransitionJournal journal) {
        this.breaker = new ToolCircuitBreaker(config, null);
        this.journal = journal;
    }

    private void journalIfChanged(String serverName) {
        if (journal == null) {
            return;
        }
        ToolCircuitBreaker.View view = breaker.stateOf(serverName);
        if (view == null) {
            return;
        }
        ToolCircuitBreaker.State current = view.state();
        ToolCircuitBreaker.State previous = lastStates.put(serverName, current);
        if (previous != null && previous != current) {
            journal.record(serverName, previous.name(), current.name(), System.currentTimeMillis());
        }
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
                    journalIfChanged(serverName);
                    throw new IllegalStateException("MCP server [" + serverName
                            + "] 熔断 OPEN（连续失败率超阈）——该 server 工具暂时不可用，"
                            + "请改用其他工具或稍后重试");
                }
                try {
                    String result = delegate.call(toolInput);
                    breaker.recordSuccess(serverName);
                    journalIfChanged(serverName);
                    return result;
                } catch (RuntimeException e) {
                    breaker.recordFailure(serverName);
                    journalIfChanged(serverName);
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
