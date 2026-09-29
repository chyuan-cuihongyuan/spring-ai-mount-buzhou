package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * Gotoh 仿射间隙对齐（spec 8045 / V8089 / impl 2346）——
 * Gotoh 1982 思想：**M/A/B 三矩阵分离间隙开口与延伸**——
 * 连续缺口一次事件罚（gapOpen+gapExtend·长度）而非线性按位
 * 罚——生物连续缺口一次事件被重复罚的病解。
 * gapOpen=gapExtend 退化为线性间隙（与 NW 圣像全等）；参数
 * fail-fast；确定性纯函数。
 *
 * <p>与 NeedlemanWunsch（spec 8043）同族不同面：仿射间隙
 * vs 线性间隙。
 */
public final class GotohAlignment {

    private GotohAlignment() {
    }

    /** 仿射间隙全局比对得分（null/参数越域 fail-fast）。 */
    public static int score(String first, String second, int match, int mismatch,
                            int gapOpen, int gapExtend) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("比对串非空引用");
        }
        if (gapOpen > 0 || gapExtend > 0) {
            throw new IllegalArgumentException("间隙罚非正（open=" + gapOpen
                    + " extend=" + gapExtend + "）");
        }
        int n = first.length();
        int m = second.length();
        final int NEG = Integer.MIN_VALUE / 4;
        double[] dummy = null;
        int[][] mMatrix = new int[n + 1][m + 1];
        int[][] aMatrix = new int[n + 1][m + 1];
        int[][] bMatrix = new int[n + 1][m + 1];
        for (int[] row : mMatrix) {
            ArraysFill.fill(row, NEG);
        }
        for (int[] row : aMatrix) {
            ArraysFill.fill(row, NEG);
        }
        for (int[] row : bMatrix) {
            ArraysFill.fill(row, NEG);
        }
        mMatrix[0][0] = 0;
        for (int i = 1; i <= n; i++) {
            aMatrix[i][0] = gapOpen + (i - 1) * gapExtend;
        }
        for (int j = 1; j <= m; j++) {
            bMatrix[0][j] = gapOpen + (j - 1) * gapExtend;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int diagonal = Math.max(mMatrix[i - 1][j - 1],
                        Math.max(aMatrix[i - 1][j - 1], bMatrix[i - 1][j - 1]));
                mMatrix[i][j] = diagonal
                        + (first.charAt(i - 1) == second.charAt(j - 1) ? match : mismatch);
                aMatrix[i][j] = Math.max(mMatrix[i - 1][j] + gapOpen + gapExtend,
                        Math.max(aMatrix[i - 1][j] + gapExtend, bMatrix[i - 1][j] + gapOpen + gapExtend));
                bMatrix[i][j] = Math.max(mMatrix[i][j - 1] + gapOpen + gapExtend,
                        Math.max(bMatrix[i][j - 1] + gapExtend, aMatrix[i][j - 1] + gapOpen + gapExtend));
            }
        }
        return Math.max(mMatrix[n][m], Math.max(aMatrix[n][m], bMatrix[n][m]));
    }

    private static final class ArraysFill {
        private static void fill(int[] row, int value) {
            java.util.Arrays.fill(row, value);
        }
    }
}
