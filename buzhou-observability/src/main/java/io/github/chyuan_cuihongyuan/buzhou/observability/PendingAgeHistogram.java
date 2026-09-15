package io.github.chyuan_cuihongyuan.buzhou.observability;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 待决事件年龄直方（L 会话 1700 系 R34 = effort #1733 / spec 1733 /
 * 票 T2667 + T2668 / impl 1333）——Kafka consumer lag exporter 的 lag
 * 分桶思想：观测管道里待决（pending，尚未落库）事件的年龄分布回答
 * 「管道是实时还是积压」——老事件桶堆积 = 消费侧跟不上了。
 * {@link PendingSnapshot} 给瞬时清单，本面给年龄分布账。
 *
 * <p>实例面线程安全：默认边界 {1s,10s,60s,300s} → 5 桶（&lt;1s/&lt;10s/
 * &lt;1m/&lt;5m/更老）+oldestMillis 哨戒。桶式房规。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class PendingAgeHistogram {

    /** 默认桶边界（毫秒）：1s/10s/1m/5m → 5 桶。 */
    public static final long[] DEFAULT_BOUNDS = {
            1_000L, 10_000L, 60_000L, 300_000L};

    private final long[] bounds;
    private final AtomicLongArray buckets;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong oldest = new AtomicLong();

    /** 默认边界。 */
    public PendingAgeHistogram() {
        this(DEFAULT_BOUNDS);
    }

    /** 自定义升序边界（n 边界 n+1 桶；null/空 = 单桶）。 */
    public PendingAgeHistogram(long[] bounds) {
        this.bounds = bounds == null ? new long[0] : bounds.clone();
        this.buckets = new AtomicLongArray(this.bounds.length + 1);
    }

    /** 记一个待决事件的年龄（负值忽略）。 */
    public void record(long ageMillis) {
        if (ageMillis < 0) {
            return;
        }
        total.incrementAndGet();
        oldest.getAndUpdate(prev -> Math.max(prev, ageMillis));
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

    /** 最老待决年龄（毫秒；无样本 0）。 */
    public long oldestMillis() {
        return oldest.get();
    }
}
