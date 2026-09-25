package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * BK 树（spec 6008 / T6217 / impl 2209）——
 * Burkhard-Keller 度量树思想（拼写检查/模糊搜索同源）：
 * 节点按「到父词的编辑距离」分叉——查询对节点算 d(node,
 * word) 后**只下探距离 ∈ [d−r, d+r] 的分支**（三角不等式
 * 保证分支外不可能在半径 r 内——剪枝不漏），避免每查询全
 * 字典 O(N·L²) 扫（词库大时放大失控）的病解。动态 add
 * 友好（词集增量）；d=0 重复词幂等不增位；query 结果字典
 * 序 canonical 输出（确定性可回放）。
 *
 * <p>与 TextDistance（spec 2052）同族不同面：距离度量 vs
 * 邻域索引结构；与 AhoCorasick（spec 6006）不同面：精确
 * 多模式命中 vs 近似邻域候选。树形由插入序定构。
 */
public final class BkTree {

    private static final class Node {
        final String word;
        final Map<Integer, Node> childrenByDistance = new TreeMap<>();

        Node(String word) {
            this.word = word;
        }
    }

    private Node root;
    private int size;

    /** 加入词（null fail-fast；重复词幂等）。 */
    public void add(String word) {
        requireNonNullWord(word);
        if (root == null) {
            root = new Node(word);
            size++;
            return;
        }
        Node cur = root;
        while (true) {
            int distance = editDistance(cur.word, word);
            if (distance == 0) {
                return;
            }
            Node child = cur.childrenByDistance.get(distance);
            if (child == null) {
                cur.childrenByDistance.put(distance, new Node(word));
                size++;
                return;
            }
            cur = child;
        }
    }

    /** 半径 r 的邻域候选（字典序 canonical；r<0 fail-fast）。 */
    public List<String> query(String word, int maxDistance) {
        requireNonNullWord(word);
        if (maxDistance < 0) {
            throw new IllegalArgumentException("半径必须非负: " + maxDistance);
        }
        List<String> out = new ArrayList<>();
        if (root != null) {
            collect(root, word, maxDistance, out);
        }
        out.sort(String::compareTo);
        return out;
    }

    /** 词数读数。 */
    public int size() {
        return size;
    }

    private void collect(Node node, String word, int maxDistance, List<String> out) {
        int distance = editDistance(node.word, word);
        if (distance <= maxDistance) {
            out.add(node.word);
        }
        for (Map.Entry<Integer, Node> e : node.childrenByDistance.entrySet()) {
            if (e.getKey() >= distance - maxDistance && e.getKey() <= distance + maxDistance) {
                collect(e.getValue(), word, maxDistance, out);
            }
        }
    }

    /** 经典编辑距离 DP（滚动行）。 */
    private static int editDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitute = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(substitute, Math.min(prev[j] + 1, cur[j - 1] + 1));
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }

    private static void requireNonNullWord(String word) {
        if (word == null) {
            throw new IllegalArgumentException("词非空");
        }
    }
}
