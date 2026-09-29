package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Needleman-Wunsch 全局比对（spec 8043 / V8073 / impl 2344）——
 * Needleman & Wunsch 1970 思想：**全矩阵 DP + 回溯**——
 * match/mismatch/gap 三参打分的最优全局比对得分与比对串对
 * （回溯并列取上——canonical）——全比对路径枚举指数（序列
 * 放大）的病解。空对空合法 0；null fail-fast；确定性纯函数。
 *
 * <p>与 SmithWaterman（spec 8044）同族不同面：端到端全局
 * vs 局部最优子串。
 */
public final class NeedlemanWunsch {

    private NeedlemanWunsch() {
    }

    /** 比对结果：score + 两行比对串（'-' 表间隙；null fail-fast）。 */
    public record Alignment(int score, String first, String second) {
    }

    /** 全局比对（null fail-fast）。 */
    public static Alignment align(String first, String second, int match, int mismatch, int gap) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("比对串非空引用");
        }
        int n = first.length();
        int m = second.length();
        int[][] scores = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            scores[i][0] = i * gap;
        }
        for (int j = 1; j <= m; j++) {
            scores[0][j] = j * gap;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int diagonal = scores[i - 1][j - 1]
                        + (first.charAt(i - 1) == second.charAt(j - 1) ? match : mismatch);
                scores[i][j] = Math.max(diagonal, Math.max(scores[i - 1][j] + gap, scores[i][j - 1] + gap));
            }
        }
        StringBuilder firstLine = new StringBuilder();
        StringBuilder secondLine = new StringBuilder();
        int i = n;
        int j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && scores[i][j] == scores[i - 1][j - 1]
                    + (first.charAt(i - 1) == second.charAt(j - 1) ? match : mismatch)) {
                firstLine.append(first.charAt(i - 1));
                secondLine.append(second.charAt(j - 1));
                i--;
                j--;
            } else if (i > 0 && scores[i][j] == scores[i - 1][j] + gap) {
                firstLine.append(first.charAt(i - 1));
                secondLine.append('-');
                i--;
            } else {
                firstLine.append('-');
                secondLine.append(second.charAt(j - 1));
                j--;
            }
        }
        return new Alignment(scores[n][m],
                firstLine.reverse().toString(), secondLine.reverse().toString());
    }
}
