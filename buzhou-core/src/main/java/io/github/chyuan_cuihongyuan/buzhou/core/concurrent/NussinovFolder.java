package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * RNA 二级结构 Nussinov 折叠（spec 10020 / X10041 / impl 2423）——
 * Nussinov–Jacobson 1980 思想（「区间 DP 最大碱基对」——ViennaRNA
 * 同源）：**D[i][j]=子串最大配对数四向递推（不配/单配/双分裂），
 * 回溯产出无假结（non-crossing）配对集**——二级结构预测的奠基
 * DP（能量模型之前的碱基对计数面）。最小环长约束（发夹环≥3）；
 * 非 ACGU 字母 fail-fast；同序列同结构确定。
 */
public final class NussinovFolder {

    /** 最小发夹环长（i 与 j 间至少隔 3 个未配碱基）。 */
    private static final int MIN_LOOP = 3;

    private NussinovFolder() {
    }

    /**
     * 折叠结果（maxPairs=最大配对数；pairs=0 基索引对，升序字典序）。
     */
    public record Structure(int maxPairs, List<int[]> pairs) {
    }

    /**
     * 折叠（sequence 仅含 A/C/G/U）。
     *
     * @throws IllegalArgumentException null/长度不足/非法字母
     */
    public static Structure fold(String sequence) {
        if (sequence == null || sequence.length() < MIN_LOOP + 2) {
            throw new IllegalArgumentException("序列长度 ≥" + (MIN_LOOP + 2)
                    + " 且非 null（实际 " + (sequence == null ? "null" : sequence.length()) + "）");
        }
        int n = sequence.length();
        for (int i = 0; i < n; i++) {
            char base = sequence.charAt(i);
            if (base != 'A' && base != 'C' && base != 'G' && base != 'U') {
                throw new IllegalArgumentException("字母域 {A,C,G,U}（位置 " + i + " 实际 " + base + "）");
            }
        }
        int[][] dp = new int[n][n];
        for (int span = MIN_LOOP + 1; span < n; span++) {
            for (int i = 0; i + span < n; i++) {
                int j = i + span;
                int best = Math.max(dp[i + 1][j], dp[i][j - 1]);
                int paired = dp[i + 1][j - 1] + pairScore(sequence, i, j);
                if (best < paired) {
                    best = paired;
                }
                for (int k = i + 1; k < j - 1; k++) {
                    best = Math.max(best, dp[i][k] + dp[k + 1][j]);
                }
                dp[i][j] = best;
            }
        }
        List<int[]> pairs = new ArrayList<>();
        traceback(dp, sequence, 0, n - 1, pairs);
        pairs.sort((x, y) -> x[0] != y[0] ? x[0] - y[0] : x[1] - y[1]);
        return new Structure(dp[0][n - 1], pairs);
    }

    /** 碱基对计分（Watson–Crick+GU 摆动=1，其余 0）。 */
    private static int pairScore(String sequence, int i, int j) {
        char left = sequence.charAt(i);
        char right = sequence.charAt(j);
        boolean matches = (left == 'A' && right == 'U') || (left == 'U' && right == 'A')
                || (left == 'C' && right == 'G') || (left == 'G' && right == 'C')
                || (left == 'G' && right == 'U') || (left == 'U' && right == 'G');
        return matches && j - i - 1 >= MIN_LOOP ? 1 : 0;
    }

    private static void traceback(int[][] dp, String sequence, int i, int j, List<int[]> pairs) {
        if (i >= j) {
            return;
        }
        if (dp[i][j] == dp[i + 1][j]) {
            traceback(dp, sequence, i + 1, j, pairs);
            return;
        }
        if (dp[i][j] == dp[i][j - 1]) {
            traceback(dp, sequence, i, j - 1, pairs);
            return;
        }
        if (dp[i][j] == dp[i + 1][j - 1] + pairScore(sequence, i, j) && pairScore(sequence, i, j) > 0) {
            pairs.add(new int[]{i, j});
            traceback(dp, sequence, i + 1, j - 1, pairs);
            return;
        }
        for (int k = i + 1; k < j; k++) {
            if (dp[i][j] == dp[i][k] + dp[k + 1][j]) {
                traceback(dp, sequence, i, k, pairs);
                traceback(dp, sequence, k + 1, j, pairs);
                return;
            }
        }
    }
}
