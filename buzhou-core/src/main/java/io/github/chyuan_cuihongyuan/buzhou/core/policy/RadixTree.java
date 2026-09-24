package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Radix Tree 基数树（spec 5034 / T6169 / impl 2185）——
 * go-chi/httprouter 静态路由思想：压缩前缀树（单字符边
 * 合并为字符串边——公共前缀只存一份，节点数=分支点数
 * 而非字符数），`insert` 沿边行走、部分命中即分裂出中间
 * 节点（静态建树语义——终态与插入序无关）；
 * `match` 最长前缀匹配（路径尽深处最近的终节点胜出）——
 * 朴素逐键 startsWith 扫描（每查 O(键数×键长)）的病解。
 * 确定性无时间依赖。
 *
 * <p>与 JumpHash（spec 5030）同族不同面：前缀结构匹配 vs
 * 无状态散列分布（结构可枚举可通配，散列只保均匀）。
 */
public final class RadixTree<V> {

    /** 一次最长前缀命中（命中键=输入的前缀）。 */
    public record Match<V>(String matchedKey, V value) {
    }

    private static final class Node {
        final Map<Character, Edge> edges = new LinkedHashMap<>();
        boolean terminal;
        Object value;
    }

    private static final class Edge {
        String label;
        Node target;

        Edge(String label, Node target) {
            this.label = label;
            this.target = target;
        }
    }

    private final Node root = new Node();
    private int nodeCount = 1;
    private int keyCount;

    /**
     * 插入键值（终态与插入序无关——部分命中边分裂出中间
     * 节点；重复键/null/空键 fail-fast）。
     */
    public void insert(String key, V value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key 非空");
        }
        if (value == null) {
            throw new IllegalArgumentException("value 非空");
        }
        Node current = root;
        int consumed = 0;
        while (consumed < key.length()) {
            Edge edge = current.edges.get(key.charAt(consumed));
            if (edge == null) {
                current.edges.put(key.charAt(consumed),
                        new Edge(key.substring(consumed), newTerminal(value)));
                nodeCount++;
                keyCount++;
                return;
            }
            int common = commonPrefixLength(edge.label, key, consumed);
            if (common == edge.label.length()) {
                current = edge.target;
                consumed += common;
                continue;
            }
            Node middle = new Node();
            middle.edges.put(edge.label.charAt(common),
                    new Edge(edge.label.substring(common), edge.target));
            nodeCount++;
            edge.label = edge.label.substring(0, common);
            edge.target = middle;
            if (common == key.length() - consumed) {
                middle.terminal = true;
                middle.value = value;
            } else {
                middle.edges.put(key.charAt(consumed + common),
                        new Edge(key.substring(consumed + common), newTerminal(value)));
                nodeCount++;
            }
            keyCount++;
            return;
        }
        if (current.terminal) {
            throw new IllegalArgumentException("键已存在：" + key);
        }
        current.terminal = true;
        current.value = value;
        keyCount++;
    }

    /**
     * 最长前缀匹配（输入尽深处最近的终节点胜出；无终节点
     * 命中返回空）。
     */
    public Optional<Match<V>> match(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input 非空");
        }
        Node node = root;
        int consumed = 0;
        Optional<Match<V>> best = Optional.empty();
        while (consumed < input.length()) {
            Edge edge = node.edges.get(input.charAt(consumed));
            if (edge == null) {
                break;
            }
            if (!input.startsWith(edge.label, consumed)) {
                break;
            }
            node = edge.target;
            consumed += edge.label.length();
            if (node.terminal) {
                best = Optional.of(new Match<>(input.substring(0, consumed), valueOf(node)));
            }
        }
        return best;
    }

    /** 已注册键数读数。 */
    public int keyCount() {
        return keyCount;
    }

    /** 节点数读数（=根+分支点+终点——压缩后远小于字符总数）。 */
    public int nodeCount() {
        return nodeCount;
    }

    @SuppressWarnings("unchecked")
    private V valueOf(Node node) {
        return (V) node.value;
    }

    private Node newTerminal(V value) {
        Node node = new Node();
        node.terminal = true;
        node.value = value;
        return node;
    }

    private static int commonPrefixLength(String label, String key, int from) {
        int max = Math.min(label.length(), key.length() - from);
        for (int i = 0; i < max; i++) {
            if (label.charAt(i) != key.charAt(from + i)) {
                return i;
            }
        }
        return max;
    }
}
