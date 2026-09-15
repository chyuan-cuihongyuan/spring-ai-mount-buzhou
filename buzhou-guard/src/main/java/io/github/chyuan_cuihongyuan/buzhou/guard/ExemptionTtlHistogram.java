package io.github.chyuan_cuihongyuan.buzhou.guard;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 豁免 TTL 直方（L 会话 1700 系 R40 = effort #1739 / spec 1739 /
 * 票 T2679 + T2680 / impl 1339）——cert-manager 证书生命周期普查思想：
 * {@link GuardExemptionRegistry} 的豁免授权 TTL 分布回答「豁免是短票
 * 还是长期通行证」——长期豁免堆积 = 权限漂移的温床。
 *
 * <p>实例面线程安全：默认边界 {1m,10m,1h,24h} → 5 桶 + `permanent`
 * 独立计数（TTL≤0 = 永久，不入桶）。桶式房规。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class ExemptionTtlHistogram {

    /** 默认桶边界（毫秒）：1m/10m/1h/24h → 5 桶。 */
    public static final long[] DEFAULT_BOUNDS = {
            60_000L, 600_000L, 3_600_000L, 86_400_000L};

    private final long[] bounds;
    private final AtomicLongArray buckets;
    private final AtomicLong permanent = new AtomicLong();
    private final AtomicLong total = new AtomicLong();

    /** 默认边界。 */
    public ExemptionTtlHistogram() {
        this(DEFAULT_BOUNDS);
    }

    /** 自定义升序边界（n 边界 n+1 桶；null/空 = 单桶+永久）。 */
    public ExemptionTtlHistogram(long[] bounds) {
        this.bounds = bounds == null ? new long[0] : bounds.clone();
        this.buckets = new AtomicLongArray(this.bounds.length + 1);
    }

    /**
     * 记一笔豁免 TTL（毫秒；负值忽略，0 = 永久豁免入 permanent 计数不入桶）。
     */
    public void record(long ttlMillis) {
        if (ttlMillis < 0) {
            return;
        }
        total.incrementAndGet();
        if (ttlMillis == 0) {
            permanent.incrementAndGet();
            return;
        }
        int bucket = 0;
        while (bucket < bounds.length && ttlMillis >= bounds[bucket]) {
            bucket++;
        }
        buckets.incrementAndGet(bucket);
    }

    /** 桶计数只读（长度 = 边界数+1，不含永久）。 */
    public long[] bucketCounts() {
        long[] out = new long[buckets.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = buckets.get(i);
        }
        return out;
    }

    /** 永久豁免计数（权限漂移哨戒）。 */
    public long permanentCount() {
        return permanent.get();
    }

    /** 已记录总数。 */
    public long total() {
        return total.get();
    }
}
