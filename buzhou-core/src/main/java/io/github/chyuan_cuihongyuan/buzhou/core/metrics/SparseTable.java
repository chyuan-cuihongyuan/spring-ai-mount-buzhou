package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Sparse Table 稀疏表（spec 6003 / T6207 / impl 2204）——
 * Bender-Farach 静态 RMQ 思想：**倍增表一次性预计算 +
 * O(1) 查询**的静态序列最小值面——{@code sparse[k][i]} =
 * 从 i 起长 2^k 的最小值；查询取覆盖区间的两个 2^k 块取
 * min（幂等聚合允许重叠——k=⌊log₂(len)⌋，两次访存出值）。
 * 每查一遍线性扫（重复查询 O(n) 放大）与为不发生的更新
 * 支付对数代价（线段树面）的病解。
 *
 * <p>与 SegmentTree（spec 5044）同族不同面：静态 O(1) 幂等
 * 重叠 vs 动态点更新 O(log n)；与 FenwickTree 同族不同面：
 * 前缀和可加聚合 vs 幂等 min。不可变定构（构建后序列只读
 * ——确定性，同序列同查询同值）。
 */
public final class SparseTable {

    private final long[] values;
    /** sparse[k] 从下标 i 起长 2^k 的最小值。 */
    private final long[][] sparse;

    /** 构建即预计算（null/空数组 fail-fast；防御性副本）。 */
    public SparseTable(long[] input) {
        if (input == null) {
            throw new IllegalArgumentException("序列非空");
        }
        if (input.length == 0) {
            throw new IllegalArgumentException("序列不可为空");
        }
        this.values = input.clone();
        int levels = 32 - Integer.numberOfLeadingZeros(values.length);
        this.sparse = new long[levels][];
        sparse[0] = values.clone();
        for (int k = 1; k < levels; k++) {
            int span = 1 << k;
            int count = values.length - span + 1;
            sparse[k] = new long[Math.max(count, 0)];
            for (int i = 0; i < count; i++) {
                sparse[k][i] = Math.min(sparse[k - 1][i], sparse[k - 1][i + (span >> 1)]);
            }
        }
    }

    /** 区间最小值（闭区间 [from, to]；倒置/越界 fail-fast；O(1)）。 */
    public long rangeMin(int from, int to) {
        if (from < 0 || to >= values.length || from > to) {
            throw new IllegalArgumentException("区间非法: [" + from + ", " + to + "]");
        }
        int k = 31 - Integer.numberOfLeadingZeros(to - from + 1);
        return Math.min(sparse[k][from], sparse[k][to - (1 << k) + 1]);
    }

    /** 元素数读数。 */
    public int size() {
        return values.length;
    }
}
