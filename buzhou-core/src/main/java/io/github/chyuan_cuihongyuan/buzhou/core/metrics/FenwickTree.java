package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Fenwick 树（spec 3015 / T5031 / impl 2016）——树状数组 / Binary
 * Indexed Tree 思想（Fenwick 1994）：lowbit 区间分解——点更新与
 * 前缀和双 O(log n)（朴素数组：更新 O(1) 查询 O(n) 或前缀和缓存
 * 反之——两难一去不回）。流式频次表的地基：计数槽点更新 + 秩/
 * 分位前缀查询（「延迟分桶直方的秩查询」类读数的单件化）。
 *
 * <p>0-based 公共面（内部 1-based lowbit 递推）；元素 long 累加；
 * 非线程安全（单流口径）。
 */
public final class FenwickTree {

    private final long[] tree;
    private final int size;

    /** 容量 ≥1（槽位 0..size−1，初值全零）。 */
    public FenwickTree(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size ≥ 1：" + size);
        }
        this.size = size;
        this.tree = new long[size + 1];
    }

    /** 点更新：槽 index 累加 delta（可负）。 */
    public void add(int index, long delta) {
        requireIndex(index);
        for (int i = index + 1; i <= size; i += i & (-i)) {
            tree[i] += delta;
        }
    }

    /** 前 count 项之和（count∈[0..size]——0 即空前缀）。 */
    public long prefixSum(int count) {
        if (count < 0 || count > size) {
            throw new IndexOutOfBoundsException("count∈[0.." + size + "]：" + count);
        }
        long sum = 0;
        for (int i = count; i > 0; i -= i & (-i)) {
            sum += tree[i];
        }
        return sum;
    }

    /** 区间和 [from, toExclusive)。 */
    public long rangeSum(int from, int toExclusive) {
        if (from < 0 || toExclusive > size || from > toExclusive) {
            throw new IndexOutOfBoundsException("[" + from + "," + toExclusive + ") 越界（size=" + size + "）");
        }
        return prefixSum(toExclusive) - prefixSum(from);
    }

    /** 全量和。 */
    public long total() {
        return prefixSum(size);
    }

    /** 槽位数。 */
    public int size() {
        return size;
    }

    private void requireIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("槽越界：" + index + "（0.." + (size - 1) + "）");
        }
    }
}
