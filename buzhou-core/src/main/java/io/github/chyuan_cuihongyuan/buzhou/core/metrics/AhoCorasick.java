package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aho-Corasick 自动机（spec 6006 / T6213 / impl 2207）——
 * Aho-Corasick 多模式匹配思想（ripgrep/安全扫描器同源）：
 * **Trie + BFS 失配链**——文本单次扫描命中全部模式，失配时
 * 沿 fail 回退复用后缀状态（O(文本长 + 命中数)，与模式数
 * 无关）——每模式独立 indexOf 扫（O(模式数×文本长) 放大）
 * 的病解。输出 canonical 序：按起始位置升序、同位按模式
 * 注册序（确定性——同文本同模式集同输出序，可回放）。
 *
 * <p>与 KmpSearch（Q 系）同族不同面：单模式前缀函数 vs 多
 * 模式自动机一次扫；与 XorFilter（spec 5042）不同面：成员
 * 判定 vs 位置命中。
 */
public final class AhoCorasick {

    /** 命中（end 排他）。 */
    public record Match(String pattern, int start, int end) {
    }

    private static final class Node {
        final Map<Character, Node> next = new HashMap<>();
        final List<Integer> patternIndices = new ArrayList<>();
        Node fail;
    }

    private final String[] patterns;
    private final Node root;

    /** 构建即编译（null/空模式 fail-fast；空模式集允许——恒零命中）。 */
    public AhoCorasick(List<String> patternList) {
        if (patternList == null) {
            throw new IllegalArgumentException("模式集非空");
        }
        for (String p : patternList) {
            if (p == null || p.isEmpty()) {
                throw new IllegalArgumentException("模式不可为空: " + p);
            }
        }
        this.patterns = patternList.toArray(new String[0]);
        this.root = new Node();
        for (int i = 0; i < patterns.length; i++) {
            insert(patterns[i], i);
        }
        linkFails();
    }

    /** 文本单次扫描（canonical 序：起始升序、模式注册序）。 */
    public List<Match> scan(String text) {
        if (text == null) {
            throw new IllegalArgumentException("文本非空");
        }
        List<Match> matches = new ArrayList<>();
        Node cur = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (cur != root && cur.next.get(c) == null) {
                cur = cur.fail;
            }
            Node moved = cur.next.get(c);
            cur = moved != null ? moved : root;
            for (Node hit = cur; hit != root; hit = hit.fail) {
                for (int idx : hit.patternIndices) {
                    String p = patterns[idx];
                    matches.add(new Match(p, i - p.length() + 1, i + 1));
                }
            }
        }
        matches.sort((a, b) -> {
            int byStart = Integer.compare(a.start(), b.start());
            if (byStart != 0) {
                return byStart;
            }
            return a.pattern().compareTo(b.pattern());
        });
        return matches;
    }

    /** 模式数读数。 */
    public int patternCount() {
        return patterns.length;
    }

    private void insert(String pattern, int index) {
        Node cur = root;
        for (int i = 0; i < pattern.length(); i++) {
            cur = cur.next.computeIfAbsent(pattern.charAt(i), c -> new Node());
        }
        cur.patternIndices.add(index);
    }

    private void linkFails() {
        java.util.Deque<Node> queue = new java.util.ArrayDeque<>();
        for (Node child : root.next.values()) {
            child.fail = root;
            queue.offer(child);
        }
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            for (Map.Entry<Character, Node> e : node.next.entrySet()) {
                char c = e.getKey();
                Node child = e.getValue();
                Node fail = node.fail;
                while (fail != root && fail.next.get(c) == null) {
                    fail = fail.fail;
                }
                Node candidate = fail.next.get(c);
                child.fail = candidate != null && candidate != child ? candidate : root;
                queue.offer(child);
            }
        }
    }
}
