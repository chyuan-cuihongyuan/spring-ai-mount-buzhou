package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * Rope 文本缓冲（spec 4044 / T6089 / impl 2145）——树状分块
 * 文本缓冲思想（xi-editor/ropey）：权重平衡二叉树，叶持文本
 * 块（≤ {@value #LEAF_LIMIT} 字符），内部节点 weight = 子树
 * 总长——charAt/insert/delete 按权重导航；insert/delete 经
 * split+concat 组合；超限深度触发**重建**（叶中位切分重排，
 * 确定性再平衡）。大文本编辑 O(n²) 全量搬移（字符串拼接/
 * 裸数组位移）的病解。
 *
 * <p>与 ThreeWayMerge 同族不同面：编辑缓冲 vs 合并对账。
 */
public final class RopeBuffer {

    /** 叶块长度上限（ropey 同量级）。 */
    private static final int LEAF_LIMIT = 512;

    /** 深度余量（相对 2·log₂len 的容许超出）。 */
    private static final int DEPTH_SLACK = 8;

    private Node root;

    /** 定构（text 可空串）。 */
    public RopeBuffer(String text) {
        root = build(chunk(text == null ? "" : text));
    }

    /** 总长度读数。 */
    public int length() {
        return root.weight();
    }

    /** 树深读数（平衡性证据）。 */
    public int depth() {
        return depthOf(root);
    }

    /** 第 index 字符（越界 IOOBE fail-fast）。 */
    public char charAt(int index) {
        checkCharIndex(index, length());
        Node node = root;
        while (!node.isLeaf()) {
            if (index < node.left.weight()) {
                node = node.left;
            } else {
                index -= node.left.weight();
                node = node.right;
            }
        }
        return node.text.charAt(index);
    }

    /** 在 offset 处插入（偏移 ∈ [0, length]，越界 IOOBE）。 */
    public void insert(int offset, String text) {
        checkInsertOffset(offset, length());
        if (text == null || text.isEmpty()) {
            return;
        }
        Node[] parts = split(root, offset);
        root = concat(concat(parts[0], build(chunk(text))), parts[1]);
        rebalanceIfNeeded();
    }

    /** 删除 [start, end)（区间非法 IOOBE）。 */
    public void delete(int start, int end) {
        checkDeleteRange(start, end, length());
        if (start == end) {
            return;
        }
        Node[] head = split(root, start);
        Node[] tail = split(head[1], end - start);
        root = concat(head[0], tail[1]);
        rebalanceIfNeeded();
    }

    /** 全量导出（中序拼接）。 */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(root.weight());
        appendTo(root, sb);
        return sb.toString();
    }

    private void rebalanceIfNeeded() {
        int total = root.weight();
        int allowedDepth = 2 * (32 - Integer.numberOfLeadingZeros(Math.max(1, total))) + DEPTH_SLACK;
        if (depthOf(root) > allowedDepth) {
            List<String> leaves = new ArrayList<>();
            collectLeaves(root, leaves);
            root = build(leaves);
        }
    }

    private static void appendTo(Node node, StringBuilder sb) {
        if (node.isLeaf()) {
            sb.append(node.text);
            return;
        }
        appendTo(node.left, sb);
        appendTo(node.right, sb);
    }

    private static void collectLeaves(Node node, List<String> leaves) {
        if (node.isLeaf()) {
            if (!node.text.isEmpty()) {
                leaves.add(node.text);
            }
            return;
        }
        collectLeaves(node.left, leaves);
        collectLeaves(node.right, leaves);
    }

    /** 叶序列 → 平衡树（中位切分，确定性）。 */
    private static Node build(List<String> leaves) {
        if (leaves.isEmpty()) {
            return Node.leaf("");
        }
        if (leaves.size() == 1) {
            return Node.leaf(leaves.get(0));
        }
        int median = leaves.size() / 2;
        return Node.branch(build(leaves.subList(0, median)),
                build(leaves.subList(median, leaves.size())));
    }

    /** 长文本切叶块。 */
    private static List<String> chunk(String text) {
        List<String> leaves = new ArrayList<>(text.length() / LEAF_LIMIT + 1);
        for (int start = 0; start < text.length(); start += LEAF_LIMIT) {
            leaves.add(text.substring(start, Math.min(start + LEAF_LIMIT, text.length())));
        }
        if (leaves.isEmpty()) {
            leaves.add("");
        }
        return leaves;
    }

    /** 在 offset 处切成两棵（offset ∈ [0, weight]）。 */
    private static Node[] split(Node node, int offset) {
        if (node.isLeaf()) {
            return new Node[]{Node.leaf(node.text.substring(0, offset)),
                    Node.leaf(node.text.substring(offset))};
        }
        if (offset < node.left.weight()) {
            Node[] parts = split(node.left, offset);
            return new Node[]{parts[0], Node.branch(parts[1], node.right)};
        }
        if (offset > node.left.weight()) {
            Node[] parts = split(node.right, offset - node.left.weight());
            return new Node[]{Node.branch(node.left, parts[0]), parts[1]};
        }
        return new Node[]{node.left, node.right};
    }

    /** 拼接（空侧短路）。 */
    private static Node concat(Node left, Node right) {
        if (left.weight() == 0) {
            return right;
        }
        if (right.weight() == 0) {
            return left;
        }
        return Node.branch(left, right);
    }

    private static int depthOf(Node node) {
        if (node.isLeaf()) {
            return 0;
        }
        return 1 + Math.max(depthOf(node.left), depthOf(node.right));
    }

    private static void checkCharIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException("charAt 越界：" + index + "/" + length);
        }
    }

    private static void checkInsertOffset(int offset, int length) {
        if (offset < 0 || offset > length) {
            throw new StringIndexOutOfBoundsException("insert 偏移越界：" + offset + "/" + length);
        }
    }

    private static void checkDeleteRange(int start, int end, int length) {
        if (start < 0 || end > length || start > end) {
            throw new StringIndexOutOfBoundsException("delete 区间非法："
                    + start + ".." + end + "/" + length);
        }
    }

    /** 权重树节点（weight = 子树/叶总长；叶持文本）。 */
    private static final class Node {

        private final String text;
        private final Node left;
        private final Node right;
        private final int weight;

        private Node(String text, Node left, Node right, int weight) {
            this.text = text;
            this.left = left;
            this.right = right;
            this.weight = weight;
        }

        static Node leaf(String text) {
            return new Node(text, null, null, text.length());
        }

        static Node branch(Node left, Node right) {
            return new Node("", left, right, left.weight() + right.weight());
        }

        boolean isLeaf() {
            return left == null;
        }

        int weight() {
            return weight;
        }
    }
}
