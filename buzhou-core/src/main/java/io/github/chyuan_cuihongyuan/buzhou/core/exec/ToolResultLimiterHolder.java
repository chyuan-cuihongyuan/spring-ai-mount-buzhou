package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 工具结果限幅器全局默认（spec 31 / T110 / impl-85，BuzhouMetricsHolder 同型 Holder
 * 模式）：装配层（auto-config 据配置）启动期设定；会话装配时经
 * {@code ctx.toolManager().setResultLimiter(...)} 可 per-session 覆盖。
 * 默认 {@link ToolResultLimiter#withDefaults()}（20K 字符 + read_range 豁免）。
 *
 * @since 1.0.0
 */
public final class ToolResultLimiterHolder {

    private static final AtomicReference<ToolResultLimiter> CURRENT =
            new AtomicReference<>(ToolResultLimiter.withDefaults());

    private ToolResultLimiterHolder() {
    }

    public static ToolResultLimiter current() {
        return CURRENT.get();
    }

    public static void set(ToolResultLimiter limiter) {
        CURRENT.set(limiter == null ? ToolResultLimiter.withDefaults() : limiter);
    }
}
