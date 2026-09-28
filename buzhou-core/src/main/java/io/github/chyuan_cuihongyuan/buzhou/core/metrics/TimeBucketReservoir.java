package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间桶样本库（spec 7026 / U7253 / impl 2278）——Dropwizard
 * SlidingWindowReservoir 思想：**样本按时间戳入库，快照按
 * 滑动窗口过滤**——指标只看「最近 N 分钟」（陈旧样本污染
 * 当前读数）的病解。时间由调用方注入（record/snapshot 显式
 * nowNs 参数——零真实时钟依赖，行为完全确定）；水位单调
 * （时间回退 fail-fast——HLC 同款纪律）；快照按入库序返回
 * （不排序——确定性诚实面）。
 *
 * <p>与 ReservoirSample（observability）同族不同面：随机
 * 抽样代表全体 vs 时间窗内全量；与 BlockedSlidingCounter
 * 不同面：计数面 vs 样本值面。
 */
public final class TimeBucketReservoir {

    private static final class Sample {
        final long timestampNs;
        final long value;

        Sample(long timestampNs, long value) {
            this.timestampNs = timestampNs;
            this.value = value;
        }
    }

    private final List<Sample> samples = new ArrayList<>();
    private long highWaterNs = Long.MIN_VALUE;

    /** 入样（nowNs 单调——回退 fail-fast）。 */
    public void record(long value, long nowNs) {
        if (nowNs < highWaterNs) {
            throw new IllegalArgumentException("时间回退: " + nowNs + " < " + highWaterNs);
        }
        highWaterNs = nowNs;
        samples.add(new Sample(nowNs, value));
    }

    /** 窗口快照 [now−window, now] 内样本值（入库序；window≤0 fail-fast）。 */
    public long[] snapshot(long nowNs, long windowNs) {
        if (windowNs <= 0) {
            throw new IllegalArgumentException("窗口须为正: " + windowNs);
        }
        long from = nowNs - windowNs;
        List<Long> out = new ArrayList<>();
        for (Sample sample : samples) {
            if (sample.timestampNs >= from && sample.timestampNs <= nowNs) {
                out.add(sample.value);
            }
        }
        long[] values = new long[out.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = out.get(i);
        }
        return values;
    }

    /** 当前样本总数（含窗外陈旧——主动清理面不假装）。 */
    public int size() {
        return samples.size();
    }

    /** 陈旧清理（now 之前的样本物理移除，返回清理数——惰性快照的显式面）。 */
    public int evictBefore(long nowNs) {
        int removed = 0;
        for (int i = samples.size() - 1; i >= 0; i--) {
            if (samples.get(i).timestampNs < nowNs) {
                samples.remove(i);
                removed++;
            }
        }
        return removed;
    }
}
