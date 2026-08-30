package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 轮内 memo 工具装饰器（spec 147 / T501，Hystrix request caching 借鉴）：
 * key = 工具名 + argsHash（与 ToolCallLogEntry.argsHash 同口径）——同轮复读
 * 秒回首轮值（引用一致）；失败不 memo。幂等契约：只包只读/幂等工具。
 * 定义透传（装配面零感知——RetryingToolCallback 同款）。
 */
public final class MemoizedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final TurnMemo memo;

    private MemoizedToolCallback(ToolCallback delegate, TurnMemo memo) {
        this.delegate = delegate;
        this.memo = memo;
    }

    public static MemoizedToolCallback wrap(ToolCallback delegate, TurnMemo memo) {
        if (delegate == null || memo == null) {
            throw new IllegalArgumentException("delegate/memo 必须非空");
        }
        return new MemoizedToolCallback(delegate, memo);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return memo.computeIfAbsent(key(toolInput), () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return memo.computeIfAbsent(key(toolInput),
                () -> delegate.call(toolInput, toolContext));
    }

    private String key(String toolInput) {
        return delegate.getToolDefinition().name() + ":" + ToolCallLogEntry.argsHash(toolInput);
    }
}
