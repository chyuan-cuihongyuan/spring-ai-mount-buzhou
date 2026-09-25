package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * Suffix Array 后缀数组（spec 6009 / T6219 / impl 2210）——
 * Manber-Myer 倍增构造 + Kasai LCP 思想（Lucene/生信索引
 * 同源）：静态文本一次建索引，任意子串查询二分后缀序
 * （O(m log n)）——每查询对全部后缀 startsWith 线性扫
 * （O(n·m) 每查放大）的病解。重叠出现全计；suffixArray/
 * lcpArray 防御性副本（结构可独立复算审计）。
 *
 * <p>与 AhoCorasick（spec 6006）同族不同面：模式集索引文本
 * vs 文本索引查询集；与 KmpSearch（Q 系）不同面：单模式
 * 流扫 vs 静态多查询。静态定构（构建后只读——确定性）。
 */
public final class SuffixArray {

    private final String text;
    private final int[] sa;
    private final int[] lcp;

    /** 构建即建索引（null/空文本 fail-fast）。 */
    public SuffixArray(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("文本非空");
        }
        this.text = text;
        this.sa = buildSuffixArray();
        this.lcp = buildLcp();
    }

    /** 是否包含子串（null/空查询 fail-fast）。 */
    public boolean contains(String query) {
        String q = requireQuery(query);
        int idx = lowerBound(q);
        return idx < sa.length && compareWithQuery(sa[idx], q) == 0;
    }

    /** 出现次数（重叠全计；null/空查询 fail-fast）。 */
    public int occurrenceCount(String query) {
        String q = requireQuery(query);
        int low = lowerBound(q);
        int high = upperBound(q);
        return high - low;
    }

    /** 后缀数组副本（sa[i]=第 i 小后缀起点）。 */
    public int[] suffixArray() {
        return sa.clone();
    }

    /** LCP 数组副本（lcp[i]=sa[i−1] 与 sa[i] 后缀最长公共前缀；lcp[0]=0）。 */
    public int[] lcpArray() {
        return lcp.clone();
    }

    /** 文本长度读数。 */
    public int length() {
        return text.length();
    }

    private int[] buildSuffixArray() {
        int n = text.length();
        Integer[] order = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
            rank[i] = text.charAt(i);
        }
        int[] sorted = new int[n];
        int[] nextRank = new int[n];
        for (int k = 0; k < n; k = k == 0 ? 1 : k * 2) {
            int step = k;
            Arrays.sort(order, (x, y) -> {
                if (rank[x] != rank[y]) {
                    return Integer.compare(rank[x], rank[y]);
                }
                int rx = x + step < n ? rank[x + step] : -1;
                int ry = y + step < n ? rank[y + step] : -1;
                return Integer.compare(rx, ry);
            });
            sorted[order[0]] = 0;
            for (int i = 1; i < n; i++) {
                int prev = order[i - 1];
                int cur = order[i];
                boolean samePair = rank[prev] == rank[cur]
                        && ((prev + step < n ? rank[prev + step] : -1)
                            == (cur + step < n ? rank[cur + step] : -1));
                sorted[cur] = sorted[prev] + (samePair ? 0 : 1);
            }
            System.arraycopy(sorted, 0, rank, 0, n);
            if (rank[order[n - 1]] == n - 1) {
                break;
            }
        }
        for (int i = 0; i < n; i++) {
            sorted[i] = order[i];
        }
        return sorted;
    }

    private int[] buildLcp() {
        int n = text.length();
        int[] inv = new int[n];
        for (int i = 0; i < n; i++) {
            inv[sa[i]] = i;
        }
        int[] result = new int[n];
        int h = 0;
        for (int i = 0; i < n; i++) {
            int rankOfI = inv[i];
            if (rankOfI > 0) {
                int j = sa[rankOfI - 1];
                while (i + h < n && j + h < n && text.charAt(i + h) == text.charAt(j + h)) {
                    h++;
                }
                result[rankOfI] = h;
                if (h > 0) {
                    h--;
                }
            } else {
                h = 0;
            }
        }
        return result;
    }

    /** 后缀与查询的前缀比较（<0 后缀小于查询作为前缀序）。 */
    private int compareWithQuery(int suffixStart, String q) {
        for (int i = 0; i < q.length(); i++) {
            if (suffixStart + i >= text.length()) {
                return -1;
            }
            char c = text.charAt(suffixStart + i);
            if (c != q.charAt(i)) {
                return c < q.charAt(i) ? -1 : 1;
            }
        }
        return 0;
    }

    private int lowerBound(String q) {
        int lo = 0;
        int hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (compareWithQuery(sa[mid], q) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private int upperBound(String q) {
        int lo = 0;
        int hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (compareWithQuery(sa[mid], q) <= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static String requireQuery(String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("查询非空");
        }
        return query;
    }
}
