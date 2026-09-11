package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

/**
 * 工具结果裁剪装饰器（spec 169 / T533，Vector VRL / jq「取你要的」借鉴）：
 * 工具结果入上下文前过宿主变换（提字段/抽段落/清噪）——<b>fail-open</b>：
 * 变换抛异常 / 返回 null / 空白 → 原样返回（变换是尽力提炼，绝不丢数据；
 * 截断护栏 ToolResultLimiter 仍在下游兜底）。定义透传（装饰器家族同款）。
 *
 * <p>spec 635 / T920：fail-open 可观测——变换常年失效（字段名笔误/上游格式变化）此前
 * 完全静默（每次都拿原文，省得少了但无人知）。{@link #failOpenCount()} 编程面 +
 * {@code buzhou.tool.transform-fail-open} 计数（tag: tool）+ 首次失败 WARN——
 * 「变换失败 100 次」与「变换从未失败」从此可区分。
 */
public final class TransformingToolCallback implements ToolCallback {

    static final String FAIL_OPEN_COUNTER = "buzhou.tool.transform-fail-open";

    private final ToolCallback delegate;
    private final UnaryOperator<String> transform;
    private final AtomicLong failOpens = new AtomicLong();
    private final java.util.concurrent.atomic.AtomicBoolean warned = new java.util.concurrent.atomic.AtomicBoolean();

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

    /** spec 635：fail-open 累计（观测面——非零持续增长 = 变换常年失效）。 */
    public long failOpenCount() {
        return failOpens.get();
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

    /** fail-open：变换异常/null/空白 → 原样 + 计数 + 首次 WARN。 */
    private String applyTransform(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        try {
            String transformed = transform.apply(raw);
            if (transformed == null || transformed.isBlank()) {
                onFailOpen("返回 " + (transformed == null ? "null" : "空白"));
                return raw;
            }
            return transformed;
        } catch (RuntimeException e) {
            onFailOpen("抛异常 " + e.getClass().getSimpleName());
            return raw;
        }
    }

    private void onFailOpen(String reason) {
        failOpens.incrementAndGet();
        String tool = delegate.getToolDefinition() == null ? "unknown"
                : delegate.getToolDefinition().name();
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter(FAIL_OPEN_COUNTER, 1, "tool", boundTag(tool));
        if (warned.compareAndSet(false, true)) {
            java.lang.System.getLogger(TransformingToolCallback.class.getName()).log(
                    java.lang.System.Logger.Level.WARNING,
                    "工具结果变换 fail-open（此后同类仅计数不再刷屏）：tool={0}，{1}——"
                            + "变换常年失效即省得少了，检查变换配置（字段名/格式假设）",
                    tool, reason);
        }
    }

    /** tag 值有界截断（tag 基数纪律——与各模块 bounded 同口径）。 */
    private static String boundTag(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.length() <= TAG_MAX_LENGTH ? value : value.substring(0, TAG_MAX_LENGTH);
    }

    private static final int TAG_MAX_LENGTH = 64;
}
