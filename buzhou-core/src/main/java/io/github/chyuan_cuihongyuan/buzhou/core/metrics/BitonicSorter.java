package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * 双调排序网络（spec 9040 / W9081 / impl 2393）——Batcher 1968
 * 思想（双调序列递归半清洁比较器网络——GPU 并行排序/排序网络
 * 理论同源）：**任意双调序列经 ⌈log n⌉ 层两两比较清洁为全序
 * ——先递归构造双调（升+降半卷），再递归清洁——数据无关固定
 * 比较模式 O(n log²n) 比较器**——比较排序 O(n log n) 下界之上
 * 换「模式预知、全并行可铺硬件」——分支预测敌对数据（快排
 * 退化 O(n²)）与依赖序数据的病解。比较器序列完全固定（同长
 * 同网络——可硬件直铺）；原地交换；n 为 2 的幂契约（网络
 * 结构域，非幂 fail-fast）；升序输出；确定性纯函数。
 *
 * <p>与 RadixSorter（同包）同域不同面：非比较排序 vs 固定
 * 模式比较网络；与 BitapSearch 等文本族无关——网络结构面。
 */
public final class BitonicSorter {

    private BitonicSorter() {
    }

    /**
     * 原地升序排序（n 须为 2 的幂——网络结构域契约）。
     *
     * @throws IllegalArgumentException null/空数组、n 非 2 的幂
     */
    public static void sort(long[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("数组非空（网络结构域）");
        }
        int n = values.length;
        if ((n & (n - 1)) != 0) {
            throw new IllegalArgumentException("n 为 2 的幂（实际 " + n + "）");
        }
        bitonicSort(values, 0, n, true);
    }

    /** 区间 [from,from+length) 排序（dir=true 升序）——递归构造双调再清洁。 */
    private static void bitonicSort(long[] values, int from, int length, boolean ascending) {
        if (length <= 1) {
            return;
        }
        int half = length / 2;
        bitonicSort(values, from, half, true);
        bitonicSort(values, from + half, half, false);
        bitonicMerge(values, from, length, ascending);
    }

    /** 双调序列清洁：跨半两两比较 + 子序列递归清洁。 */
    private static void bitonicMerge(long[] values, int from, int length, boolean ascending) {
        if (length <= 1) {
            return;
        }
        int half = length / 2;
        for (int i = 0; i < half; i++) {
            compareExchange(values, from + i, from + i + half, ascending);
        }
        bitonicMerge(values, from, half, ascending);
        bitonicMerge(values, from + half, half, ascending);
    }

    private static void compareExchange(long[] values, int i, int j, boolean ascending) {
        if (ascending == values[i] > values[j]) {
            long tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }

    /** 比较器数（与 sort 同构的递归计数——Batcher 网络真值：C(n)=2C(n/2)+M(n)、M(n)=n/2+2M(n/2)）。 */
    public static long comparatorCount(int n) {
        if (n < 2 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("n 为 ≥2 的 2 的幂（实际 " + n + "）");
        }
        if (n == 2) {
            return 1;
        }
        return 2 * comparatorCount(n / 2) + mergeCount(n);
    }

    private static long mergeCount(int n) {
        if (n == 2) {
            return 1;
        }
        return n / 2 + 2 * mergeCount(n / 2);
    }

    /** 便利面：排序副本（原数组不动）。 */
    public static long[] sortedCopy(long[] values) {
        long[] copy = Arrays.copyOf(values, values.length);
        sort(copy);
        return copy;
    }
}
