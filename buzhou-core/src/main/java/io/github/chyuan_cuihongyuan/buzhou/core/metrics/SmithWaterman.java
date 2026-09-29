package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Smith-Waterman 局部比对（spec 8044 / V8087 / impl 2345）——
 * Smith & Waterman 1981 思想：**DP 单元负值归零 + 从最大格
 * 回溯至 0**——最优局部比对子串对（全负得 0 空比对诚实
 * 缺省）——全局比对强设端到端（嵌噪声局部相似不可见）的
 * 病解。null fail-fast；确定性纯函数。
 *
 * <p>与 NeedlemanWunsch（spec 8043）同族不同面：局部最优
 * 子串 vs 端到端全局。
 */
public final class SmithWaterman {

    private SmithWaterman() {
    }

    /** 局部比对结果：bestScore + 两行比对子串（全负时空串）。 */
    public record Alignment(int bestScore, String first, String second) {
    }

    /** 局部比对（null fail-fast）。 */
    public static Alignment align(String first, String second, int match, int mismatch, int gap) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("比对串非空引用");
        }
        int n = first.length();
        int m = second.length();
        int[][] scores = new int[n + 1][m + 1];
        int best = 0;
        int bestI = 0;
        int bestJ = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int diagonal = scores[i - 1][j - 1]
                        + (first.charAt(i - 1) == second.charAt(j - 1) ? match : mismatch);
                int up = scores[i - 1][j] + gap;
                int left = scores[i][j - 1] + gap;
                int value = Math.max(0, Math.max(diagonal, Math.max(up, left)));
                scores[i][j] = value;
                if (value > best) {
                    best = value;
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        StringBuilder firstLine = new StringBuilder();
        StringBuilder secondLine = new StringBuilder();
        int i = bestI;
        int j = bestJ;
        while (i > 0 && j > 0 && scores[i][j] > 0) {
            int diagonal = scores[i - 1][j - 1]
                    + (first.charAt(i - 1) == second.charAt(j - 1) ? match : mismatch);
            if (scores[i][j] == diagonal) {
                firstLine.append(first.charAt(i - 1));
                secondLine.append(second.charAt(j - 1));
                i--;
                j--;
            } else if (scores[i][j] == scores[i - 1][j] + gap) {
                firstLine.append(first.charAt(i - 1));
                secondLine.append('-');
                i--;
            } else {
                firstLine.append('-');
                secondLine.append(second.charAt(j - 1));
                j--;
            }
        }
        return new Alignment(best, firstLine.reverse().toString(), secondLine.reverse().toString());
    }
}
