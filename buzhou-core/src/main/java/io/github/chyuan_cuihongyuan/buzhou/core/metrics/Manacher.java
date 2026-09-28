package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Manacher 最长回文（spec 7015 / U7231 / impl 2267）——
 * Manacher 1975 线性回文思想：**插入分隔符统一奇偶 +
 * 回文镜像复用右界**（p[i] ≥ min(p[mirror], right−i)
 * 起步扩展）O(n)——逐中心扩展 O(n²)（长串放大）的病解。
 * 最长回文子串并列取起点最小者（canonical——同串同结果
 * 完全确定）。分隔符 '#' 与边界哨兵：输入含 '#' fail-fast
 * （变换唯一性前提，明示拒绝而非静默错配）。空串 fail-fast
 * （无回文面）。
 *
 * <p>与 KmpSearch/ZArray（同包）同族不同面：前缀函数/匹配
 * 段 vs 回文半径镜像复用。
 */
public final class Manacher {

    private static final char SEPARATOR = '#';

    private Manacher() {
    }

    /** 最长回文子串（并列取起点最小；null/空串/含 # fail-fast）。 */
    public static String longestPalindrome(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("串非空");
        }
        if (s.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("输入不得含分隔符 #");
        }
        int n = s.length();
        if (n == 1) {
            return s;
        }
        char[] t = new char[2 * n + 1];
        for (int i = 0; i < n; i++) {
            t[2 * i] = SEPARATOR;
            t[2 * i + 1] = s.charAt(i);
        }
        t[2 * n] = SEPARATOR;
        int m = t.length;
        int[] p = new int[m];
        int center = 0;
        int right = 0;
        int bestCenter = 0;
        for (int i = 0; i < m; i++) {
            if (i < right) {
                p[i] = Math.min(right - i, p[2 * center - i]);
            }
            while (i - p[i] - 1 >= 0 && i + p[i] + 1 < m
                    && t[i - p[i] - 1] == t[i + p[i] + 1]) {
                p[i]++;
            }
            if (i + p[i] > right) {
                center = i;
                right = i + p[i];
            }
            if (p[i] > p[bestCenter]) {
                bestCenter = i;
            }
        }
        int radius = p[bestCenter];
        int start = (bestCenter - radius) / 2;
        return s.substring(start, start + radius);
    }
}
