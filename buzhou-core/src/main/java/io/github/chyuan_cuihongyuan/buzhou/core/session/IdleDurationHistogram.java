package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 会话空闲时长分桶直方（spec 841 / T1183，S5 备选池——IdleSessionMonitor
 * 「谁空闲」之外的「空闲多久分布」面）：空闲毫秒落入固定边界桶（可配升序
 * 边界，n 边界 n+1 桶）+总数/最长空闲——「清一批会话能释放多少/边界档位
 * 是否合理」的调参依据。
 *
 * <p>纯记账：桶计数 AtomicLongArray（固定桶无封顶问题）；负值忽略；
 * 默认边界 {1m, 5m, 15m, 60m}（分钟）。喂点=IdleSessionMonitor/清理器
 * 装配侧。
 */
public final class IdleDurationHistogram {

    /** 默认桶边界（毫秒）：1m/5m/15m/60m → 5 桶。 */
    public static final long[] DEFAULT_BOUNDS = {60_000, 300_000, 900_000, 3_600_000};

    private final long[] bounds;
    private final AtomicLongArray buckets;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong longest = new AtomicLong();

    /** 默认边界。 */
    public IdleDurationHistogram() {
        this(DEFAULT_BOUNDS);
    }

    /** 自定义升序边界（n 边界 n+1 桶；null/空 = 单桶）。 */
    public IdleDurationHistogram(long[] bounds) {
        this.bounds = bounds == null ? new long[0] : bounds.clone();
        this.buckets = new AtomicLongArray(this.bounds.length + 1);
    }

    /** 记录一个空闲时长（负值忽略）。 */
    public void record(long idleMillis) {
        if (idleMillis < 0) {
            return;
        }
        total.incrementAndGet();
        longest.getAndUpdate(prev -> Math.max(prev, idleMillis));
        int bucket = bucketOf(idleMillis);
        buckets.incrementAndGet(bucket);
    }

    private int bucketOf(long idleMillis) {
        for (int i = 0; i < bounds.length; i++) {
            if (idleMillis < bounds[i]) {
                return i;
            }
        }
        return bounds.length;
    }

    /** 桶计数只读（长度 = 边界数+1）。 */
    public long[] bucketCounts() {
        long[] out = new long[buckets.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = buckets.get(i);
        }
        return out;
    }

    /** 桶上边界（只读；末桶上界 = Long.MAX_VALUE 语义）。 */
    public long[] bounds() {
        return bounds.clone();
    }

    public long total() {
        return total.get();
    }

    public long longestIdleMillis() {
        return longest.get();
    }

    /** 桶区间人话标签（运维口径）。 */
    public static String bucketLabel(long[] bounds, int index) {
        long lower = index == 0 ? 0 : bounds[index - 1];
        long upper = index >= bounds.length ? Long.MAX_VALUE : bounds[index];
        if (upper == Long.MAX_VALUE) {
            return "≥" + humanize(lower);
        }
        return "[" + humanize(lower) + "," + humanize(upper) + ")";
    }

    private static String humanize(long millis) {
        if (millis == 0) {
            return "0";
        }
        if (millis % 3_600_000 == 0) {
            return (millis / 3_600_000) + "h";
        }
        if (millis % 60_000 == 0) {
            return (millis / 60_000) + "m";
        }
        return String.valueOf(millis);
    }

    /** 全量只读行（辅助快照）。 */
    public record BucketRow(int index, String label, long count) {
    }

    /** 带标签的完整快照。 */
    public List<BucketRow> labeledSnapshot() {
        long[] counts = bucketCounts();
        List<BucketRow> rows = new java.util.ArrayList<>(counts.length);
        for (int i = 0; i < counts.length; i++) {
            rows.add(new BucketRow(i, bucketLabel(bounds, i), counts[i]));
        }
        return List.copyOf(rows);
    }
}
