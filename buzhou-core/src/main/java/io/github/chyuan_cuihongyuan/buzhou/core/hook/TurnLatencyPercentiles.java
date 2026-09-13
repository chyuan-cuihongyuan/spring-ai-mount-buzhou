package io.github.chyuan_cuihongyuan.buzhou.core.hook;

/**
 * impl-763 / spec 1010：轮次时延滚动窗口分位数读数（R-7 线性插值口径——
 * 与 spec 909 同风；p95 即 spec 191 用户故事的「用户体感一轮」基线）。
 *
 * @param count     窗口内样本数（0 = 无样本零值行）
 * @param p50Millis p50（中位）
 * @param p95Millis p95（尾延迟标准问法）
 * @param maxMillis 窗口峰值
 */
public record TurnLatencyPercentiles(long count, double p50Millis, double p95Millis,
        long maxMillis) {
}
