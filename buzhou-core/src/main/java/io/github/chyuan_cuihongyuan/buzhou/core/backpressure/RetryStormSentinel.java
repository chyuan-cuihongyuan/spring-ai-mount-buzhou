package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * 重试风暴哨兵（spec 1917 / T3035 / impl 1518）——SRE 重试风暴
 * 惯例：故障期「失败 → 重试 → 再失败」的放大系数让重试占比飙升
 * （Σpⁿ）。占比读数 + 阈值判定让风暴可测可警——等限流器报错时
 * 风暴已成形，哨兵要的是提前一刻。
 *
 * <p>纯函数零状态；计数维护归 RetryBudget 面。
 */
public final class RetryStormSentinel {

    private RetryStormSentinel() {
    }

    /**
     * 重试占比：retried/total。契约：total ≥ 1、0 ≤ retried ≤ total
     * （fail-fast）。
     */
    public static double retryRatio(long totalRequests, long retriedRequests) {
        if (totalRequests < 1) {
            throw new IllegalArgumentException(
                    "totalRequests 不能小于 1：" + totalRequests);
        }
        if (retriedRequests < 0 || retriedRequests > totalRequests) {
            throw new IllegalArgumentException(String.format(
                    "retriedRequests 须在 [0, %d]：%d",
                    totalRequests, retriedRequests));
        }
        return (double) retriedRequests / totalRequests;
    }

    /**
     * 风暴判定：占比 ≥ threshold 即风暴（边界含上）。契约：
     * threshold ∈ (0,1]（fail-fast）。
     */
    public static boolean isStorm(double ratio, double threshold) {
        if (threshold <= 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold 须在 (0,1]：" + threshold);
        }
        return ratio >= threshold;
    }
}
