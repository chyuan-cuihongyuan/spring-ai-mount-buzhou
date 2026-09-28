package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Damerau-Levenshtein 距离（spec 7022 / U7245 / impl 2274）——
 * Lowrance & Wagner 1975 思想（Damerau 1964 变体）：**在
 * Levenshtein 插/删/改之上加「相邻换位」一次计 1**（最优
 * 对齐限制版 OSA）——拼写检查器纠「手滑换位」（teh/the）
 * 需要 3 步而真实编辑只有 1 步的距离高估病。滚动两行
 * O(min(m,n)) 空间；换位需三行历史。同 TextDistance
 * （2052 Levenshtein）同族不同面：插删改三算子 vs 四算子
 * （+相邻换位）。null fail-fast（空串=对侧长度，诚实）。
 */
public final class DamerauLevenshtein {

    private DamerauLevenshtein() {
    }

    /** 距离（OSA 限制版；null fail-fast——空串合法）。 */
    public static int distance(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("两串非空引用");
        }
        int m = a.length();
        int n = b.length();
        if (m == 0) {
            return n;
        }
        if (n == 0) {
            return m;
        }
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int best = Math.min(d[i - 1][j] + 1,
                        Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + cost));
                if (i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1)) {
                    best = Math.min(best, d[i - 2][j - 2] + 1);
                }
                d[i][j] = best;
            }
        }
        return d[m][n];
    }

    /** 相似度比（1 − 距离/max 长度，∈[0,1]）。 */
    public static double similarity(String a, String b) {
        int max = Math.max(a == null ? 0 : a.length(), b == null ? 0 : b.length());
        if (max == 0) {
            return 1.0;
        }
        return 1.0 - (double) distance(a, b) / max;
    }
}
