package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import java.util.Arrays;

/**
 * External Merge Sort 外归并排序（spec 6031 / T6261 / impl 2232）——
 * Spark/数据库外部排序思想：**内存窗内排序成游程+多路归并**
 * ——每次取 min(chunkSize, 剩余) 切块排序成有序游程，再逐路
 * 取最小归并（稳定取各游程队首最小）——数据远超内存时
 * 全内存排序 OOM 的病解。runCount = ⌈n/chunkSize⌉ 显形。
 * 归并取数并列时游程序靠前（确定性——同输入同输出）。
 */
public final class ExternalMergeSort {

    private final long[] sorted;
    private final int runCount;
    private final int chunkSize;

    /** 排序（null/非法窗 fail-fast；防御性副本）。 */
    public ExternalMergeSort(long[] data, int maxInMemory) {
        if (data == null) {
            throw new IllegalArgumentException("数据非空");
        }
        if (maxInMemory <= 0) {
            throw new IllegalArgumentException("内存窗必须为正: " + maxInMemory);
        }
        this.chunkSize = maxInMemory;
        this.runCount = (data.length + maxInMemory - 1) / maxInMemory;
        long[] work = data.clone();
        long[][] runs = new long[runCount][];
        for (int r = 0; r < runCount; r++) {
            int from = r * maxInMemory;
            int to = Math.min(data.length, from + maxInMemory);
            Arrays.sort(work, from, to);
            runs[r] = Arrays.copyOfRange(work, from, to);
        }
        this.sorted = mergeRuns(runs, data.length);
    }

    /** 排序结果。 */
    public long[] sorted() {
        return sorted.clone();
    }

    /** 游程数读数（⌈n/chunkSize⌉）。 */
    public int runCount() {
        return runCount;
    }

    /** 数据量读数。 */
    public int size() {
        return sorted.length;
    }

    /** 窗读数。 */
    public int chunkSize() {
        return chunkSize;
    }

    private static long[] mergeRuns(long[][] runs, int total) {
        long[] out = new long[total];
        int[] cursor = new int[runs.length];
        for (int i = 0; i < total; i++) {
            int best = -1;
            for (int r = 0; r < runs.length; r++) {
                if (cursor[r] < runs[r].length
                        && (best == -1 || runs[r][cursor[r]] < runs[best][cursor[best]])) {
                    best = r;
                }
            }
            out[i] = runs[best][cursor[best]++];
        }
        return out;
    }
}
