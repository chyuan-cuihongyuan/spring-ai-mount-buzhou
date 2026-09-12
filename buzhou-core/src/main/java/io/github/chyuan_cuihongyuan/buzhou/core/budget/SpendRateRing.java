package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;

/**
 * 花费速率环形窗（spec 403 / T697，AWS Budgets forecast 借鉴）：分钟桶
 * 环形——record 记入当前分钟桶，跨桶滚动清零旧桶；窗内合计与小时速率。
 * 内存上界 = 桶数（有界纪律）；Clock 可注入（测试确定性）。
 */
public final class SpendRateRing {

    /** 默认 1h 窗 = 60 分钟桶。 */
    public static final int DEFAULT_BUCKETS = 60;

    private final long[] buckets;
    private final long[] bucketEpochMinute;
    private final Clock clock;

    public SpendRateRing() {
        this(DEFAULT_BUCKETS, Clock.systemUTC());
    }

    public SpendRateRing(int buckets, Clock clock) {
        if (buckets <= 0) {
            throw new IllegalArgumentException("buckets must be > 0: " + buckets);
        }
        this.buckets = new long[buckets];
        this.bucketEpochMinute = new long[buckets];
        Arrays.fill(this.bucketEpochMinute, -1);
        this.clock = clock;
    }

    /** 记一笔花费（microUsd ≥ 0）到当前分钟桶。 */
    public void record(long microUsd) {
        if (microUsd < 0) {
            throw new IllegalArgumentException("microUsd must be >= 0: " + microUsd);
        }
        long minute = clock.millis() / 60_000L;
        int slot = (int) Math.floorMod(minute, buckets.length);
        synchronized (this) {
            if (bucketEpochMinute[slot] != minute) {
                buckets[slot] = 0; // 旧桶滚动清零（环复用）
                bucketEpochMinute[slot] = minute;
            }
            buckets[slot] += microUsd;
        }
    }

    /** 窗内合计（lookback 截断到桶环容量；microUsd）。 */
    public synchronized long windowTotal(Duration lookback) {
        long minutes = Math.max(1, lookback.toMinutes());
        minutes = Math.min(minutes, buckets.length);
        long nowMinute = clock.millis() / 60_000L;
        long total = 0;
        for (int i = 0; i < buckets.length; i++) {
            if (bucketEpochMinute[i] >= 0 && nowMinute - bucketEpochMinute[i] < minutes) {
                total += buckets[i];
            }
        }
        return total;
    }

    /**
     * 最近 count 个已完成分钟桶总额（不含当前分钟；仅本进程写入过的桶——
     * 陈旧/未写桶剔除；返回长度可能 < count；升序旧→新。spec 508 基线面）。
     */
    public synchronized long[] completedBuckets(int count) {
        long nowMinute = clock.millis() / 60_000L;
        java.util.List<Long> out = new java.util.ArrayList<>(Math.max(0, count));
        for (int back = Math.min(count, buckets.length); back >= 1; back--) {
            long minute = nowMinute - back;
            int slot = (int) Math.floorMod(minute, buckets.length);
            if (bucketEpochMinute[slot] == minute) {
                out.add(buckets[slot]);
            }
        }
        long[] values = new long[out.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = out.get(i);
        }
        return values;
    }

    /** 当前分钟桶累计（microUsd——spec 508 检测面）。 */
    public synchronized long currentBucketTotal() {
        long minute = clock.millis() / 60_000L;
        int slot = (int) Math.floorMod(minute, buckets.length);
        return bucketEpochMinute[slot] == minute ? buckets[slot] : 0;
    }

    /** 小时速率（microUsd/h）：窗内合计按窗长外推。 */
    public long ratePerHour(Duration lookback) {
        long minutes = Math.max(1, Math.min(lookback.toMinutes(), buckets.length));
        long total = windowTotal(lookback);
        if (total == 0) {
            return 0;
        }
        // ceil 除法：零星小额不因整除截断归零
        return (total * 60 + minutes - 1) / minutes;
    }
}
