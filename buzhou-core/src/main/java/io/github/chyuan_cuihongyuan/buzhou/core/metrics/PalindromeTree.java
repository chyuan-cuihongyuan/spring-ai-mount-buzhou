package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 回文树（spec 9021 / W9043 / impl 2374）——Eertree 思想
 * （Apostolico 1995/Gusfield 同源，在线构造）：**每个回文子串
 * 恰一个状态、双根（长度 −1 奇根/0 偶根）+ 最长回文后缀扩展
 * 链——增量一字符至多新增一态**——逐中心枚举 O(n²) 与 Manacher
 * 单一最长的中间形态：全部互异回文在线收集。distinct 数、
 * longest 长度、全部回文串（字典序升序）三查询面；空文本零
 * 回文诚实边界；null fail-fast。
 *
 * <p>与 Manacher（metrics 域）同域不同面：单一最长回文半径 vs
 * 全部互异回文集；与 SuffixAutomaton（spec 9020）同构不同面：
 * 全子串等价类 vs 回文子串等价类。
 */
public final class PalindromeTree {

    private final List<Node> nodes;
    private final int[] occur;
    private final String text;

    private static final class Node {
        private final int length;
        private int suffixLink;
        private final Map<Character, Integer> transitions = new HashMap<>();
        private int start = -1;

        private Node(int length) {
            this.length = length;
        }
    }

    private PalindromeTree(List<Node> nodes, int[] occur, String text) {
        this.nodes = nodes;
        this.occur = occur;
        this.text = text;
    }

    /**
     * 在线构造。
     *
     * @throws IllegalArgumentException null 文本
     */
    public static PalindromeTree of(String text) {
        if (text == null) {
            throw new IllegalArgumentException("文本非空引用");
        }
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(-1)); // 奇根
        nodes.add(new Node(0));  // 偶根
        nodes.get(0).suffixLink = 0;
        nodes.get(1).suffixLink = 0;
        int[] position = new int[text.length()];
        int last = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int current = findAddable(nodes, last, i, text);
            if (current == -1) {
                // 单字符回文挂偶根链（经奇根 -1 语义）
                int fresh = nodes.size();
                Node node = new Node(1);
                node.start = i;
                nodes.add(node);
                nodes.get(1).transitions.put(c, fresh);
                nodes.get(fresh).suffixLink = 1;
                position[i] = fresh;
                last = fresh;
                continue;
            }
            Integer existing = nodes.get(current).transitions.get(c);
            if (existing != null) {
                position[i] = existing;
                last = existing;
                continue;
            }
            int fresh = nodes.size();
            Node node = new Node(nodes.get(current).length + 2);
            node.start = i - nodes.get(current).length - 1;
            nodes.add(node);
            nodes.get(current).transitions.put(c, fresh);
            // 新态的后缀链 = 同字符扩展的次长回文后缀
            int linkBase = findAddable(nodes, nodes.get(current).suffixLink, i, text);
            if (node.length == 1) {
                nodes.get(fresh).suffixLink = 1;
            } else {
                int linkTarget = linkBase == -1 ? 1 : nodes.get(linkBase).transitions.get(c);
                nodes.get(fresh).suffixLink = linkTarget == fresh ? 1 : linkTarget;
            }
            position[i] = fresh;
            last = fresh;
        }
        int[] occur = new int[nodes.size()];
        for (int i = 0; i < text.length(); i++) {
            occur[position[i]]++;
        }
        // 按长度降序沿后缀链传播（link 严格更短——级联正确性靠长度序）
        Integer[] byLength = new Integer[nodes.size()];
        for (int v = 0; v < nodes.size(); v++) {
            byLength[v] = v;
        }
        java.util.Arrays.sort(byLength, (a, b) -> Integer.compare(nodes.get(b).length, nodes.get(a).length));
        for (int v : byLength) {
            int parent = nodes.get(v).suffixLink;
            if (parent > 1) {
                occur[parent] += occur[v];
            }
        }
        return new PalindromeTree(nodes, occur, text);
    }

    /** 从 base 沿后缀链找可由 text[i] 两翼扩展的状态（-1 = 需走奇根单字符）。 */
    private static int findAddable(List<Node> nodes, int base, int i, String text) {
        int state = base;
        while (true) {
            int length = nodes.get(state).length;
            int left = i - length - 1;
            if (left >= 0 && text.charAt(left) == text.charAt(i)) {
                return state;
            }
            if (state == 0) {
                return -1;
            }
            state = nodes.get(state).suffixLink;
        }
    }

    /** 互异回文子串数（每回文恰一态）。 */
    public long distinctPalindromeCount() {
        return nodes.size() - 2;
    }

    /** 最长回文子串长度（空文本 0）。 */
    public int longestPalindromeLength() {
        int longest = 0;
        for (int v = 2; v < nodes.size(); v++) {
            longest = Math.max(longest, nodes.get(v).length);
        }
        return longest;
    }

    /** 回文→出现次数（字典序升序稳定读面）。 */
    public Map<String, Long> palindromeFrequencies() {
        Map<String, Long> result = new java.util.TreeMap<>();
        for (int v = 2; v < nodes.size(); v++) {
            result.put(substringOf(nodes.get(v)), (long) occur[v]);
        }
        return result;
    }

    /** 全部互异回文（字典序升序）。 */
    public List<String> allPalindromes() {
        List<String> result = new ArrayList<>();
        for (int v = 2; v < nodes.size(); v++) {
            Node node = nodes.get(v);
            result.add(substringOf(node));
        }
        result.sort(String::compareTo);
        return result;
    }

    private String substringOf(Node node) {
        return text.substring(node.start, node.start + node.length);
    }
}
