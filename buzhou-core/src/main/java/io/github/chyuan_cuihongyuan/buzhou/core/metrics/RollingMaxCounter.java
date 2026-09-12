package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.LongSupplier;

/**
 * 时间桶滚动 max（spec 708 / T967，micrometer {@code Timer} max 发布衰减窗借鉴）：
 * 固定桶环（{@value #DEFAULT_BUCKETS} 桶 × {@value #DEFAULT_BUCKET_MILLIS}ms 默认），
 * {@link #record(long)} 按时钟定位桶——过期桶先重置再 CAS 更新桶内 max；
 * {@link #max()} = 活跃桶最大。<b>诚实口径</b>：全部桶过期（窗口内无样本）返回 0——
 * 「没有近期样本」与「近期 max 很小」可区分（进程生命周期 max 永不衰减的误导面
 * 由本读面修正：修好慢 hook 后「现在还慢不慢」可答）。
 *
 * <p>时钟 {@link LongSupplier} 注入（默认系统毫秒——测试可推进零等待）。线程安全
 * （AtomicLongArray + CAS）；record 常数开销可上热路径。
 */
public final class RollingMaxCounter {

    /** 默认桶数。 */
    public static final int DEFAULT_BUCKETS = 8;
    /** 默认桶时长（毫秒）——8×10s = 80s 观测窗。 */
    public static final long DEFAULT_BUCKET_MILLIS = 10_000L;

    private final AtomicLongArray slots;
    private final AtomicLongArray stamps;
    private final int buckets;
    private final long bucketMillis;
    private final LongSupplier clockMillis;

    /**
     * @param buckets      桶数（≥1）
     * @param bucketMillis 桶时长毫秒（≥1）
     * @param clockMillis  毫秒时钟
     */
    public RollingMaxCounter(int buckets, long bucketMillis, LongSupplier clockMillis) {
        if (buckets < 1 || bucketMillis < 1 || clockMillis == null) {
            throw new IllegalArgumentException("buckets>=1、bucketMillis>=1、clock 非空");
        }
        this.buckets = buckets;
        this.bucketMillis = bucketMillis;
        this.clockMillis = clockMillis;
        this.slots = new AtomicLongArray(buckets);
        this.stamps = new AtomicLongArray(buckets);
    }

    /** 默认窗构造（8×10s，系统时钟）。 */
    public RollingMaxCounter() {
        this(DEFAULT_BUCKETS, DEFAULT_BUCKET_MILLIS, System::currentTimeMillis);
    }

    /** 累计一个样本。 */
    public void record(long value) {
        long now = clockMillis.getAsLong();
        long generation = now / bucketMillis;
        int idx = bucketIndex(now);
        // 判代重置：本桶时间戳非当前代 → 过期，抢写新代时间戳并清零槽
        if (stamps.getAndSet(idx, generation) != generation) {
            slots.set(idx, 0);
        }
        long currentMax;
        do {
            currentMax = slots.get(idx);
            if (value <= currentMax) {
                return;
            }
        } while (!slots.compareAndSet(idx, currentMax, value));
    }

    private int bucketIndex(long now) {
        return (int) Math.floorDiv(now, bucketMillis) % buckets;
    }

    /** 滚动 max（窗口 = buckets×bucketMillis；窗口内无样本 = 0）。 */
    public long max() {
        long now = clockMillis.getAsLong();
        long currentGeneration = now / bucketMillis;
        long oldestLiveGeneration = currentGeneration - buckets + 1;
        long best = 0;
        for (int i = 0; i < buckets; i++) {
            long stamp = stamps.get(i);
            if (stamp > oldestLiveGeneration - 1) {
                best = Math.max(best, slots.get(i));
            }
        }
        return best;
    }
}
