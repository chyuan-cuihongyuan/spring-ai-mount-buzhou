package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Kadane 最大子数组（spec 7048 / U7297 / impl 2300）——
 * Kadane 1984 思想（流式最大子段和经典）：**前缀处either
 * 延续既有子段或另起炉灶（取大）**——单遍 O(n)——全子段
 * 枚举 O(n²)（序列放大）的病解。全负语义明示：允许空段
 * 返回 0（明示，不隐匿）；long 域；确定性纯函数；null
 * fail-fast（空列 0 合法）。
 *
 * <p>与 PatienceLis（同包）同族不同面：最长递增子序列 vs
 * 最大权和连续段。
 */
public final class KadaneMaxSubarray {

    private KadaneMaxSubarray() {
    }

    /** 最大子段和（允许空段——全负返回 0 诚实；null fail-fast）。 */
    public static long maxSubarray(long[] values) {
        if (values == null) {
            throw new IllegalArgumentException("序列非空引用");
        }
        long best = 0;
        long current = 0;
        for (long value : values) {
            current = Math.max(0, current + value);
            best = Math.max(best, current);
        }
        return best;
    }

    /** 非空段语义（全负返回最大单元素——与空段语义区分的显式面）。 */
    public static long maxSubarrayNonEmpty(long[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("非空段语义要求至少一个元素");
        }
        long best = values[0];
        long current = values[0];
        for (int i = 1; i < values.length; i++) {
            current = Math.max(values[i], current + values[i]);
            best = Math.max(best, current);
        }
        return best;
    }
}
