package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * MCP client 生命周期注册表（spec 04）：Harness 在 starter 之上自建的注册表层——
 * Spring AI 自动配置无运行时增删 server 的公开 API，本注册表提供差量刷新、
 * 引用计数延迟关闭与绑定级可见性裁剪。
 *
 * <p>实现 MUST 线程安全：{@link #toolCallbacksFor} 在会话每轮工具集构建时并发调用，
 * {@link #refresh} 由配置变更回调触发。
 */
public interface McpClientRegistry {

    /**
     * 解析某 {@code (appId, agentName)} 当前可见的全部 ToolCallback；
     * 返回的是当前 ACTIVE 且绑定匹配条目的快照视图——DRAINING 条目即刻不可见，
     * 已取走的快照内调用继续持引用直至完成（引用计数语义）。
     */
    List<ToolCallback> toolCallbacksFor(String appId, String agentName);

    /** 差量刷新入口：由 {@code ToolSetProvider} 变更回调触发；只增删变化项，未变化连接零重建。 */
    void refresh(List<ToolSetSpec> newSpecs);

    /** 优雅关闭：全部条目录 DRAINING，等待在途归零或兜底到期后关闭连接。 */
    void shutdown();

    /**
     * impl-50 / spec 14 §F：当前 ACTIVE 连接数（健康/指标面）。
     */
    default int activeConnections() {
        return -1;
    }

    /**
     * impl-50 / spec 14 §F：DRAINING 条目数（停机/热更窗口观测）。
     */
    default int drainingConnections() {
        return -1;
    }

    /**
     * impl-50 / spec 14 §F：客户端侧危险工具登记——按调用方配置的动词模式
     * （{@code buzhou.mcp.dangerous-tool-patterns}，如 {@code *.delete*}）过滤当前可见工具名，
     * 供装配侧与 buzhou-tools 的 enabledDangerousToolNames() 同挂点接 guard HITL。
     *
     * <p><b>不信任 server 自报元数据</b>（LangChain 实践：恶意 server 可把删除工具标记为只读）——
     * 危险性由客户端自己的风险分类裁决。
     */
    default java.util.Set<String> dangerousToolNames() {
        return java.util.Set.of();
    }
}
