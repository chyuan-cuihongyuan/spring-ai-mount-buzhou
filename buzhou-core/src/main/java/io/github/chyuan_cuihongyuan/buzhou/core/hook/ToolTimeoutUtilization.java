package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 工具超时余量直方（L 会话 1700 系 R17 = effort #1716 / spec 1716 /
 * 票 T2633 + T2634 / impl 1316）——Envoy 请求超时利用率思想：用时/限时
 * 比的分布回答「超时预算是余量充足还是常年贴线」——贴线桶堆积 = 限值
 * 该调，全在低位 = 限值虚设。{@link ToolTimeoutOverrideStats} 管覆盖改值，
 * 本面管预算利用率。
 *
 * <p>实例面线程安全：`record(durationMillis, limitMillis)` 记利用率
 * ratio=duration/limit（limit≤0 忽略），默认边界 {0.25,0.5,0.75,0.90,1.0}
 * → 6 桶（&lt;25%/&lt;50%/&lt;75%/&lt;90%/&lt;100%/≥100% 超时档）。
 *
 * @since 1.0.0
 */
public final class ToolTimeoutUtilization {

    /** 默认桶边界（利用率比率）：6 桶。 */
    public static final double[] DEFAULT_BOUNDS = {0.25d, 0.5d, 0.75d, 0.90d, 1.0d};

    private final double[] bounds;
    private final AtomicLongArray buckets;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong maxRatioMilli = new AtomicLong();

    /** 默认边界。 */
    public ToolTimeoutUtilization() {
        this(DEFAULT_BOUNDS);
    }

    /** 自定义升序边界（n 边界 n+1 桶；null/空 = 单桶）。 */
    public ToolTimeoutUtilization(double[] bounds) {
        this.bounds = bounds == null ? new double[0] : bounds.clone();
        this.buckets = new AtomicLongArray(this.bounds.length + 1);
    }

    /** 记一次工具执行的时长与限时（limit≤0 忽略——无预算不谈利用率）。 */
    public void record(long durationMillis, long limitMillis) {
        if (limitMillis <= 0) {
            return;
        }
        double ratio = (double) durationMillis / limitMillis;
        total.incrementAndGet();
        long ratioMilli = (long) Math.round(ratio * 1000);
        maxRatioMilli.getAndUpdate(prev -> Math.max(prev, ratioMilli));
        int bucket = 0;
        while (bucket < bounds.length && ratio >= bounds[bucket]) {
            bucket++;
        }
        buckets.incrementAndGet(bucket);
    }

    /** 桶计数只读（长度 = 边界数+1）。 */
    public long[] bucketCounts() {
        long[] out = new long[buckets.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = buckets.get(i);
        }
        return out;
    }

    /** 已记录数。 */
    public long total() {
        return total.get();
    }

    /** 最大利用率（比率，千分精度；无样本 0）。 */
    public double maxRatio() {
        return maxRatioMilli.get() / 1000d;
    }
}
