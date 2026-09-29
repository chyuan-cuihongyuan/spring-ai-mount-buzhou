package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Levenshtein 自动机（spec 8003 / V8007 / impl 2305）——
 * Schulz & Mihov 2002 思想（Lucene 模糊查询同源）：**模式
 * 固定而候选海量时，按模式构建 k-容差自动机、候选逐字符
 * 只推进活性状态行**——位置 0..m 的编辑数向量三源取小
 * （上+1 删除/左+1 插入/左上+cost 替换），终态值 ≤ maxEdits
 * 即接受；行最小值已越界时后续只增不减——早停诚实缺省
 * false。与全矩阵逐词重算（每候选 O(m·n) 全量）的病解划界。
 * null/空模式/负 k fail-fast；确定性纯函数。
 *
 * <p>与 DamerauLevenshtein（spec 7022）同族不同面：四算子
 * 全矩阵距离值 vs 模式侧 k-容差接受判定（流式推进）。
 */
public final class LevenshteinAutomaton {

    private final String pattern;
    private final int maxEdits;

    private LevenshteinAutomaton(String pattern, int maxEdits) {
        this.pattern = pattern;
        this.maxEdits = maxEdits;
    }

    /** 构建（null/空模式或负 k fail-fast）。 */
    public static LevenshteinAutomaton of(String pattern, int maxEdits) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("模式非空引用且非空串");
        }
        if (maxEdits < 0) {
            throw new IllegalArgumentException("容差 k 非负（实际 " + maxEdits + "）");
        }
        return new LevenshteinAutomaton(pattern, maxEdits);
    }

    /** 候选与模式编辑数 ≤ k（null 候选 fail-fast）。 */
    public boolean matches(String word) {
        return distanceTo(word) <= maxEdits;
    }

    /** 候选到模式的完整编辑距离（活性状态行推进——自动机语义的最小接受值）。 */
    public int distanceTo(String word) {
        if (word == null) {
            throw new IllegalArgumentException("候选非空引用");
        }
        int m = pattern.length();
        int n = word.length();
        int[] row = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            row[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            int diagonal = row[0];
            row[0] = i;
            int rowMin = row[0];
            char c = word.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int up = row[j];
                int cost = pattern.charAt(j - 1) == c ? 0 : 1;
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), diagonal + cost);
                diagonal = up;
                rowMin = Math.min(rowMin, row[j]);
            }
            if (rowMin > maxEdits) {
                return Integer.MAX_VALUE;
            }
        }
        return row[m];
    }
}
