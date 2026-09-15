package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;

/**
 * 租约续期抖动读面（L 会话 1700 系 R9 = effort #1708 / spec 1708 /
 * 票 T2617 + T2618 / impl 1308）——etcd lease keepalive / ZooKeeper session
 * 的续期节奏健康思想：续租间隔本应恒定，抖动（变异系数）显形 GC 停顿/
 * 锁竞争/调度饥荒——「续上了」不等于「续得稳」。
 *
 * <p>纯函数零状态：吃续期间隔序列（毫秒，正数；负值忽略），吐均值/
 * 变异系数 cv（总体标准差/均值）/最大偏斜 max−min。n&lt;2 哨兵 cv=−1
 * （无散布可言）；n=0 均值 0。
 *
 * @since 1.0.0
 */
public final class LeaseRenewalStats {

    private LeaseRenewalStats() {
    }

    /**
     * @param samples      有效间隔样本数（负值忽略后）
     * @param meanMillis   平均续期间隔（无样本 0）
     * @param cv           变异系数（总体 std/mean；n&lt;2 哨兵 −1）
     * @param maxSkewMillis 最大偏斜 max−min（n&lt;2 哨兵 −1）
     */
    public record RenewalReport(int samples, double meanMillis,
                                double cv, long maxSkewMillis) {
    }

    /** 审计入口：续期间隔序列（毫秒）。 */
    public static RenewalReport analyze(List<Long> intervalsMillis) {
        List<Long> data = intervalsMillis == null ? List.of() : intervalsMillis;
        List<Long> valid = data.stream().filter(v -> v >= 0).toList();
        int n = valid.size();
        if (n == 0) {
            return new RenewalReport(0, 0d, -1d, -1L);
        }
        if (n == 1) {
            return new RenewalReport(1, valid.get(0).doubleValue(), -1d, -1L);
        }
        double mean = valid.stream().mapToLong(Long::longValue).average().orElse(0d);
        double variance = valid.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .sum() / n;
        double cv = Math.sqrt(variance) / mean;
        long min = valid.stream().mapToLong(Long::longValue).min().orElse(0L);
        long max = valid.stream().mapToLong(Long::longValue).max().orElse(0L);
        return new RenewalReport(n, mean, cv, max - min);
    }
}
