package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * Interpolation Search 插值查找（spec 6017 / T6237 续 / impl 2220）——
 * 均匀分布有序数组的插值探测思想（数据库统计/数值库同源）：
 * **按值域线性内插估位**——pos = lo + (key−a[lo])·(hi−lo)/(a[hi]
 * −a[lo])，均匀分布期望 O(log log n)（二分的对半探测在此
 * 浪费分布信息）；双精度内插 + [lo,hi] 钳制保证正确性不依赖
 * 分布（最坏退化为 O(n) 有序扫——诚实边界）。等值窗口零除
 * 守卫（a[hi]==a[lo] 时直接判等）。
 *
 * <p>与 Arrays.binarySearch 同族不同面：分布感知内插 vs
 * 固定对半；与 SparseTable（T4）不同面：动态键探测 vs
 * 静态区间聚合。静态工具面（无状态）。
 */
public final class InterpolationSearch {

    private InterpolationSearch() {
    }

    /**
     * 有序升序数组查找（找到返回任一下标——重复值不承诺具体
     * 位次；未找到 -1；null 数组 fail-fast）。
     */
    public static int search(long[] sortedArray, long key) {
        if (sortedArray == null) {
            throw new IllegalArgumentException("数组非空");
        }
        int lo = 0;
        int hi = sortedArray.length - 1;
        while (lo <= hi && key >= sortedArray[lo] && key <= sortedArray[hi]) {
            if (sortedArray[lo] == sortedArray[hi]) {
                return sortedArray[lo] == key ? lo : -1;
            }
            double numerator = (double) key - (double) sortedArray[lo];
            double denominator = (double) sortedArray[hi] - (double) sortedArray[lo];
            int pos = lo + (int) Math.round(numerator / denominator * (hi - lo));
            if (pos < lo) {
                pos = lo;
            }
            if (pos > hi) {
                pos = hi;
            }
            if (sortedArray[pos] == key) {
                return pos;
            }
            if (sortedArray[pos] < key) {
                lo = pos + 1;
            } else {
                hi = pos - 1;
            }
        }
        return -1;
    }
}
