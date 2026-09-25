package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Median Finder 双堆中位数流（spec 6040 / T6279 / impl 2240）——
 * 流式中位数双堆经典思想：**最大堆保左半+最小堆保右半**
 * ——插入经再平衡（两堆大小差 ≤1），中位数 O(1) 取（奇数
 * 取大堆顶，偶数取两顶均值——奇偶显式返回），插入 O(log n)
 * ——每次查询全量排序 O(n log n) 放大的病解。双堆不变量
 * （左顶≤右顶、大小差≤1）由插入路径维护——确定性（同流
 * 同中位数序列）。
 *
 * <p>与 IndexedHeap（spec 6026）同族不同面：单序堆+位置
 * 映射 vs 双堆夹逼中位数；与 DdSketch（4039）不同面：近似
 * 分位草图 vs 精确流式中位数。
 */
public final class MedianFinder {

    private final PriorityQueue<Long> lower = new PriorityQueue<>(Collections.reverseOrder());
    private final PriorityQueue<Long> upper = new PriorityQueue<>();

    /** 插入一个值（再平衡——两堆大小差恒 ≤1）。 */
    public void insert(long value) {
        if (lower.isEmpty() || value <= lower.peek()) {
            lower.add(value);
        } else {
            upper.add(value);
        }
        if (lower.size() > upper.size() + 1) {
            upper.add(lower.poll());
        } else if (upper.size() > lower.size() + 1) {
            lower.add(upper.poll());
        }
    }

    /** 当前中位数（空返回 null；偶数个返回两中间值下取整均值）。 */
    public Long median() {
        if (lower.isEmpty() && upper.isEmpty()) {
            return null;
        }
        if (lower.size() > upper.size()) {
            return lower.peek();
        }
        if (upper.size() > lower.size()) {
            return upper.peek();
        }
        return (lower.peek() + upper.peek()) >>> 1;
    }

    /** 元素数读数。 */
    public int size() {
        return lower.size() + upper.size();
    }

    /** 是否空。 */
    public boolean isEmpty() {
        return size() == 0;
    }
}
