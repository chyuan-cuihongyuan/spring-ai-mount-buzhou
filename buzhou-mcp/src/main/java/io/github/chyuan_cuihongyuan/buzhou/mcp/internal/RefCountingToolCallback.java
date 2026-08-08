package io.github.chyuan_cuihongyuan.buzhou.mcp.internal;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * 引用计数包装（spec 04 推演：计数挂在 ToolCallback 包装层，与 ToolCall Span 包装点同位）。
 *
 * <p>获取/释放在工具调用边界：入口 {@code inFlight++}、finally {@code inFlight--} 并触发关闭检查。
 * 条目已非 ACTIVE（DRAINING/CLOSED）时拒绝执行——「DRAINING 不再接新调用」的硬边界；
 * 拒绝走「失败转文本」（同 06 号档工具失败语义），不抛异常中断工具循环。
 *
 * <p>与关闭路径的竞态收口：{@code acquire} 与关闭置 CLOSED 同在条目锁内，故不会出现
 * 「计数已 +1 而连接已关」的窗口——已取快照但尚未开始的调用在 acquire 处被拒，
 * 已开始的调用持引用挡住关闭直至完成。
 */
public class RefCountingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final DefaultMcpClientRegistry.Entry entry;
    private final DefaultMcpClientRegistry registry;

    public RefCountingToolCallback(ToolCallback delegate, DefaultMcpClientRegistry.Entry entry,
                                   DefaultMcpClientRegistry registry) {
        this.delegate = delegate;
        this.entry = entry;
        this.registry = registry;
    }

    /** 被包装的原回调（供断言/解包）。 */
    public ToolCallback delegate() {
        return delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, new ToolContext(java.util.Map.of()));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        if (!registry.tryAcquire(entry)) {
            return "MCP server '" + entry.name() + "' 已被配置热更摘除，该工具调用未执行";
        }
        try {
            return delegate.call(toolInput, toolContext);
        } finally {
            registry.release(entry);
        }
    }
}
