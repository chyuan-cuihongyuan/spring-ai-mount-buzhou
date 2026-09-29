package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * 基数排序（spec 8022 / V8045 / impl 2324）——
 * LSD 基数排序思想（Hollerith 打孔卡机同源）：**非负 long
 * 域每轮 8 位 256 桶计数稳定分桶**——O(n·w)（w=字长/8 轮）
 * 线性扫——比较排序 O(n log n) 下界（计数可省比较）的病解。
 * sort 返回新数组（值语义——原数组不动）；全零 max=0 轮空
 * 退化原样副本；负数 fail-fast（偏置变换不做——诚实拒绝而
 * 非静默错排）；null fail-fast；确定性纯函数。
 *
 * <p>与 QuickSelect（spec 7033）同族不同面：全序面 vs 秩
 * 选择面。
 */
public final class RadixSorter {

    private static final int BITS_PER_ROUND = 8;
    private static final int BUCKETS = 1 << BITS_PER_ROUND;
    private static final int BUCKET_MASK = BUCKETS - 1;

    private RadixSorter() {
    }

    /** 排序（返回新数组；null/负数 fail-fast）。 */
    public static long[] sort(long[] values) {
        if (values == null) {
            throw new IllegalArgumentException("序列非空引用");
        }
        for (long value : values) {
            if (value < 0) {
                throw new IllegalArgumentException("非负域（实际 " + value + "）");
            }
        }
        long[] result = Arrays.copyOf(values, values.length);
        if (values.length < 2) {
            return result;
        }
        long max = 0;
        for (long value : values) {
            max = Math.max(max, value);
        }
        long[] working = new long[values.length];
        for (int shift = 0; (max >> shift) > 0; shift += BITS_PER_ROUND) {
            int[] counts = new int[BUCKETS];
            for (long value : result) {
                counts[(int) ((value >>> shift) & BUCKET_MASK)]++;
            }
            for (int bucket = 1; bucket < BUCKETS; bucket++) {
                counts[bucket] += counts[bucket - 1];
            }
            for (int i = result.length - 1; i >= 0; i--) {
                int bucket = (int) ((result[i] >>> shift) & BUCKET_MASK);
                working[--counts[bucket]] = result[i];
            }
            long[] swap = result;
            result = working;
            working = swap;
        }
        return result;
    }
}
