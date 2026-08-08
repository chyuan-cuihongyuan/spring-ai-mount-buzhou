package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 一条 MCP server 连接（注册表条目的物理载体）。
 *
 * <p>连接级 seam：默认实现 {@link SpringAiMcpConnectionFactory} 用 Spring AI 公开类手工构建
 * {@code McpSyncClient} + {@code SyncMcpToolCallbackProvider}；测试与自定义场景可注入伪连接。
 */
public interface McpConnection extends AutoCloseable {

    /** 该连接当前发现的工具回调（连接建立时快照）。 */
    List<ToolCallback> toolCallbacks();

    /**
     * 关闭底层 client；实现允许阻塞（注册表在独立线程调用并设强杀兜底）。
     *
     * <p>实现 MUST 容忍重复/并发调用：强杀兜底路径会在原 close 线程僵死时于另一线程再次
     * 调用本方法（spec 04 关闭等待语义）。
     */
    @Override
    void close();
}
