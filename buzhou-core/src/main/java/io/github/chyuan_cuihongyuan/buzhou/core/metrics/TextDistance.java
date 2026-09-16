package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 文本编辑距离（spec 2052 / T3205 / impl 1603）——Levenshtein 经典 DP
 * 思想：两串最小单字符编辑（插/删/改）数——文本差异的精确口径
 *（SimHash 是近似口径，二者互补）；相似比 1−dist/maxLen 归一 ∈[0,1]
 * 跨长短文本可比；近匹配阈值判定给「差多少算像」统一答案。
 *
 * <p>纯函数零状态；两行 DP 滚动数组（O(min(m,n)) 空间）；a/b 任一
 * null fail-fast（null 语义归调用方）。
 */
public final class TextDistance {

    /** 默认近匹配相似比阈值（0.8——config 键纠错同款口径）。 */
    public static final double DEFAULT_NEAR_MATCH_RATIO = 0.8d;

    private TextDistance() {
    }

    /** Levenshtein 距离（插/删/改各计 1）。契约：a/b 非 null。 */
    public static int levenshtein(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("a/b 不能为 null");
        }
        if (a.isEmpty() || b.isEmpty()) {
            return a.length() + b.length(); // 一方空——全插/删
        }
        // 短串做列——空间 O(min)
        String shorter = a.length() <= b.length() ? a : b;
        String longer = shorter == a ? b : a;
        int[] prev = new int[shorter.length() + 1];
        int[] curr = new int[shorter.length() + 1];
        for (int j = 0; j <= shorter.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= longer.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= shorter.length(); j++) {
                int cost = longer.charAt(i - 1) == shorter.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[shorter.length()];
    }

    /** 相似比 ∈ [0,1]：1 − dist/max(len)——1 全同、0 完全不同（空双串=1）。 */
    public static double similarity(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("a/b 不能为 null");
        }
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0d; // 双空全同
        }
        return 1.0d - (double) levenshtein(a, b) / maxLen;
    }

    /** 近匹配判定：相似比 ≥ threshold。契约：threshold ∈ [0,1]。 */
    public static boolean isNearMatch(String a, String b, double threshold) {
        if (!(threshold >= 0) || threshold > 1 || Double.isNaN(threshold)) {
            throw new IllegalArgumentException("threshold 须在 [0,1]：" + threshold);
        }
        return similarity(a, b) >= threshold;
    }
}
