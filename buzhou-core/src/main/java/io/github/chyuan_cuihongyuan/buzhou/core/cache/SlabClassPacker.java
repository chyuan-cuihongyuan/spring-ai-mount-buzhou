package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * Slab 类装箱（spec 4015 / T6031 / impl 2116）——固定尺寸槽分配
 * 思想（Memcached slab allocator）：块尺寸按增长因子（默认 1.25）
 * 几何级数分档，item 归**最小容纳档**——同档等尺寸切槽，分配/释放
 * 永不产生外部碎片（代价是档内**内部浪费**：chunk−item）；
 * 超最大块拒收（−1 档）。
 *
 * <p>「malloc 混尺寸长跑碎片化」病的根治件——缓冲池/对象池/
 * 消息帧池的容量规划地基。与 BucketTableSizing（哈希表桶数组）
 * 同族不同面：彼管表容、本管槽档。附分配记账（per-class 计数）
 * 供容量审计。
 */
public final class SlabClassPacker {

    private final List<Integer> chunkSizes;
    private final long[] allocationCounts;

    /** 定构（chunkSizeMin≥8、growthFactor>1、maxItemSize≥chunkSizeMin 否则 fail-fast）。 */
    public SlabClassPacker(int chunkSizeMin, int maxItemSize, double growthFactor) {
        if (chunkSizeMin < 8 || growthFactor <= 1.0 || maxItemSize < chunkSizeMin) {
            throw new IllegalArgumentException("chunkSizeMin≥8 / growth>1 / max≥min 违反："
                    + chunkSizeMin + "/" + maxItemSize + "/" + growthFactor);
        }
        this.chunkSizes = new ArrayList<>();
        int s = chunkSizeMin;
        chunkSizes.add(s);
        while (s < maxItemSize) {
            int next = (int) Math.min(maxItemSize, Math.ceil(s * growthFactor));
            if (next <= s) {
                next = s + 1;
            }
            chunkSizes.add(next);
            s = next;
        }
        this.allocationCounts = new long[chunkSizes.size()];
    }

    /** item 归档（最小容纳档下标；超最大块 −1 拒收）。 */
    public int classFor(int itemSize) {
        if (itemSize <= 0) {
            throw new IllegalArgumentException("itemSize>0：" + itemSize);
        }
        for (int i = 0; i < chunkSizes.size(); i++) {
            if (chunkSizes.get(i) >= itemSize) {
                return i;
            }
        }
        return -1;
    }

    /** 分配记账（归档并计数；超块 −1 拒收）。 */
    public int allocate(int itemSize) {
        int idx = classFor(itemSize);
        if (idx >= 0) {
            allocationCounts[idx]++;
        }
        return idx;
    }

    /** 档 chunk 尺寸读数（越界 fail-fast）。 */
    public int chunkSizeFor(int classIdx) {
        if (classIdx < 0 || classIdx >= chunkSizes.size()) {
            throw new IllegalArgumentException("classIdx 越界：" + classIdx);
        }
        return chunkSizes.get(classIdx);
    }

    /** 档内浪费比（0–1：chunk−item / chunk；超块 NaN 诚实）。 */
    public double wasteRatio(int itemSize) {
        int idx = classFor(itemSize);
        if (idx < 0) {
            return Double.NaN;
        }
        return (double) (chunkSizes.get(idx) - itemSize) / chunkSizes.get(idx);
    }

    /** 档计数读数（分配记账面；越界 fail-fast）。 */
    public long allocationsIn(int classIdx) {
        if (classIdx < 0 || classIdx >= chunkSizes.size()) {
            throw new IllegalArgumentException("classIdx 越界：" + classIdx);
        }
        return allocationCounts[classIdx];
    }

    /** 档数读数。 */
    public int classCount() {
        return chunkSizes.size();
    }
}
