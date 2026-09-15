package io.github.chyuan_cuihongyuan.buzhou.core.cache;

/**
 * 桶表容量阶梯（spec 1851 / T2903 / impl 1452）——HashMap 负载因子 /
 * 2 的幂容量惯例：桶表（哈希索引/分桶缓存）的容量决策三件套——**建议
 * 容量**（预期条目÷负载因子向上取 2 的幂——位与取模的基数）、**扩容
 * 判定**（size/capacity ≥ 负载因子即该扩——碰撞链长度开始超线性）、
 * **装填度**（当前负载，扩容前瞻）。默认负载因子 0.75（时空折衷惯例）。
 *
 * <p>纯函数零状态、只建议不扩容（rehash 归宿主）。
 */
public final class BucketTableSizing {

    /** 默认负载因子（HashMap 家族时空折衷惯例）。 */
    public static final double DEFAULT_LOAD_FACTOR = 0.75d;

    private BucketTableSizing() {
    }

    /** 扩容判定两态：OK 装填度内 / RESIZE_NEEDED 该扩。 */
    public enum Verdict {

        /** 装填度 < 负载因子——不扩。 */
        OK,

        /** 装填度 ≥ 负载因子——碰撞链将超线性增长，该扩容。 */
        RESIZE_NEEDED
    }

    /**
     * 建议容量：容纳 expectedEntries 的最小 2 的幂（(⌈n/lf⌉ 向上取 2 的幂）。
     * 契约：expectedEntries ≥ 0、loadFactor ∈ (0,1]（fail-fast）；0 条目
     * 返回 1（最小幂）。
     */
    public static int suggestCapacity(int expectedEntries, double loadFactor) {
        if (expectedEntries < 0) {
            throw new IllegalArgumentException(
                    "expectedEntries 不能为负：" + expectedEntries);
        }
        validateLoadFactor(loadFactor);
        long minBuckets = (long) Math.ceil(expectedEntries / loadFactor);
        int capacity = 1;
        while (capacity < minBuckets) {
            capacity <<= 1;
        }
        return capacity;
    }

    /**
     * 扩容判定。契约：capacity ≥ 1、size ≥ 0、size ≤ Integer.MAX_VALUE、
     * loadFactor ∈ (0,1]；语义：装填度 = size/capacity ≥ loadFactor 即
     * RESIZE_NEEDED（边界含——到线即扩，不留侥幸）。
     */
    public static Verdict verdict(int capacity, int size, double loadFactor) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity 不能小于 1：" + capacity);
        }
        if (size < 0) {
            throw new IllegalArgumentException("size 不能为负：" + size);
        }
        validateLoadFactor(loadFactor);
        return (double) size / capacity >= loadFactor
                ? Verdict.RESIZE_NEEDED
                : Verdict.OK;
    }

    /** 当前装填度（capacity ≥ 1 保证分母合法）。 */
    public static double load(int capacity, int size) {
        if (capacity < 1 || size < 0) {
            throw new IllegalArgumentException(
                    "非法入参：capacity=" + capacity + ", size=" + size);
        }
        return (double) size / capacity;
    }

    private static void validateLoadFactor(double loadFactor) {
        if (Double.isNaN(loadFactor) || loadFactor <= 0 || loadFactor > 1) {
            throw new IllegalArgumentException(
                    "loadFactor 须在 (0,1]：" + loadFactor);
        }
    }
}
