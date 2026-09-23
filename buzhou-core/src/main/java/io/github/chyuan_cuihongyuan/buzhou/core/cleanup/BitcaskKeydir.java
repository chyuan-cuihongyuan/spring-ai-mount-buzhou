package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.HashMap;
import java.util.Map;

/**
 * Bitcask 键目录合并（spec 4014 / T6029 / impl 2115）——追加日志
 * 存储思想（Riak bitcask）：写只追加（append-only log——顺序写
 * 零放大），读走内存 keydir（key → 最新 (offset,size)），旧版本
 * 与删除留下**死字节**；merge 重写活键回收死区——死比超阈值才
 * 触发（写放大换空间回收的调度杆）。
 *
 * <p>本件为纯账面模型（偏移/字节计数，真 IO 归存储层）：与
 * SizeTieredMergePicker（按尺寸成组）互补——bitcask merge 按
 * 死比触发、整文件重写。与 KeyCompaction（键压缩）同族不同层。
 */
public final class BitcaskKeydir {

    /** 合并计划（活键数 + 活字节——重写成本账）。 */
    public record MergePlan(int liveKeys, long liveBytes) {
    }

    private record Entry(long offset, int sizeBytes) {
    }

    private final Map<String, Entry> keydir = new HashMap<>();
    private long totalBytes;
    private long deadBytes;

    /** 追加写（覆盖旧版本——旧字节入死账；返回落盘偏移）。 */
    public long append(String key, int sizeBytes) {
        if (key == null || key.isEmpty() || sizeBytes < 0) {
            throw new IllegalArgumentException("key 非空且 size≥0");
        }
        long offset = totalBytes;
        Entry previous = keydir.put(key, new Entry(offset, sizeBytes));
        if (previous != null) {
            deadBytes += previous.sizeBytes();
        }
        totalBytes += sizeBytes;
        return offset;
    }

    /** 删除（键出目录，其字节入死账——merge 才真回收）。 */
    public void delete(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        Entry removed = keydir.remove(key);
        if (removed != null) {
            deadBytes += removed.sizeBytes();
        }
    }

    /** 活键判定。 */
    public boolean contains(String key) {
        return keydir.containsKey(key);
    }

    /** 最新值偏移读数（无键 −1）。 */
    public long offsetOf(String key) {
        Entry e = keydir.get(key);
        return e == null ? -1 : e.offset();
    }

    /** 日志总字节（活 + 死）。 */
    public long totalBytes() {
        return totalBytes;
    }

    /** 死字节（旧版本 + 已删除——merge 可回收量）。 */
    public long deadBytes() {
        return deadBytes;
    }

    /** 死比（0–1；空日志 NaN 诚实）。 */
    public double deadRatio() {
        if (totalBytes == 0) {
            return Double.NaN;
        }
        return (double) deadBytes / totalBytes;
    }

    /** 合并门（死比 ≥ 阈值才值得重写）。 */
    public boolean shouldMerge(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("threshold∈[0,1]：" + threshold);
        }
        return deadRatio() >= threshold;
    }

    /** 合并计划（重写成本账——活键数与活字节）。 */
    public MergePlan mergePlan() {
        long live = 0;
        for (Entry e : keydir.values()) {
            live += e.sizeBytes();
        }
        return new MergePlan(keydir.size(), live);
    }

    /** 执行合并（压实：死字节清零、总字节对齐活字节——键目录不动）。 */
    public void applyMerge() {
        MergePlan plan = mergePlan();
        deadBytes = 0;
        totalBytes = plan.liveBytes();
    }
}
