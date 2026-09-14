package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.concurrent.atomic.AtomicReference;

/**
 * impl-701 / spec 958：EvalPrunePolicy 进程级兜底 Holder（RetryBudgetHolder
 * 同款模式）：yml 装配（autoconfig bean）写入进程级兜底策略——宿主手动
 * {@code new EvalRunner(...)} 未显式 setPrunePolicy 时，EvalRunner 惰性拾取
 * Holder 兜底（装配面覆盖到非 bean 构造路径）。
 *
 * <p>显式 setPrunePolicy 优先于 Holder（实例级覆盖进程级）；Holder 清理
 * 归装配层 DisposableBean（buzhouRetryBudgetAdapter 先例）。
 */
public final class EvalPrunePolicyHolder {

    private static final AtomicReference<EvalPrunePolicy> CURRENT =
            new AtomicReference<>(null);

    private EvalPrunePolicyHolder() {
    }

    /** 进程级兜底策略（null = 未装配——EvalRunner 保持自身默认关闭）。 */
    public static EvalPrunePolicy current() {
        return CURRENT.get();
    }

    public static void set(EvalPrunePolicy policy) {
        CURRENT.set(policy);
    }

    /** 清理（装配层关闭钩子用）。 */
    public static void clear() {
        CURRENT.set(null);
    }
}
