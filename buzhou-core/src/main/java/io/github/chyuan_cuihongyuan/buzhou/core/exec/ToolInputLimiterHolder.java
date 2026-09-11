package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 工具入参限幅器全局默认（spec 506 / T763，{@link ToolResultLimiterHolder}
 * 同型 Holder 模式）：装配层（auto-config 据配置）启动期设定；默认
 * {@link ToolInputLimiter#disabled()}（零默认行为变化——31 结果限幅的
 * 入站对称面，opt-in）。
 *
 * @since 1.0.0
 */
public final class ToolInputLimiterHolder {

    private static final AtomicReference<ToolInputLimiter> CURRENT =
            new AtomicReference<>(ToolInputLimiter.disabled());

    private ToolInputLimiterHolder() {
    }

    public static ToolInputLimiter current() {
        return CURRENT.get();
    }

    public static void set(ToolInputLimiter limiter) {
        CURRENT.set(limiter == null ? ToolInputLimiter.disabled() : limiter);
    }
}
