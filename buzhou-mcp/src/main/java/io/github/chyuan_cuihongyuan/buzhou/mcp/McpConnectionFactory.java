package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.function.Consumer;

/**
 * 连接工厂 seam：按 ToolSetSpec 建物理连接。
 *
 * <p>默认实现 {@link SpringAiMcpConnectionFactory}；测试注入伪实现以断言
 * 「未变化条目连接零重建」「变更 = 删旧增新」等差量语义。
 */
@FunctionalInterface
public interface McpConnectionFactory {

    /** 建连（含初始化握手与工具发现）；失败抛异常由注册表记 ERROR Event 并跳过该条目。 */
    McpConnection connect(ToolSetSpec spec);

    /**
     * 带工具变更监听的建连（spec 18 / T86 / impl-61）：实现应把协议
     * {@code tools/list_changed} 通知透传给 {@code toolsChangedListener}（参数 = server 端
     * 最新工具全量列表）。默认忽略 listener（兼容既有实现与测试 fake）。
     */
    default McpConnection connect(ToolSetSpec spec,
                                  Consumer<List<McpSchema.Tool>> toolsChangedListener) {
        return connect(spec);
    }
}
