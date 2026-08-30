package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.function.UnaryOperator;

/**
 * 工具结果裁剪装饰器（spec 169 / T533，Vector VRL / jq「取你要的」借鉴）：
 * 工具结果入上下文前过宿主变换（提字段/抽段落/清噪）——<b>fail-open</b>：
 * 变换抛异常 / 返回 null / 空白 → 原样返回（变换是尽力提炼，绝不丢数据；
 * 截断护栏 ToolResultLimiter 仍在下游兜底）。定义透传（装饰器家族同款）。
 */
public final class TransformingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final UnaryOperator<String> transform;

    private TransformingToolCallback(ToolCallback delegate, UnaryOperator<String> transform) {
        this.delegate = delegate;
        this.transform = transform;
    }

    public static TransformingToolCallback wrap(ToolCallback delegate,
                                                UnaryOperator<String> transform) {
        if (delegate == null || transform == null) {
            throw new IllegalArgumentException("delegate/transform 必须非空");
        }
        return new TransformingToolCallback(delegate, transform);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return applyTransform(delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return applyTransform(delegate.call(toolInput, toolContext));
    }

    /** fail-open：变换异常/null/空白 → 原样。 */
    private String applyTransform(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        try {
            String transformed = transform.apply(raw);
            return transformed == null || transformed.isBlank() ? raw : transformed;
        } catch (RuntimeException e) {
            return raw;
        }
    }
}
