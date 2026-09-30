package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 后缀自动机（spec 9020 / W9041 / impl 2373）——SAM 思想
 * （Blumer 1985/在线构造——competitive programming 与
 * suffix 结构同源）：**最小 DFA 恰接受全文所有子串：endpos
 * 等价类为状态、suffix link 树张成类层次——O(n) 状态在线
 * 构造**——逐查询扫描全文 O(nm) 与后缀数组+LCP 的离线重排
 * 之间的在线形态。面向查询：contains（在线子串判定
 * O(|q|)）、distinctSubstringCount（Σ(len−len(link))——
 * 恰等计数的经典不变量）、substringFrequency（endpos 大小
 * 沿 link 树聚出）；克隆状态语义完整（分裂不变量）；
 * null/空查询 fail-fast/诚实边界明示。
 *
 * <p>与 SuffixArray（metrics 域）同域不同面：序结构离线
 * 排序 vs 状态机在线判定；与 AhoCorasick（同域）不同面：
 * 多模式匹配 vs 单文全子串。
 */
public final class SuffixAutomaton {

    private static final int MAXN = 128;

    private final int[] len;
    private final int[] link;
    private final int[][] next;
    private final boolean[] clone;
    private final long[] endpos;
    private final int size;

    private SuffixAutomaton(int[] len, int[] link, int[][] next, boolean[] clone,
                            long[] endpos, int size) {
        this.len = len;
        this.link = link;
        this.next = next;
        this.clone = clone;
        this.endpos = endpos;
        this.size = size;
    }

    /**
     * 在线构造（文本 0..127 字节域——ASCII 明示）。
     *
     * @throws IllegalArgumentException null 文本
     */
    public static SuffixAutomaton of(String text) {
        if (text == null) {
            throw new IllegalArgumentException("文本非空引用");
        }
        int capacity = 2 * Math.max(1, text.length()) + 1;
        int[] len = new int[capacity];
        int[] link = new int[capacity];
        int[][] next = new int[capacity][];
        boolean[] clone = new boolean[capacity];
        int size = 1;
        link[0] = -1;
        next[0] = newState(MAXN);
        int last = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c >= MAXN) {
                throw new IllegalArgumentException("字节域 0..127（实际 " + c + "）");
            }
            int current = size++;
            len[current] = len[last] + 1;
            clone[current] = false;
            next[current] = newState(MAXN);
            int p = last;
            while (p != -1 && next[p][c] == 0) {
                next[p][c] = current;
                p = link[p];
            }
            if (p == -1) {
                link[current] = 0;
            } else {
                int q = next[p][c];
                if (len[p] + 1 == len[q]) {
                    link[current] = q;
                } else {
                    int copied = size++;
                    len[copied] = len[p] + 1;
                    clone[copied] = true;
                    next[copied] = Arrays.copyOf(next[q], MAXN);
                    link[copied] = link[q];
                    while (p != -1 && next[p][c] == q) {
                        next[p][c] = copied;
                        p = link[p];
                    }
                    link[q] = copied;
                    link[current] = copied;
                }
            }
            last = current;
        }
        // endpos 聚出：非克隆态初值 1，按 len 降序沿 link 汇总
        long[] endpos = new long[size];
        for (int v = 1; v < size; v++) {
            endpos[v] = clone[v] ? 0 : 1;
        }
        Integer[] order = new Integer[size];
        for (int v = 0; v < size; v++) {
            order[v] = v;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(len[b], len[a]));
        for (int v : order) {
            if (link[v] > 0) {
                endpos[link[v]] += endpos[v];
            }
        }
        return new SuffixAutomaton(len, link, next, clone, endpos, size);
    }

    private static int[] newState(int alphabet) {
        int[] transitions = new int[alphabet];
        Arrays.fill(transitions, 0);
        return transitions;
    }

    /** 在线子串判定（O(|query|)——空串 true 诚实边界）。 */
    public boolean containsSubstring(String query) {
        if (query == null) {
            throw new IllegalArgumentException("查询非空引用");
        }
        int state = 0;
        for (int i = 0; i < query.length(); i++) {
            int c = query.charAt(i);
            if (c >= MAXN || next[state][c] == 0) {
                return false;
            }
            state = next[state][c];
        }
        return true;
    }

    /** 互异子串数（Σ(len[v]−len[link[v]])——恰等计数不变量）。 */
    public long distinctSubstringCount() {
        long total = 0;
        for (int v = 1; v < size; v++) {
            total += len[v] - len[link[v]];
        }
        return total;
    }

    /** 子串出现次数（endpos 大小；非子串 0；空串全文长+1?——空查询 fail-fast 明示拒绝）。 */
    public long substringFrequency(String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("查询非空（空串频率语义拒绝）");
        }
        int state = 0;
        for (int i = 0; i < query.length(); i++) {
            int c = query.charAt(i);
            if (c >= MAXN || next[state][c] == 0) {
                return 0;
            }
            state = next[state][c];
        }
        return endpos[state];
    }

    /** 状态数（结构读数——克隆含入）。 */
    public int stateCount() {
        return size;
    }

    /** 全部长度恰为 length 的互异子串数（频次分布读数）。 */
    public List<Long> substringCountByLength() {
        long[] byLength = new long[len[longestState()]];
        for (int v = 1; v < size; v++) {
            for (int l = len[link[v]] + 1; l <= len[v]; l++) {
                byLength[l - 1]++;
            }
        }
        List<Long> result = new ArrayList<>(byLength.length);
        for (long count : byLength) {
            result.add(count);
        }
        return result;
    }

    private int longestState() {
        int longest = 0;
        for (int v = 1; v < size; v++) {
            if (len[v] > len[longest]) {
                longest = v;
            }
        }
        return longest;
    }
}
