package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Boyer–Moore 子串搜索（spec 8001 / V8003 / impl 2303）——
 * Boyer & Moore 1977 思想（grep/less 同源）：**失配时同时利用
 * 文本侧坏字符与模式侧好后缀两类知识，取更大者滑动**——
 * 期望 O(n/m) 次比较——KMP 只按模式自知识滑动（文本侧信息
 * 弃而不用）的病解。 findAll 返回全部（含重叠）命中；确定性
 * 纯函数；null/空模式 fail-fast。
 *
 * <p>与 KmpSearch（spec 3014）、RabinKarpSearch（spec 3025）
 * 同族不同面：失配函数自知识 vs 滚动哈希 vs 坏字符+好后缀
 * 双启发。
 */
public final class BoyerMooreSearch {

    private BoyerMooreSearch() {
    }

    /** 全部（含重叠）命中起始位升序（null 文本/模式或空模式 fail-fast）。 */
    public static List<Integer> findAll(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("文本与模式均非空引用");
        }
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("模式非空串（空模式命中语义无定义）");
        }
        int n = text.length();
        int m = pattern.length();
        List<Integer> hits = new ArrayList<>();
        if (m > n) {
            return List.copyOf(hits);
        }
        Map<Character, Integer> rightmost = buildRightmost(pattern);
        int[] goodSuffix = buildGoodSuffix(pattern);
        int i = 0;
        while (i <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(i + j)) {
                j--;
            }
            if (j < 0) {
                hits.add(i);
                i += goodSuffix[0];
            } else {
                char bad = text.charAt(i + j);
                int bcShift = j - rightmost.getOrDefault(bad, -1);
                i += Math.max(goodSuffix[j + 1], bcShift);
            }
        }
        return List.copyOf(hits);
    }

    /** 首个命中位（无命中 −1 诚实缺省）。 */
    public static int first(String text, String pattern) {
        List<Integer> hits = findAll(text, pattern);
        return hits.isEmpty() ? -1 : hits.get(0);
    }

    /** 坏字符表：模式内每字符最右出现位（缺省 −1 由调用方兜底）。 */
    private static Map<Character, Integer> buildRightmost(String pattern) {
        Map<Character, Integer> rightmost = new HashMap<>();
        for (int i = 0; i < pattern.length(); i++) {
            rightmost.put(pattern.charAt(i), i);
        }
        return rightmost;
    }

    /** 强好后缀表：shift[k] = 已匹配后缀长 k−1 时的安全滑动量（经典边界构造）。 */
    private static int[] buildGoodSuffix(String pattern) {
        int m = pattern.length();
        int[] shift = new int[m + 1];
        int[] border = new int[m + 1];
        int i = m;
        int j = m + 1;
        border[i] = j;
        while (i > 0) {
            while (j <= m && pattern.charAt(i - 1) != pattern.charAt(j - 1)) {
                if (shift[j] == 0) {
                    shift[j] = j - i;
                }
                j = border[j];
            }
            i--;
            j--;
            border[i] = j;
        }
        j = border[0];
        for (i = 0; i <= m; i++) {
            if (shift[i] == 0) {
                shift[i] = j;
            }
            if (i == j) {
                j = border[j];
            }
        }
        return shift;
    }
}
