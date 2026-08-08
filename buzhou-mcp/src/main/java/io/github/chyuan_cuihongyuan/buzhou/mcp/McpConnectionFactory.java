package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;

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
}
