package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LSM 键序合并（spec 7028 / U7257 / impl 2280）——LevelDB/
 * RocksDB 多路归并思想：**k 条有序游程按 (key,runIndex)
 * 归并，新游程（小下标）覆盖旧值，TOMBSTONE 掩埋全部旧值
 * 且不输出**——逐条全表覆盖写（写放大）与只挑最新不清理
 * （读放大）两种病的归并解。游程必须按键升序（乱序
 * fail-fast——LSM 游程不变量）；键同游程内去重语义不承诺
 * （按序后写覆盖前写——取后者）。完全确定（同游程同输出）。
 *
 * <p>与 ExternalMergeSort（6031）同族不同面：排序管道 vs
 * 新旧覆盖+墓碑清理；与 LeveledCompaction（5031）不同面：
 * 挑选哪层合并 vs 合并本身语义。
 */
public final class SortedRunMerge {

    /** 墓碑值（删除标记——掩埋旧游程同键值且自身不输出）。 */
    public static final long TOMBSTONE = Long.MIN_VALUE;

    private SortedRunMerge() {
    }

    /** k 路归并（runs[0] 最新；乱序/空游程/null fail-fast）。 */
    public static List<long[]> merge(List<List<long[]>> runs) {
        if (runs == null || runs.isEmpty()) {
            throw new IllegalArgumentException("游程非空");
        }
        for (List<long[]> run : runs) {
            if (run == null || run.isEmpty()) {
                throw new IllegalArgumentException("游程须为非空有序列");
            }
            for (int i = 0; i < run.size(); i++) {
                long[] entry = run.get(i);
                if (entry == null || entry.length != 2) {
                    throw new IllegalArgumentException("条目须为 [key,value]");
                }
                if (i > 0 && entry[0] < run.get(i - 1)[0]) {
                    throw new IllegalArgumentException("游程乱序（须按键升序）");
                }
            }
        }
        int[] cursors = new int[runs.size()];
        List<long[]> output = new ArrayList<>();
        int total = 0;
        for (List<long[]> run : runs) {
            total += run.size();
        }
        long lastKey = 0;
        boolean hasLast = false;
        while (total > 0) {
            long minKey = Long.MAX_VALUE;
            for (int r = 0; r < runs.size(); r++) {
                if (cursors[r] < runs.get(r).size()) {
                    minKey = Math.min(minKey, runs.get(r).get(cursors[r])[0]);
                }
            }
            long winnerRun = -1;
            long winnerValue = 0;
            for (int r = 0; r < runs.size(); r++) {
                List<long[]> run = runs.get(r);
                if (cursors[r] < run.size() && run.get(cursors[r])[0] == minKey) {
                    long value = run.get(cursors[r])[1];
                    cursors[r]++;
                    int consumed = 1;
                    while (cursors[r] < run.size() && run.get(cursors[r])[0] == minKey) {
                        value = run.get(cursors[r])[1];
                        cursors[r]++;
                        consumed++;
                    }
                    if (winnerRun == -1) {
                        winnerRun = r;
                        winnerValue = value;
                    }
                    total -= consumed;
                }
            }
            boolean tombstoned = winnerValue == TOMBSTONE;
            if (!tombstoned && (!hasLast || minKey != lastKey)) {
                output.add(new long[]{minKey, winnerValue});
            }
            lastKey = minKey;
            hasLast = true;
        }
        return output;
    }

    /** 便捷构造游程。 */
    public static List<long[]> run(long[]... pairs) {
        List<long[]> entries = new ArrayList<>();
        for (long[] pair : pairs) {
            entries.add(Arrays.copyOf(pair, 2));
        }
        return entries;
    }
}
