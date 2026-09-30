package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * 替罪羊树（spec 9010 / W9021 / impl 2363）——Scapegoat tree
 * 思想（Galperin–Rivest 1993）：**零旋转零元数据——深度超
 * log_{1/α} n 或删空超阈即整子树推平重建为完美平衡**（替罪羊
 * = 失衡最深祖先），α=0.75 固定——旋转不变量繁多（AVL/红黑）
 * 与访问路径摊还（Splay/Treap）之外的第三条路：简单性优先。
 * 有序集语义：insert/contains/remove/size/inOrder；重复插入
 * 幂等 false；缺席删除 false；重建完全确定（同操作序列同树）。
 *
 * <p>与 SplayTree（spec 6001）/Treap（spec 6002）同域不同面：
 * 自调整/随机平衡 vs 推平重建；与 AvlTree（同包）对照：旋转
 * 元数据全免。
 */
public final class ScapegoatTree {

    private static final double ALPHA = 0.75;

    private static final class Node {
        private int key;
        private Node left;
        private Node right;
        private int size;

        private Node(int key) {
            this.key = key;
            this.size = 1;
        }
    }

    private Node root;
    private int liveCount;
    private int rebuiltSizeHighWater;

    /** 空树起步。 */
    public ScapegoatTree() {
        this.rebuiltSizeHighWater = 0;
    }

    /** 插入（已存在幂等 false；深度超 log_{1/α} n 触发自底向上替罪羊重建）。 */
    public boolean insert(int key) {
        java.util.ArrayList<Node> path = new java.util.ArrayList<>();
        Node node = root;
        while (node != null) {
            path.add(node);
            if (key == node.key) {
                return false;
            }
            node = key < node.key ? node.left : node.right;
        }
        Node fresh = new Node(key);
        if (path.isEmpty()) {
            root = fresh;
        } else {
            Node parent = path.get(path.size() - 1);
            if (key < parent.key) {
                parent.left = fresh;
            } else {
                parent.right = fresh;
            }
        }
        for (Node ancestor : path) {
            ancestor.size++;
        }
        liveCount++;
        rebuiltSizeHighWater = Math.max(rebuiltSizeHighWater, liveCount);
        int depth = path.size();
        if (depth > logLimit(liveCount)) {
            rebuildScapegoat(path);
        }
        return true;
    }

    /** 最高 α 失衡祖先（size(路径子) > α·size(父)——链场景治本）推平重建。 */
    private void rebuildScapegoat(java.util.ArrayList<Node> path) {
        int highest = -1;
        for (int i = 1; i < path.size(); i++) {
            Node parent = path.get(i - 1);
            Node child = path.get(i);
            if (child.size > ALPHA * parent.size) {
                highest = i - 1;
            }
        }
        if (highest < 0) {
            return;
        }
        Node scapegoat = path.get(highest);
        Node rebuilt = rebuild(scapegoat);
        if (scapegoat == root) {
            root = rebuilt;
        } else {
            Node grand = path.get(highest - 1);
            if (grand.left == scapegoat) {
                grand.left = rebuilt;
            } else {
                grand.right = rebuilt;
            }
        }
    }

    /** log_{1/α}(size) 深度上限（α=0.75）。 */
    private static int logLimit(int size) {
        return (int) Math.ceil(Math.log(size) / Math.log(1.0 / ALPHA)) + 1;
    }

    /** 删除（缺席 false；live 掉半高水线触发全树重建）。 */
    public boolean remove(int key) {
        if (!contains(key)) {
            return false;
        }
        root = removeFrom(root, key);
        liveCount--;
        if (liveCount < ALPHA * rebuiltSizeHighWater / 2) {
            root = rebuild(root);
            rebuiltSizeHighWater = liveCount;
        }
        return true;
    }

    private Node removeFrom(Node node, int key) {
        if (key == node.key) {
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }
            Node successor = node.right;
            while (successor.left != null) {
                successor = successor.left;
            }
            node.key = successor.key;
            node.right = removeFrom(node.right, successor.key);
        } else if (key < node.key) {
            node.left = removeFrom(node.left, key);
        } else {
            node.right = removeFrom(node.right, key);
        }
        node.size = 1 + sizeOf(node.left) + sizeOf(node.right);
        return node;
    }

    public boolean contains(int key) {
        Node node = root;
        while (node != null) {
            if (key == node.key) {
                return true;
            }
            node = key < node.key ? node.left : node.right;
        }
        return false;
    }

    public int size() {
        return liveCount;
    }

    /** 中序全序（升序）。 */
    public int[] inOrder() {
        List<Integer> keys = new ArrayList<>(liveCount);
        collect(root, keys);
        int[] result = new int[keys.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = keys.get(i);
        }
        return result;
    }

    /** 树高（同包测试面：平衡不变量锚）。 */
    int height() {
        return heightOf(root);
    }

    private static int heightOf(Node node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(heightOf(node.left), heightOf(node.right));
    }

    private static void collect(Node node, List<Integer> keys) {
        if (node == null) {
            return;
        }
        collect(node.left, keys);
        keys.add(node.key);
        collect(node.right, keys);
    }

    private static int sizeOf(Node node) {
        return node == null ? 0 : node.size;
    }

    /** 推平重建：完美平衡（中序二分定根——完全确定）。 */
    private static Node rebuild(Node node) {
        List<Integer> keys = new ArrayList<>(sizeOf(node));
        collect(node, keys);
        return buildBalanced(keys, 0, keys.size() - 1);
    }

    private static Node buildBalanced(List<Integer> keys, int lo, int hi) {
        if (lo > hi) {
            return null;
        }
        int mid = (lo + hi) >>> 1;
        Node node = new Node(keys.get(mid));
        node.left = buildBalanced(keys, lo, mid - 1);
        node.right = buildBalanced(keys, mid + 1, hi);
        node.size = 1 + sizeOf(node.left) + sizeOf(node.right);
        return node;
    }
}
