package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * KMP 字符串搜索（spec 3014 / T5029 / impl 2015）——Knuth-Morris-
 * Pratt 失配函数思想：模式预构**最长真前后缀表**（lps），主扫描
 * 失配时模式滑而不回退主指针——O(n+m) 免朴素 O(n·m) 回退（守卫
 * 规则/敏感词/停用串多模式匹配的单模式地基；多模式聚合归
 * Aho-Corasick 留白）。findAll **可重叠**（失败回退到 lps 前缀
 * 继续找——「aa」在「aaaa」命中 0/1/2）。
 *
 * <p>纯函数静态件；空模式：indexOf 约定 0（JDK 同款）、findAll
 * fail-fast（无穷匹配无意义——诚实拒绝）。
 */
public final class KmpSearch {

    private KmpSearch() {
    }

    /** 首次匹配下标（无匹配 −1；空模式 0——JDK indexOf 同约定）。 */
    public static int indexOf(String text, String pattern) {
        requireNonNull(text, pattern);
        if (pattern.isEmpty()) {
            return 0;
        }
        int[] lps = failureFunction(pattern);
        int match = 0;
        for (int i = 0; i < text.length(); i++) {
            while (match > 0 && text.charAt(i) != pattern.charAt(match)) {
                match = lps[match - 1];
            }
            if (text.charAt(i) == pattern.charAt(match)) {
                match++;
            }
            if (match == pattern.length()) {
                return i - match + 1;
            }
        }
        return -1;
    }

    /** 全部匹配起点（可重叠；空模式 fail-fast）。 */
    public static List<Integer> findAll(String text, String pattern) {
        requireNonNull(text, pattern);
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("findAll 空模式无穷匹配——拒绝");
        }
        int[] lps = failureFunction(pattern);
        List<Integer> hits = new ArrayList<>();
        int match = 0;
        for (int i = 0; i < text.length(); i++) {
            while (match > 0 && text.charAt(i) != pattern.charAt(match)) {
                match = lps[match - 1];
            }
            if (text.charAt(i) == pattern.charAt(match)) {
                match++;
            }
            if (match == pattern.length()) {
                hits.add(i - match + 1);
                match = lps[match - 1];
            }
        }
        return List.copyOf(hits);
    }

    /**
     * 失配函数（lps）：lps[i] = pattern[0..i] 最长真前缀==真后缀的
     * 长度——失配滑窗跳到该前缀继续（公共读数/对账面）。
     */
    public static int[] failureFunction(String pattern) {
        requireNonNull(pattern, pattern);
        int[] lps = new int[pattern.length()];
        int length = 0;
        for (int i = 1; i < pattern.length(); i++) {
            while (length > 0 && pattern.charAt(i) != pattern.charAt(length)) {
                length = lps[length - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(length)) {
                length++;
            }
            lps[i] = length;
        }
        return lps;
    }

    private static void requireNonNull(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("text/pattern 非空");
        }
    }
}
