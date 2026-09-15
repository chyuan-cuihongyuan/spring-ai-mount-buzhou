package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * 覆写环形缓冲（spec 1832 / T2865 / impl 1433）——LMAX Disruptor 思想：
 * 固定容量环形槽位，**满则覆写最老**——永不阻塞写入方（观测/最近窗场景
 * 的第一美德是「不挡主路」），代价显式入账（overwrites 覆写计数——
 * 丢了多少老样本可审计）。items() 恒以最老到最新序返回当前窗。
 *
 * <p>synchronized 小临界区（槽位翻转与读数原子）；泛型元素非空契约。
 */
public final class OverwritingRingBuffer<T> {

    private final Object[] slots;
    private int head;
    private long size;
    private long overwrites;

    /** 契约：capacity ≥ 1（fail-fast）。 */
    public OverwritingRingBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity 不能小于 1：" + capacity);
        }
        this.slots = new Object[capacity];
    }

    /** 追加（满则覆写最老并计数）；元素非空（fail-fast）。 */
    public synchronized void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("元素不能为 null");
        }
        if (size == slots.length) {
            overwrites++;
        } else {
            size++;
        }
        slots[head] = item;
        head = (head + 1) % slots.length;
    }

    /** 当前窗快照（最老到最新序；防御拷贝——后续 add 不影响已取快照）。 */
    public synchronized List<T> items() {
        @SuppressWarnings("unchecked")
        List<T> result = new ArrayList<>(slots.length);
        int oldest = (int) ((head - size + slots.length) % slots.length);
        for (long i = 0; i < size; i++) {
            result.add((T) slots[(oldest + (int) i) % slots.length]);
        }
        return result;
    }

    /** 只读快照。 */
    public synchronized Snapshot stats() {
        return new Snapshot(slots.length, size, overwrites);
    }

    /** @param capacity 容量；size 当前窗长（≤capacity）；overwrites 累计覆写数 */
    public record Snapshot(int capacity, long size, long overwrites) {

        /** 是否发生过覆写（老样本丢失即入账）。 */
        public boolean hasOverwritten() {
            return overwrites > 0;
        }
    }
}
