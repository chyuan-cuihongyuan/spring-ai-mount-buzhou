package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Jaro-Winkler 相似度（spec 8034 / V8069 / impl 2336）——
 * Jaro 1989/Winkler 1990 思想（美普查局记录链接标准）：
 * **匹配窗 ⌊max/2⌋−1 内的字符匹配 + 换位数/2 比率 + 公共
 * 前缀 ≤4 加成**——编辑距离对换位不敏感（人名链接误配）
 * 的病解。similarity ∈[0,1]；null fail-fast；对称性
 * （Jaro 对称、Winkler 前缀加成亦对称——前缀公共）；确定
 * 性纯函数。
 *
 * <p>与 DamerauLevenshtein（spec 7022）同族不同面：距离
 * 计数 vs 归一相似度+前缀加成。
 */
public final class JaroWinklerSimilarity {

    /** Winkler 前缀加成长度上限（经典 4）。 */
    private static final int PREFIX_CAP = 4;

    private JaroWinklerSimilarity() {
    }

    /** Jaro-Winkler 相似度 ∈[0,1]（null fail-fast）。 */
    public static double similarity(String first, String second) {
        return jaroWinkler(first, second, 0.1);
    }

    /** 可配前缀加成系数（0=纯 Jaro）。 */
    public static double jaroWinkler(String first, String second, double prefixScale) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("比较串非空引用");
        }
        double jaro = jaro(first, second);
        int prefix = 0;
        int limit = Math.min(PREFIX_CAP, Math.min(first.length(), second.length()));
        while (prefix < limit && first.charAt(prefix) == second.charAt(prefix)) {
            prefix++;
        }
        return jaro + prefix * prefixScale * (1 - jaro);
    }

    /** Jaro 相似度 ∈[0,1]。 */
    public static double jaro(String first, String second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("比较串非空引用");
        }
        if (first.isEmpty() && second.isEmpty()) {
            return 1;
        }
        if (first.isEmpty() || second.isEmpty()) {
            return 0;
        }
        int window = Math.max(first.length(), second.length()) / 2 - 1;
        if (window < 0) {
            window = 0;
        }
        boolean[] firstMatched = new boolean[first.length()];
        boolean[] secondMatched = new boolean[second.length()];
        int matches = 0;
        for (int i = 0; i < first.length(); i++) {
            int from = Math.max(0, i - window);
            int to = Math.min(second.length() - 1, i + window);
            for (int j = from; j <= to; j++) {
                if (!secondMatched[j] && first.charAt(i) == second.charAt(j)) {
                    firstMatched[i] = true;
                    secondMatched[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0;
        }
        int transpositions = 0;
        int cursor = 0;
        for (int i = 0; i < first.length(); i++) {
            if (!firstMatched[i]) {
                continue;
            }
            while (!secondMatched[cursor]) {
                cursor++;
            }
            if (first.charAt(i) != second.charAt(cursor)) {
                transpositions++;
            }
            cursor++;
        }
        double m = matches;
        double t = transpositions / 2.0;
        return (m / first.length() + m / second.length() + (m - t) / m) / 3;
    }
}
