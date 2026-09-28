package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Z 数组（spec 7001 / U7203 / impl 2253）——Z 算法线性前缀面
 * （Gusfield 思想，竞争编程/字符串索引同源）：z[i] = 后缀
 * s[i:] 与整串的最长公共前缀长，**Z-box [l,r) 匹配段复用**
 * 摊还 O(n)——逐位置暴力 LCP O(n²)（长串放大）的病解。z[0]=n
 * （整串自比约定）。模式匹配经 p+SEP+t 拼接一次 z 扫描得全部
 * （重叠）命中——与 KmpSearch（同包）同族不同面：失败函数
 * 边界回归 vs Z-box 区间复用。SEP 用 NUL：任一侧含 NUL
 * fail-fast（分隔符唯一性前提）。
 */
public final class ZArray {

    private static final char SEPARATOR = '\u0000';

    private ZArray() {
    }

    /** Z 函数（z[0]=n；null/空串 fail-fast——空串无后缀面）。 */
    public static int[] zFunction(String s) {
        if (s == null) {
            throw new IllegalArgumentException("串非空");
        }
        int n = s.length();
        if (n == 0) {
            throw new IllegalArgumentException("空串无后缀面");
        }
        int[] z = new int[n];
        z[0] = n;
        int left = 0;
        int right = 0;
        for (int i = 1; i < n; i++) {
            if (i < right) {
                z[i] = Math.min(right - i, z[i - left]);
            }
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                z[i]++;
            }
            if (i + z[i] > right) {
                left = i;
                right = i + z[i];
            }
        }
        return z;
    }

    /** 模式在文本中的全部（重叠）起始位（升序）。 */
    public static List<Integer> findAll(String text, String pattern) {
        requireSearchable(text, pattern);
        String joined = pattern + SEPARATOR + text;
        int[] z = zFunction(joined);
        int m = pattern.length();
        List<Integer> hits = new ArrayList<>();
        for (int i = m + 1; i < joined.length(); i++) {
            if (z[i] >= m) {
                hits.add(i - m - 1);
            }
        }
        return hits;
    }

    /** 命中数读数。 */
    public static int count(String text, String pattern) {
        return findAll(text, pattern).size();
    }

    private static void requireSearchable(String text, String pattern) {
        if (text == null || pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("文本/模式非空（模式不得空串）");
        }
        if (text.indexOf(SEPARATOR) >= 0 || pattern.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("文本/模式不得含 NUL 分隔符");
        }
    }
}
