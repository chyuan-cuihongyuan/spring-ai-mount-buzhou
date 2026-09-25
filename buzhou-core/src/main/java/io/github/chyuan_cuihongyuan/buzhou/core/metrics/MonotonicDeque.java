package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Monotonic Deque 单调队列（spec 6004 / T6209 / impl 2205）——
 * 固定窗口最大值的**流式摊还 O(1)** 聚合面：队内保窗口元素
 * 下标与严格递减值——新值从尾弹出所有 ≤ 自己者（弱者没有
 * 未来：出窗更早且值更小，永不可能成为最大），队首越窗从
 * 首弹出；队首恒为当前窗最大。每元素至多入队/出队各一次
 * （摊还 O(1)，无墓碑）——每窗重扫 O(k·n)（滑动放大）与
 * 有序堆惰性墓碑删除（O(n log k) 且删除语义腐化）的病解。
 *
 * <p>与 BlockedSlidingCounter（spec 5040）同族不同面：近似
 * 计数+显式误差界 vs 精确最值流式聚合；与 SparseTable
 * （spec 6003）同族不同面：静态离线 O(1) vs 流式在线摊还
 * O(1)。同输入序列同 max 序列（确定性——无随机无时间）。
 */
public final class MonotonicDeque {

    private record Entry(int index, long value) {
    }

    private final Deque<Entry> deque = new ArrayDeque<>();
    private final int windowCapacity;
    private int count;

    /** 定容窗口（capacity≤0 fail-fast）。 */
    public MonotonicDeque(int windowCapacity) {
        if (windowCapacity <= 0) {
            throw new IllegalArgumentException("窗口容量必须为正: " + windowCapacity);
        }
        this.windowCapacity = windowCapacity;
    }

    /** 追加一个值（越窗队首弹出、队尾弱者弹出——摊还 O(1)）。 */
    public void offer(long value) {
        while (!deque.isEmpty() && deque.peekLast().value() <= value) {
            deque.pollLast();
        }
        deque.offerLast(new Entry(count, value));
        if (count - deque.peekFirst().index() >= windowCapacity) {
            deque.pollFirst();
        }
        count++;
    }

    /** 当前窗最大值（窗空 null）。 */
    public Long max() {
        return deque.isEmpty() ? null : deque.peekFirst().value();
    }

    /** 当前窗内元素数读数（≤ capacity）。 */
    public int size() {
        return deque.size();
    }

    /** 是否已喂入过值。 */
    public boolean isEmpty() {
        return deque.isEmpty();
    }
}
