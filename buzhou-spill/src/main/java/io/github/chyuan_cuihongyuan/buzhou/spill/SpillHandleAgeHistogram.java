package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * spill 句柄驻留年龄直方（L 会话 1700 系 R44 = effort #1743 / spec 1743 /
 * 票 T2687 + T2688 / impl 1343）——Redis OBJECT IDLETIME 思想：spill 句柄
 * （{@link SpillHandle}）自创建以来的驻留年龄分布——老句柄堆积 = onload
 * （回收）跟不上 offload，盘上驻留成本与丢失风险同步上涨。
 *
 * <p>实例面线程安全：默认边界 {1m,1h,1d} → 4 桶（&lt;1m/&lt;1h/&lt;1d/更老）
 * +eldestMillis 哨戒。桶式房规。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class SpillHandleAgeHistogram {

    /** 默认桶边界（毫秒）：1m/1h/1d → 4 桶。 */
    public static final long[] DEFAULT_BOUNDS = {60_000L, 3_600_000L, 86_400_000L};

    private final long[] bounds;
    private final AtomicLongArray buckets;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong eldest = new AtomicLong();

    /** 默认边界。 */
    public SpillHandleAgeHistogram() {
        this(DEFAULT_BOUNDS);
    }

    /** 自定义升序边界（n 边界 n+1 桶；null/空 = 单桶）。 */
    public SpillHandleAgeHistogram(long[] bounds) {
        this.bounds = bounds == null ? new long[0] : bounds.clone();
        this.buckets = new AtomicLongArray(this.bounds.length + 1);
    }

    /** 记一个句柄的驻留年龄（负值忽略）。 */
    public void record(long ageMillis) {
        if (ageMillis < 0) {
            return;
        }
        total.incrementAndGet();
        eldest.getAndUpdate(prev -> Math.max(prev, ageMillis));
        int bucket = 0;
        while (bucket < bounds.length && ageMillis >= bounds[bucket]) {
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

    /** 最老驻留年龄（无样本 0）。 */
    public long eldestMillis() {
        return eldest.get();
    }
}
