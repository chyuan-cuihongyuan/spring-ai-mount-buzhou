package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 工具调用批规模直方（L 会话 1700 系 R13 = effort #1712 / spec 1712 /
 * 票 T2625 + T2626 / impl 1312）——OpenAI 并行工具调用 / vLLM batching 的
 * 批规模遥测思想：单轮内工具调用数 1=串行、&gt;1=并行——并行度画像与
 * 「模型会不会用并行工具」的治理依据。
 *
 * <p>实例面线程安全：默认边界 {2,3,4,5} → 5 桶（1 / 2 / 3 / 4 / 5+），
 * 与 {@link ToolInputSizeHistogram} 同款桶式房规；负值/0 忽略（非一轮）。
 *
 * @since 1.0.0
 */
public final class ToolBatchHistogram {

    /** 默认桶边界：批规模 2/3/4/5 → 5 桶（1 | 2 | 3 | 4 | 5+）。 */
    public static final long[] DEFAULT_BOUNDS = {2, 3, 4, 5};

    private final long[] bounds;
    private final AtomicLongArray buckets;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong largest = new AtomicLong();

    /** 默认边界。 */
    public ToolBatchHistogram() {
        this(DEFAULT_BOUNDS);
    }

    /** 自定义升序边界（n 边界 n+1 桶；null/空 = 单桶）。 */
    public ToolBatchHistogram(long[] bounds) {
        this.bounds = bounds == null ? new long[0] : bounds.clone();
        this.buckets = new AtomicLongArray(this.bounds.length + 1);
    }

    /** 记录一轮的工具调用批规模（&lt;1 忽略）。 */
    public void record(int batchSize) {
        if (batchSize < 1) {
            return;
        }
        total.incrementAndGet();
        largest.getAndUpdate(prev -> Math.max(prev, batchSize));
        int bucket = 0;
        while (bucket < bounds.length && batchSize >= bounds[bucket]) {
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

    /** 已记录轮数。 */
    public long total() {
        return total.get();
    }

    /** 最大批规模（无样本 0）。 */
    public int largestBatch() {
        return (int) largest.get();
    }
}
