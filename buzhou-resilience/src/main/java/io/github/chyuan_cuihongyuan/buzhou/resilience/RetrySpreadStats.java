package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.util.List;

/**
 * 重试抖动实效读面（L 会话 1700 系 R46 = effort #1745 / spec 1745 /
 * 票 T2691 + T2692 / impl 1346）——AWS Builders' Library / Envoy 的
 * backoff jitter 思想：jitter 的全部目的就是把重试在时间上摊开——
 * 实际重试延迟的**相对散布**（(max−min)/mean）是 jitter 有没有干活的
 * 直接量度：≈0 = 全撞在同一时刻（jitter 失效或没配）。
 *
 * <p>纯函数零状态：`analyze(observedDelaysMillis)` → `SpreadReport(count/
 * meanMillis/relativeSpread/minMillis/maxMillis)`。n&lt;2 哨兵 −1；
 * 负值样本忽略；mean=0（全零延迟）散布记 0。
 *
 * @since 1.0.0
 */
public final class RetrySpreadStats {

    private RetrySpreadStats() {
    }

    /**
     * @param count           有效样本数
     * @param meanMillis      平均延迟
     * @param relativeSpread  相对散布 (max−min)/mean（n&lt;2 哨兵 −1；mean=0 记 0）
     * @param minMillis       最小延迟
     * @param maxMillis       最大延迟
     */
    public record SpreadReport(int count, double meanMillis, double relativeSpread,
                               long minMillis, long maxMillis) {
    }

    /** 审计入口：同批重试的观测延迟序列。 */
    public static SpreadReport analyze(List<Long> observedDelaysMillis) {
        List<Long> data = observedDelaysMillis == null ? List.of() : observedDelaysMillis;
        List<Long> valid = data.stream().filter(v -> v >= 0).toList();
        int n = valid.size();
        if (n < 2) {
            return new SpreadReport(n, n == 1 ? valid.get(0) : 0d, -1d,
                    n == 1 ? valid.get(0) : -1L, n == 1 ? valid.get(0) : -1L);
        }
        double mean = valid.stream().mapToLong(Long::longValue).average().orElse(0d);
        long min = valid.stream().mapToLong(Long::longValue).min().orElse(0L);
        long max = valid.stream().mapToLong(Long::longValue).max().orElse(0L);
        double spread = mean == 0 ? 0d : (max - min) / mean;
        return new SpreadReport(n, mean, spread, min, max);
    }
}
