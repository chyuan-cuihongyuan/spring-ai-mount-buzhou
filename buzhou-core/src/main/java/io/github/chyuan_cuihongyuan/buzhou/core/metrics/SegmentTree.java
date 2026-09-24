package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Segment Tree 线段树（spec 5044 / T6189 / impl 2195）——
 * 区间聚合点更新经典结构（ZKW 2n 迭代式数组实现）：叶层
 * 存原值、父节点存子区间和，`rangeSum` O(log n)——区间
 * 和每次线性扫（区间查询 O(n) 放大）与前缀和数组（点更新
 * O(n) 重算）两种病的同解；`update` O(log n) 自叶向上回填。
 * 确定性无时间依赖。
 *
 * <p>与 FenwickTree（metrics，前缀和 BIT）同族不同面：
 * 树状数组前缀和面 vs 任意区间和+点更新面；与
 * IntervalTree（spec 5022）不同面： stabbing 区间集合 vs
 * 序列区间聚合。
 */
public final class SegmentTree {

    private final int size;
    private final long[] tree;

    /** 定构并建树（null/空数组 fail-fast）。 */
    public SegmentTree(long[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("values 非空非空表");
        }
        this.size = values.length;
        this.tree = new long[2 * size];
        for (int i = 0; i < size; i++) {
            tree[size + i] = values[i];
        }
        for (int i = size - 1; i >= 1; i--) {
            tree[i] = tree[2 * i] + tree[2 * i + 1];
        }
    }

    /** 闭区间 [from, to] 区间和（越界/倒置 fail-fast）。 */
    public long rangeSum(int fromInclusive, int toInclusive) {
        requireRange(fromInclusive, toInclusive);
        long sum = 0;
        int left = fromInclusive + size;
        int right = toInclusive + size + 1;
        while (left < right) {
            if ((left & 1) == 1) {
                sum += tree[left];
                left++;
            }
            if ((right & 1) == 1) {
                right--;
                sum += tree[right];
            }
            left >>= 1;
            right >>= 1;
        }
        return sum;
    }

    /** 单点更新（自叶向上回填 O(log n)）。 */
    public void update(int index, long newValue) {
        requireIndex(index);
        int i = index + size;
        tree[i] = newValue;
        i >>= 1;
        while (i >= 1) {
            tree[i] = tree[2 * i] + tree[2 * i + 1];
            i >>= 1;
        }
    }

    /** 元素数读数。 */
    public int size() {
        return size;
    }

    private void requireRange(int fromInclusive, int toInclusive) {
        if (fromInclusive < 0 || toInclusive >= size || fromInclusive > toInclusive) {
            throw new IllegalArgumentException("区间越界或倒置 [0.." + (size - 1) + "]："
                    + fromInclusive + ".." + toInclusive);
        }
    }

    private void requireIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("下标越界 0.." + (size - 1) + "：" + index);
        }
    }
}
