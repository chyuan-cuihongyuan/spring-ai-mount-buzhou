package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 分块分解（spec 7002 / U7205 / impl 2254）——sqrt decomposition
 * 区间统计思想（MO's algorithm/线段树替代面同源）：值列按
 * ⌈√n⌉ 分块维护**块级和摘要**——点更新 O(1)（差分回写块和）、
 * 区间和 O(√n)（零整块段直读 + 整块段摘要点），**无递归无
 * 2n 冗余**——每次区间扫全列 O(n)（热点区间反复求和放大）
 * 与线段树递归栈（小数据面过度设计）两种病的折中解。
 *
 * <p>与 FenwickTree（spec 5041）同族不同面：前缀树点更
 * O(log n)/前缀差 vs 分块点更 O(1)/区间直摘；与 SegmentTree
 * 不同面：递归区间树 vs 平铺块摘要。
 */
public final class SqrtDecomposition {

    private final long[] values;
    private final long[] blockSums;
    private final int blockSize;

    /** 建块（null/空列 fail-fast）。 */
    public SqrtDecomposition(long[] initialValues) {
        if (initialValues == null || initialValues.length == 0) {
            throw new IllegalArgumentException("初值列非空");
        }
        this.values = initialValues.clone();
        this.blockSize = (int) Math.ceil(Math.sqrt(values.length));
        this.blockSums = new long[(values.length + blockSize - 1) / blockSize];
        for (int i = 0; i < values.length; i++) {
            blockSums[i / blockSize] += values[i];
        }
    }

    /** 点更新 O(1)（差分回写块和；越域 fail-fast）。 */
    public void update(int index, long newValue) {
        checkIndex(index);
        int block = index / blockSize;
        blockSums[block] += newValue - values[index];
        values[index] = newValue;
    }

    /** 闭区间和 O(√n)（零整块段直读 + 整块段摘要点；界越域/倒置 fail-fast）。 */
    public long rangeSum(int fromInclusive, int toInclusive) {
        checkIndex(fromInclusive);
        checkIndex(toInclusive);
        if (fromInclusive > toInclusive) {
            throw new IllegalArgumentException("区间倒置: [" + fromInclusive + "," + toInclusive + "]");
        }
        int fromBlock = fromInclusive / blockSize;
        int toBlock = toInclusive / blockSize;
        long sum = 0;
        if (fromBlock == toBlock) {
            for (int i = fromInclusive; i <= toInclusive; i++) {
                sum += values[i];
            }
            return sum;
        }
        for (int i = fromInclusive; i < (fromBlock + 1) * blockSize; i++) {
            sum += values[i];
        }
        for (int b = fromBlock + 1; b < toBlock; b++) {
            sum += blockSums[b];
        }
        for (int i = toBlock * blockSize; i <= toInclusive; i++) {
            sum += values[i];
        }
        return sum;
    }

    /** 点读（越域 fail-fast）。 */
    public long get(int index) {
        checkIndex(index);
        return values[index];
    }

    /** 长度读数。 */
    public int size() {
        return values.length;
    }

    /** 块大小读数（⌈√n⌉）。 */
    public int blockSize() {
        return blockSize;
    }

    /** 块数读数（⌈n/块⌉）。 */
    public int blockCount() {
        return blockSums.length;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("下标越域 [0," + values.length + "): " + index);
        }
    }
}
