package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 窃取时间读面（spec 1887 / T2975 / impl 1488）——Linux /proc/stat
 * 的 steal time：虚拟机 CPU tick 被宿主挪给其他租户的账。应用
 * 「自己没干活但也没闲着」时唯一可见的争用信号——宿主超卖下
 * user/system 占比双降但工作没变快，idle 看似充足实为 tick 被偷。
 *
 * <p>纯函数零状态；两采样差分口径（单调计数器）。
 */
public final class StealTimeReadout {

    private StealTimeReadout() {
    }

    /**
     * 窃取占比：Δsteal/Δtotal（total 为全字段 tick 和）。Δtotal=0
     * 哨兵 0.0（未流逝——诚实无信号而非除零）。契约：steal/total
     * 计数单调（curr ≥ prev），fail-fast。
     */
    public static double stealRatio(long prevSteal, long currSteal,
                                    long prevTotal, long currTotal) {
        if (currSteal < prevSteal) {
            throw new IllegalArgumentException(String.format(
                    "steal 计数倒退：%d → %d", prevSteal, currSteal));
        }
        if (currTotal < prevTotal) {
            throw new IllegalArgumentException(String.format(
                    "total 计数倒退：%d → %d", prevTotal, currTotal));
        }
        long totalDelta = currTotal - prevTotal;
        if (totalDelta == 0) {
            return 0.0;
        }
        return (double) (currSteal - prevSteal) / totalDelta;
    }

    /**
     * 争用判定：占比 ≥ threshold 即争用实锤。契约：threshold ∈
     * [0,1]（fail-fast）。
     */
    public static boolean isContended(double ratio, double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold 须在 [0,1]：" + threshold);
        }
        return ratio >= threshold;
    }
}
