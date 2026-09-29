package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;

/**
 * Glob 通配符匹配（spec 8004 / V8009 / impl 2306）——
 * POSIX fnmatch/bash glob 思想：**四类原语（`*` 任意含空/
 * `?` 恰一字符/`[...]` 字符类/字面量）前端解析为 token、
 * 星号单候选位回溯线性匹配**（最坏 O(nm) 有界——朴素递归
 * 回溯在最坏路径上指数的病解）。字符类支持 `a-z` 区间与
 * `!` 否定、首 `]` 为字面量（POSIX 约定）；`*` 无跨段特例
 * （POSIX glob 无 `**`——明示）；未闭合 `[`/null fail-fast；
 * 确定性纯函数。
 *
 * <p>与正则面划界：glob 是配置面事实标准（.gitignore/ant
 * 路径/Shell），语义窄而可预期。
 */
public final class GlobMatcher {

    private GlobMatcher() {
    }

    /** glob 全匹配语义（null 模式/文本或未闭合 [ fail-fast）。 */
    public static boolean matches(String pattern, String text) {
        if (pattern == null || text == null) {
            throw new IllegalArgumentException("模式与文本均非空引用");
        }
        List<Object> tokens = parse(pattern);
        int n = text.length();
        int ti = 0;
        int si = 0;
        int starIndex = -1;
        int starMark = 0;
        int tokenCount = tokens.size();
        while (si < n) {
            if (ti < tokenCount && !(tokens.get(ti) instanceof Star)
                    && tokenMatches(tokens.get(ti), text.charAt(si))) {
                ti++;
                si++;
            } else if (ti < tokenCount && tokens.get(ti) instanceof Star) {
                starIndex = ti++;
                starMark = si;
            } else if (starIndex >= 0) {
                ti = starIndex + 1;
                si = ++starMark;
            } else {
                return false;
            }
        }
        while (ti < tokenCount && tokens.get(ti) instanceof Star) {
            ti++;
        }
        return ti == tokenCount;
    }

    private static List<Object> parse(String pattern) {
        java.util.ArrayList<Object> tokens = new java.util.ArrayList<>();
        int i = 0;
        int m = pattern.length();
        while (i < m) {
            char c = pattern.charAt(i);
            if (c == '*') {
                tokens.add(new Star());
                i++;
            } else if (c == '?') {
                tokens.add(new Any());
                i++;
            } else if (c == '[') {
                i = parseClass(pattern, i, tokens);
            } else {
                tokens.add(new Literal(c));
                i++;
            }
        }
        return List.copyOf(tokens);
    }

    /** 解析字符类 token（返回类闭合 ] 之后的位置；未闭合 fail-fast）。 */
    private static int parseClass(String pattern, int start, java.util.List<Object> tokens) {
        int i = start + 1;
        int m = pattern.length();
        boolean negate = false;
        if (i < m && (pattern.charAt(i) == '!' || pattern.charAt(i) == '^')) {
            negate = true;
            i++;
        }
        java.util.ArrayList<int[]> ranges = new java.util.ArrayList<>();
        boolean first = true;
        while (i < m && (first || pattern.charAt(i) != ']')) {
            first = false;
            char lo = pattern.charAt(i);
            if (i + 2 < m && pattern.charAt(i + 1) == '-' && pattern.charAt(i + 2) != ']') {
                char hi = pattern.charAt(i + 2);
                if (lo > hi) {
                    throw new IllegalArgumentException("字符类区间倒置 " + lo + "-" + hi);
                }
                ranges.add(new int[]{lo, hi});
                i += 3;
            } else {
                ranges.add(new int[]{lo, lo});
                i++;
            }
        }
        if (i >= m) {
            throw new IllegalArgumentException("字符类未闭合（位置 " + start + "）");
        }
        tokens.add(new CharClass(List.copyOf(ranges), negate));
        return i + 1;
    }

    private static boolean tokenMatches(Object token, char c) {
        if (token instanceof Literal literal) {
            return literal.value() == c;
        }
        if (token instanceof Any) {
            return true;
        }
        CharClass klass = (CharClass) token;
        boolean in = false;
        for (int[] range : klass.ranges()) {
            if (c >= range[0] && c <= range[1]) {
                in = true;
                break;
            }
        }
        return in != klass.negate();
    }

    private sealed interface Token {
    }

    private record Literal(char value) implements Token {
    }

    private record Any() implements Token {
    }

    private record Star() implements Token {
    }

    private record CharClass(List<int[]> ranges, boolean negate) implements Token {
    }
}
