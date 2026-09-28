package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * QuickSelect 确定性选择（spec 7033 / U7267 / impl 2285）——
 * Blum-Floyd-Pratt-Rivest-Tarjan 1973 **中位数的中位数**
 * 枢轴思想：五数分组取组中位的中位作枢轴——最坏 O(n)
 * ——排序取第 k（O(n log n) 放大）与朴素随机枢轴（最坏
 * O(n²) 对抗输入）的病解。返回副本分区不挪原数组语义
 * （值语义诚实）；并列秩取值域（第 k 小允许多解之一）；
 * 完全确定（无随机）。
 *
 * <p>与 InterpolationSearch（同包）同族不同面：存在性定位
 * vs 秩选择。
 */
public final class QuickSelect {

    private QuickSelect() {
    }

    /** 第 k 小（k∈[0,n) 0 起；null/k 越域 fail-fast）。 */
    public static long select(long[] values, int k) {
        if (values == null) {
            throw new IllegalArgumentException("序列非空");
        }
        if (k < 0 || k >= values.length) {
            throw new IllegalArgumentException("k 越域 [0," + values.length + "): " + k);
        }
        long[] work = values.clone();
        int from = 0;
        int to = work.length - 1;
        while (true) {
            if (from == to) {
                return work[from];
            }
            long pivot = medianOfMedians(work, from, to);
            int less = from;
            int greater = to;
            int cursor = from;
            while (cursor <= greater) {
                if (work[cursor] < pivot) {
                    long t = work[less];
                    work[less] = work[cursor];
                    work[cursor] = t;
                    less++;
                    cursor++;
                } else if (work[cursor] > pivot) {
                    long t = work[greater];
                    work[greater] = work[cursor];
                    work[cursor] = t;
                    greater--;
                } else {
                    cursor++;
                }
            }
            if (k < less) {
                to = less - 1;
            } else if (k > greater) {
                from = greater + 1;
            } else {
                return pivot;
            }
        }
    }

    /** 中位数的中位数枢轴（五数分组）。 */
    private static long medianOfMedians(long[] work, int from, int to) {
        int count = to - from + 1;
        if (count <= 5) {
            insertionSort(work, from, to);
            return work[from + count / 2];
        }
        int[] medians = new int[(count + 4) / 5];
        for (int i = 0; i < medians.length; i++) {
            int groupFrom = from + i * 5;
            int groupTo = Math.min(groupFrom + 4, to);
            insertionSort(work, groupFrom, groupTo);
            medians[i] = (int) (work[groupFrom + (groupTo - groupFrom) / 2]);
        }
        long[] medianValues = new long[medians.length];
        for (int i = 0; i < medians.length; i++) {
            medianValues[i] = medians[i];
        }
        return select(medianValues, (medianValues.length - 1) / 2);
    }

    private static void insertionSort(long[] work, int from, int to) {
        for (int i = from + 1; i <= to; i++) {
            long value = work[i];
            int j = i - 1;
            while (j >= from && work[j] > value) {
                work[j + 1] = work[j];
                j--;
            }
            work[j + 1] = value;
        }
    }
}
