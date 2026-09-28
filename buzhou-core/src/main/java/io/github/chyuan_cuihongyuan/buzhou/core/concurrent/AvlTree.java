package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * AVL 树（spec 7003 / U7207 / impl 2255）——Adelson-Velsky
 * 与 Landis 1962 **首个自平衡 BST**思想：每节点缓存子树高，
 * 插入/删除回溯重平衡（LL/RR 单旋、LR/RL 双旋），平衡因子
 * 恒 ∈ {−1,0,+1}——高度界 1.44·log₂(n)（朴素 BST 顺序插入
 * 退化链 O(n) 的根治）。put upsert（同键覆值不增位）、
 * delete（缺席 fail-fast）；键序遍历确定性——同操作序列
 * 同构（旋转由平衡因子唯一决定，无随机无惰性）。
 *
 * <p>与 SplayTree（spec 6001）同族不同面：访问自调整
 * （摊还 O(log n)，结构随访问漂移）vs 严格平衡（最坏
 * O(log n)，结构只随插删变）；与 Treap（6002）不同面：
 * 随机优先级 vs 确定性旋转。
 */
public final class AvlTree {

    private static final class Node {
        long key;
        long value;
        Node left;
        Node right;
        int height = 1;

        Node(long key, long value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node root;
    private int size;

    /** 插入/覆值（upsert——同键覆值不增位）。 */
    public void put(long key, long value) {
        root = insert(root, key, value);
    }

    /** 取值（缺席 null——诚实缺省）。 */
    public Long get(long key) {
        Node node = root;
        while (node != null) {
            if (key < node.key) {
                node = node.left;
            } else if (key > node.key) {
                node = node.right;
            } else {
                return node.value;
            }
        }
        return null;
    }

    /** 成员判定。 */
    public boolean containsKey(long key) {
        return get(key) != null;
    }

    /** 删除（缺席 fail-fast——缺席语义与「删了什么」不可分）。 */
    public void remove(long key) {
        if (!containsKey(key)) {
            throw new IllegalArgumentException("键不存在: " + key);
        }
        root = delete(root, key);
        size--;
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 空判定。 */
    public boolean isEmpty() {
        return size == 0;
    }

    /** 全树高读数（空树 0）。 */
    public int height() {
        return height(root);
    }

    /** 键升序遍历（确定性）。 */
    public long[] keysInOrder() {
        long[] keys = new long[size];
        inorder(root, keys, new int[]{0});
        return keys;
    }

    // ---- 递归核（插入/删除共用回溯重平衡） ----

    private Node insert(Node node, long key, long value) {
        if (node == null) {
            size++;
            return new Node(key, value);
        }
        if (key < node.key) {
            node.left = insert(node.left, key, value);
        } else if (key > node.key) {
            node.right = insert(node.right, key, value);
        } else {
            node.value = value;
            return node;
        }
        return rebalance(node);
    }

    private Node delete(Node node, long key) {
        if (key < node.key) {
            node.left = delete(node.left, key);
        } else if (key > node.key) {
            node.right = delete(node.right, key);
        } else {
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }
            Node successor = min(node.right);
            node.key = successor.key;
            node.value = successor.value;
            node.right = delete(node.right, successor.key);
        }
        return rebalance(node);
    }

    private Node min(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private Node rebalance(Node node) {
        update(node);
        int balance = balanceOf(node);
        if (balance > 1) {
            if (balanceOf(node.left) < 0) {
                node.left = rotateLeft(node.left);
            }
            return rotateRight(node);
        }
        if (balance < -1) {
            if (balanceOf(node.right) > 0) {
                node.right = rotateRight(node.right);
            }
            return rotateLeft(node);
        }
        return node;
    }

    private int balanceOf(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private Node rotateRight(Node y) {
        Node x = y.left;
        y.left = x.right;
        x.right = y;
        update(y);
        update(x);
        return x;
    }

    private Node rotateLeft(Node x) {
        Node y = x.right;
        x.right = y.left;
        y.left = x;
        update(x);
        update(y);
        return y;
    }

    private void update(Node node) {
        node.height = 1 + Math.max(height(node.left), height(node.right));
    }

    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    private void inorder(Node node, long[] out, int[] cursor) {
        if (node == null) {
            return;
        }
        inorder(node.left, out, cursor);
        out[cursor[0]++] = node.key;
        inorder(node.right, out, cursor);
    }
}
