package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import java.util.Arrays;

/**
 * Arena Allocator 竞技场分配器（spec 6034 / T6267 / impl 2235）——
 * Netty/Flink pooled arena 思想：**线性 bump 分配+整池回收**
 * ——分配只前移水位指针（O(1) 无元数据）、freeAll 一次归零、
 * highWaterMark 显形峰值——小对象高频分配走通用堆（GC 压力
 * 放大）的病解。free 单块不支持（竞技场语义——整池回收），
 * 显式抛出诚实边界。
 *
 * <p>与 BuddyAllocator（spec 6030）同族不同面：线性 bump+整池
 * 回收 vs 2 的幂分裂合并；与 SlabClassPacker（cache）不同面：
 * 尺寸分类装箱 vs 单池顺序。确定性定构（同分配序列同偏移）。
 */
public final class ArenaAllocator {

    private final long[] buffer;
    private long watermark;
    private long highWaterMark;

    /** 建池（capacity≤0 fail-fast）。 */
    public ArenaAllocator(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("容量必须为正: " + capacity);
        }
        this.buffer = new long[capacity];
    }

    /** 分配 size 单位（越容/非法 size fail-fast；返回起始偏移）。 */
    public long allocate(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("分配量必须为正: " + size);
        }
        if (watermark + size > buffer.length) {
            throw new IllegalArgumentException("池不足: 需 " + size + " 剩 "
                    + (buffer.length - watermark));
        }
        long offset = watermark;
        watermark += size;
        highWaterMark = Math.max(highWaterMark, watermark);
        Arrays.fill(buffer, (int) offset, (int) watermark, 0L);
        return offset;
    }

    /** 写入（越界/越入他块 fail-fast——块界由分配量守恒）。 */
    public void write(long offset, long value) {
        if (offset < 0 || offset >= watermark) {
            throw new IllegalArgumentException("偏移越界: " + offset);
        }
        buffer[(int) offset] = value;
    }

    /** 读取（越界 fail-fast）。 */
    public long read(long offset) {
        if (offset < 0 || offset >= watermark) {
            throw new IllegalArgumentException("偏移越界: " + offset);
        }
        return buffer[(int) offset];
    }

    /** 整池回收（水位归零；峰值保留）。 */
    public void freeAll() {
        watermark = 0;
        Arrays.fill(buffer, 0L);
    }

    /** 当前水位读数。 */
    public long watermark() {
        return watermark;
    }

    /** 历史峰值水位读数（审计——不被 freeAll 清零）。 */
    public long highWaterMark() {
        return highWaterMark;
    }

    /** 容量读数。 */
    public int capacity() {
        return buffer.length;
    }
}
