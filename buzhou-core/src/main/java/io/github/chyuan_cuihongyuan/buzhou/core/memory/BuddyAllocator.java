package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Buddy Allocator 伙伴分配器（spec 6030 / T6259 续 / impl 2231）——
 * Linux 内存管理 buddy 思想：**2 的幂块分裂/伙伴合并**——
 * 申请 order k 无现成块时把更大块对半裂开（低半自用高半入
 * 闲链），释放时与伙伴（offset XOR 2^k）逐级合并——外部
 * 碎片收敛回整块。确定性：同分配史同布局；freeList 用
 * TreeSet（同阶多块取最小偏移——可回放）。
 *
 * <p>与 SlabClassPacker（cache）同族不同面：2 的幂分裂合并
 * vs 尺寸分类装箱。单位模型（无字节语义——结构可证）。
 */
public final class BuddyAllocator {

    private final int maxOrder;
    private final Map<Integer, TreeSet<Long>> freeLists = new HashMap<>();
    private final Set<Long> allocated = new HashSet<>();

    /** 建池：总容量 2^maxOrder 单位（maxOrder∈[0,20] fail-fast）。 */
    public BuddyAllocator(int maxOrder) {
        if (maxOrder < 0 || maxOrder > 20) {
            throw new IllegalArgumentException("maxOrder 须在 [0,20]: " + maxOrder);
        }
        this.maxOrder = maxOrder;
        freeLists.computeIfAbsent(maxOrder, k -> new TreeSet<>()).add(0L);
    }

    /** 分配 2^order 单位块，返回块偏移（无块/超阶 fail-fast）。 */
    public long allocate(int order) {
        requireOrder(order);
        int from = order;
        while (from <= maxOrder && freeLists.getOrDefault(from, new TreeSet<>()).isEmpty()) {
            from++;
        }
        if (from > maxOrder) {
            throw new IllegalArgumentException("无足够空闲块: order=" + order);
        }
        long offset = freeLists.get(from).first();
        freeLists.get(from).remove(offset);
        while (from > order) {
            from--;
            freeLists.computeIfAbsent(from, k -> new TreeSet<>()).add(offset + (1L << from));
        }
        allocated.add(offset);
        return offset;
    }

    /** 释放（伙伴逐级合并；双重释放/越阶 fail-fast）。 */
    public void free(long offset, int order) {
        requireOrder(order);
        if (!allocated.remove(offset)) {
            throw new IllegalArgumentException("偏移未分配或已释放: " + offset);
        }
        long merged = offset;
        int o = order;
        while (o < maxOrder) {
            long buddy = merged ^ (1L << o);
            TreeSet<Long> list = freeLists.get(o);
            if (list != null && list.remove(buddy)) {
                merged = Math.min(merged, buddy);
                o++;
            } else {
                break;
            }
        }
        freeLists.computeIfAbsent(o, k -> new TreeSet<>()).add(merged);
    }

    /** 最大阶读数。 */
    public int maxOrder() {
        return maxOrder;
    }

    /** 指定阶空闲块数读数。 */
    public int freeBlocks(int order) {
        requireOrder(order);
        return freeLists.getOrDefault(order, new TreeSet<>()).size();
    }

    private void requireOrder(int order) {
        if (order < 0 || order > maxOrder) {
            throw new IllegalArgumentException("order 须在 [0," + maxOrder + "]: " + order);
        }
    }
}
