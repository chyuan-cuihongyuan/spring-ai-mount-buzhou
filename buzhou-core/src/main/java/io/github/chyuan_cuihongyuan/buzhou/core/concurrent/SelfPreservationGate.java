package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 自保模式门（spec 1885 / T2971 / impl 1486）——Eureka
 * self-preservation 语义：续约率跌破阈值时「宁保数据不逐实例」——
 * 大面积失联更可能是自己瞎了（网络分区）而不是全死了，朴素逐出会
 * 把健康实例全部清场（重启风暴）。恢复后自动退出自保，过期剔除
 * 照常。
 *
 * <p>持态小门；续约计数由调用方按窗口喂入（心跳收发归租约面）。
 */
public final class SelfPreservationGate {

    private final long expectedRenewals;
    private final double threshold;
    private final AtomicLong renewals = new AtomicLong(0);
    private volatile boolean selfPreserving;

    /**
     * @param expectedRenewals 每窗口期望续约数（实例数 × 每实例心跳数，
     *                         ≥ 1，fail-fast）
     * @param threshold        续约率阈值 ∈ (0,1)——低于即停逐（fail-fast）
     */
    public SelfPreservationGate(long expectedRenewals, double threshold) {
        if (expectedRenewals < 1) {
            throw new IllegalArgumentException(
                    "expectedRenewals 不能小于 1：" + expectedRenewals);
        }
        if (threshold <= 0.0 || threshold >= 1.0) {
            throw new IllegalArgumentException(
                    "threshold 须在 (0,1)：" + threshold);
        }
        this.expectedRenewals = expectedRenewals;
        this.threshold = threshold;
    }

    /** 记一笔续约（窗口内调用）。 */
    public void onRenewal() {
        renewals.incrementAndGet();
    }

    /** 窗口翻新：调用方传入本窗口实际续约数并重置计数基线。 */
    public void resetWindow(long observedRenewals) {
        if (observedRenewals < 0) {
            throw new IllegalArgumentException(
                    "observedRenewals 不能为负：" + observedRenewals);
        }
        renewals.set(observedRenewals);
        selfPreserving = observedRenewals < expectedRenewals * threshold;
    }

    /**
     * 续约率读数：实际/期望（可 > 1——超发续约也是信号）。
     */
    public double renewalRatio() {
        return (double) renewals.get() / expectedRenewals;
    }

    /**
     * 剔除前必查：false = 自保停逐（比率低于阈值），true = 正常逐。
     * 边界「恰等于阈值」按正常逐处理——保守在宁停不误、恢复不含糊。
     */
    public boolean shouldExpire() {
        return !selfPreserving;
    }

    /** 自保态读数（可观测面）。 */
    public boolean selfPreserving() {
        return selfPreserving;
    }
}
