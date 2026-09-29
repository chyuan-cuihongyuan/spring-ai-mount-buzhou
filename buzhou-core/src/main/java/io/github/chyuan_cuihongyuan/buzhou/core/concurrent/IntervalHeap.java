package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * IntervalHeap 双端优先队列（spec 8018 / V8037 / impl 2320）——
 * Atkinson, Sack, Santoro & Strothotte 1986 min-max 堆思想
 * （U 系 Wave 9 退雾区遗珠，本轮认领）：**单数组隐式树、
 * 偶层 min 性质/奇层 max 性质交错**——双端 O(log n) 单份
 * 存储——两个堆双份存储+两堆互删惰性删除堆积的病解。
 * offer 上滤（先与同节点对偶值交换定层）+pollMin/pollMax
 * 下滤+peekMin/peekMax O(1)+size 读数；null 元素 fail-fast；
 * 空堆 poll null 诚实缺省；同操作序同布局完全确定。
 *
 * <p>与 AgingPriorityQueue（exec）同族不同面：老化调度
 * 权重 vs 双端最值数据结构。
 */
public final class IntervalHeap {

    private static final int INITIAL_CAPACITY = 16;

    private long[] items = new long[INITIAL_CAPACITY];
    private int size;

    /** 入队（null 装箱元素由调用方判——原语面只收 long）。 */
    public void offer(long value) {
        if (size == items.length) {
            items = Arrays.copyOf(items, items.length * 2);
        }
        int index = size++;
        items[index] = value;
        trickleUp(index);
    }

    /** 弹最小（空堆 null 诚实缺省）。 */
    public Long pollMin() {
        if (size == 0) {
            return null;
        }
        long min = items[0];
        int last = --size;
        items[0] = items[last];
        trickleDownMin(0);
        return min;
    }

    /** 弹最大（空堆 null 诚实缺省；最大在根的两个孩子之一——勘误：初版硬编码索引 1 漏索引 2）。 */
    public Long pollMax() {
        if (size == 0) {
            return null;
        }
        int maxIndex = maxIndexOf();
        long max = items[maxIndex];
        int last = --size;
        items[maxIndex] = items[last];
        if (maxIndex < size) {
            trickleDown(maxIndex);
        }
        return max;
    }

    /** 看最小（空堆 null 诚实缺省）。 */
    public Long peekMin() {
        return size == 0 ? null : items[0];
    }

    /** 看最大（空堆 null 诚实缺省）。 */
    public Long peekMax() {
        return size == 0 ? null : items[maxIndexOf()];
    }

    /** 最大值槽位：size==1 在根，否则在根的两个孩子（max 层）取大者。 */
    private int maxIndexOf() {
        if (size == 1) {
            return 0;
        }
        if (size == 2) {
            return 1;
        }
        return items[1] >= items[2] ? 1 : 2;
    }

    /** 元素数。 */
    public int size() {
        return size;
    }

    private void trickleUp(int index) {
        if (index == 0) {
            return;
        }
        int parent = (index - 1) / 2;
        if (onMinLevel(index)) {
            if (items[index] > items[parent]) {
                swap(index, parent);
                trickleUpMax(parent);
            } else {
                trickleUpMin(index);
            }
        } else {
            if (items[index] < items[parent]) {
                swap(index, parent);
                trickleUpMin(parent);
            } else {
                trickleUpMax(index);
            }
        }
    }

    private void trickleUpMin(int index) {
        while (hasGrandparent(index)) {
            int grandparent = (index - 3) / 4;
            if (items[index] < items[grandparent]) {
                swap(index, grandparent);
                index = grandparent;
            } else {
                break;
            }
        }
    }

    private void trickleUpMax(int index) {
        while (hasGrandparent(index)) {
            int grandparent = (index - 3) / 4;
            if (items[index] > items[grandparent]) {
                swap(index, grandparent);
                index = grandparent;
            } else {
                break;
            }
        }
    }

    private void trickleDown(int index) {
        if (onMinLevel(index)) {
            trickleDownMin(index);
        } else {
            trickleDownMax(index);
        }
    }

    private void trickleDownMin(int index) {
        while (true) {
            int m = smallestDescendant(index);
            if (m < 0) {
                return;
            }
            if (isGrandchild(index, m)) {
                if (items[m] < items[index]) {
                    swap(m, index);
                    int parent = (m - 1) / 2;
                    if (items[m] > items[parent]) {
                        swap(m, parent);
                    }
                    index = m;
                } else {
                    return;
                }
            } else {
                if (items[m] < items[index]) {
                    swap(m, index);
                }
                return;
            }
        }
    }

    private void trickleDownMax(int index) {
        while (true) {
            int m = largestDescendant(index);
            if (m < 0) {
                return;
            }
            if (isGrandchild(index, m)) {
                if (items[m] > items[index]) {
                    swap(m, index);
                    int parent = (m - 1) / 2;
                    if (items[m] < items[parent]) {
                        swap(m, parent);
                    }
                    index = m;
                } else {
                    return;
                }
            } else {
                if (items[m] > items[index]) {
                    swap(m, index);
                }
                return;
            }
        }
    }

    private int smallestDescendant(int index) {
        return extremeDescendant(index, true);
    }

    private int largestDescendant(int index) {
        return extremeDescendant(index, false);
    }

    private int extremeDescendant(int index, boolean smallest) {
        int firstChild = index * 2 + 1;
        if (firstChild >= size) {
            return -1;
        }
        int best = -1;
        long bestValue = smallest ? Long.MAX_VALUE : Long.MIN_VALUE;
        for (int child = firstChild; child <= firstChild + 1 && child < size; child++) {
            if (smallest ? items[child] < bestValue : items[child] > bestValue) {
                bestValue = items[child];
                best = child;
            }
        }
        int firstGrandchild = firstChild * 2 + 1;
        for (int g = firstGrandchild; g <= firstGrandchild + 3 && g < size; g++) {
            if (smallest ? items[g] < bestValue : items[g] > bestValue) {
                bestValue = items[g];
                best = g;
            }
        }
        return best;
    }

    private boolean isGrandchild(int index, int candidate) {
        return candidate >= index * 4 + 3;
    }

    private boolean onMinLevel(int index) {
        int level = 32 - Integer.numberOfLeadingZeros(index + 1) - 1;
        return level % 2 == 0;
    }

    private boolean hasGrandparent(int index) {
        return index >= 3;
    }

    private void swap(int a, int b) {
        long tmp = items[a];
        items[a] = items[b];
        items[b] = tmp;
    }
}
