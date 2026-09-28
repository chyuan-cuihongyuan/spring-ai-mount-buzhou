package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Patience LIS（spec 7025 / U7251 / impl 2277）——耐心排序
 * 思想（Patience game/Best 1977）：**牌堆顶可放即放、否则
 * 新开一堆，堆数=最长递增子序列长**——二分找堆 O(n log n)
 * ——DP O(n²)（长序列放大）的病解。严格递增语义（相等
 * 不续——明示）；返回长度+一个 LIS 实例（topIndex 父链
 * 回溯，canonical 确定）。long 域。
 *
 * <p>与 MonotonicDeque（同包）同族不同面：滑窗最值维护 vs
 * 全序列 LIS 计数。
 */
public final class PatienceLis {

    private PatienceLis() {
    }

    /** LIS 结果（长度 + 一个实例）。 */
    public static final class Result {
        final int length;
        final long[] sequence;

        Result(int length, long[] sequence) {
            this.length = length;
            this.sequence = sequence;
        }

        public int length() {
            return length;
        }

        public long[] sequence() {
            return sequence;
        }
    }

    /** 最长严格递增子序列（O(n log n)；null fail-fast——空列长 0 合法）。 */
    public static Result longestIncreasingSubsequence(long[] values) {
        if (values == null) {
            throw new IllegalArgumentException("序列非空引用");
        }
        if (values.length == 0) {
            return new Result(0, new long[0]);
        }
        long[] pileTops = new long[values.length];
        int[] topIndexOf = new int[values.length];
        int[] parent = new int[values.length];
        int[] pileIndexOf = new int[values.length];
        int piles = 0;
        for (int i = 0; i < values.length; i++) {
            long value = values[i];
            int low = 0;
            int high = piles;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (pileTops[mid] < value) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            pileTops[low] = value;
            pileIndexOf[i] = low;
            parent[i] = low == 0 ? -1 : topIndexOf[low - 1];
            topIndexOf[low] = i;
            if (low == piles) {
                piles++;
            }
        }
        long[] sequence = new long[piles];
        int index = topIndexOf[piles - 1];
        for (int k = piles - 1; k >= 0; k--) {
            sequence[k] = values[index];
            index = parent[index];
        }
        return new Result(piles, sequence);
    }

    /** 长度读数便捷面。 */
    public static int length(long[] values) {
        return longestIncreasingSubsequence(values).length();
    }
}
