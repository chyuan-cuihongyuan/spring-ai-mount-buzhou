package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 事件循环滞后探针（spec 1923 / T3047 / impl 1524）——Node.js
 * event loop lag 惯例：调度一个定时器，实际执行时刻与调度时刻之
 * 差即滞后——循环被同步任务/回调塞满的程度直读。「任务慢」归因
 * 下游之前，先看调度器本身还灵不灵。
 *
 * <p>纯函数零状态；负滞后（提前执行——定时器合并噪声）钳 0。
 */
public final class EventLoopLagProbe {

    private EventLoopLagProbe() {
    }

    /**
     * 滞后读数：executed − scheduled 与 0 取大。契约：两时刻 ≥ 0
     * （fail-fast）。
     */
    public static long lagMillis(long scheduledMillis, long executedMillis) {
        if (scheduledMillis < 0 || executedMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "时刻不能为负：scheduled=%d, executed=%d",
                    scheduledMillis, executedMillis));
        }
        return Math.max(0, executedMillis - scheduledMillis);
    }

    /**
     * 判定：lag ≤ threshold → OK（边界含上——恰在阈值内仍健康）；
     * 否则 SATURATED。契约：threshold ≥ 0（fail-fast）。
     */
    public static boolean saturated(long lag, long threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException(
                    "threshold 不能为负：" + threshold);
        }
        return lag > threshold;
    }
}
