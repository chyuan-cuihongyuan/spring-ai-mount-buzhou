package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 进程级重试预算全局默认（spec 302 / T595，BuzhouMetricsHolder / ToolResultLimiterHolder
 * 同型 Holder 模式）：装配层（auto-config 据 {@code buzhou.backpressure.retry-budget} 配置）
 * 启动期设定；模型重试（ResilienceAdvisor）与工具重试（RetryingToolCallback）动态读取。
 * 默认 {@code null} = 未启用（两条重试路径行为逐位不变——opt-in 诚实边界）。
 */
public final class RetryBudgetHolder {

    private static final AtomicReference<RetryBudget> CURRENT = new AtomicReference<>(null);

    private RetryBudgetHolder() {
    }

    /** 当前进程级预算（null = 未启用）。 */
    public static RetryBudget current() {
        return CURRENT.get();
    }

    /** 设定进程级预算（null = 停用；容器关闭时装配层清理防跨上下文残留）。 */
    public static void set(RetryBudget budget) {
        CURRENT.set(budget);
    }
}
