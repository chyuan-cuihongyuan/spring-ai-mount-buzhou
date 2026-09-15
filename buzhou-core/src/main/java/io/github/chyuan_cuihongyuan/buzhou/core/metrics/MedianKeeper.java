package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 流式中位数保持器（spec 1869 / T2939 / impl 1470）——双堆（大顶+小顶
 * 对半）经典结构：流式到达的样本不排序、不全存序——两半堆平衡即可
 * O(1) 取中位数、O(log n) 入列。延迟/评分的「当前中枢」要随流更新而
 * 排序法每次 O(n log n)——观测流的中枢读数该有流式结构。双堆不变的
 * 量：小半（大顶堆）的最大值 ≤ 大半（小顶堆）的最小值，且两半大小差
 * ≤ 1（小半可多一）。
 *
 * <p>synchronized 小临界区；样本须为有限实数（NaN/Inf 拒绝）。
 */
public final class MedianKeeper {

    /** 小半（较小的一半）——大顶堆（堆顶为小半最大）。 */
    private final PriorityQueue<Double> lowerHalf =
            new PriorityQueue<>(Comparator.reverseOrder());

    /** 大半（较大的一半）——小顶堆（堆顶为大半最小）。 */
    private final PriorityQueue<Double> upperHalf = new PriorityQueue<>();

    /** 入列：先进小半再平衡（小半至多多一）。契约：非 NaN/Inf。 */
    public synchronized void add(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("样本须为有限实数：" + value);
        }
        if (lowerHalf.size() == 0 || value <= lowerHalf.peek()) {
            lowerHalf.add(value);
        } else {
            upperHalf.add(value);
        }
        rebalance();
    }

    /**
     * 中位数：奇数取小半堆顶（小半多一约定）；偶数取两堆顶均值。
     * 空保持器 -1 哨兵（与读面族口径一致）。
     */
    public synchronized double median() {
        if (lowerHalf.isEmpty()) {
            return -1d;
        }
        if (lowerHalf.size() > upperHalf.size()) {
            return lowerHalf.peek();
        }
        return (lowerHalf.peek() + upperHalf.peek()) / 2.0d;
    }

    /** 已收样本数。 */
    public synchronized long size() {
        return (long) lowerHalf.size() + upperHalf.size();
    }

    /** 平衡不变量：大小差 ≤ 1，小半可多一。 */
    private void rebalance() {
        if (lowerHalf.size() > upperHalf.size() + 1) {
            upperHalf.add(lowerHalf.poll());
        } else if (upperHalf.size() > lowerHalf.size()) {
            lowerHalf.add(upperHalf.poll());
        }
    }
}
