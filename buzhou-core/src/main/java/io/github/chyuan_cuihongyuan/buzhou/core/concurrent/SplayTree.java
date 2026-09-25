package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * Splay Tree 伸展树（spec 6001 / T6203 / impl 2202）——
 * Sleator-Tarjan 伸展树思想（GCC splay-tree 同源）：
 * **访问即伸展**的自调整有序映射——put/get/remove 后目标键
 * 经三式旋转（zig 单旋 / zig-zig 一字形双旋 / zig-zag 之字形
 * 双旋）上浮至根，热键恒在根附近（摊还 O(log n)），节点不携
 * 平衡元数据（高度/色/秩全免）——普通 BST 顺序插入退化成链
 * （操作 O(n)）与平衡树旋转不变量繁多（实现复杂）的病解。
 *
 * <p>自顶向下一次遍历完成分裂与合并（dummy 表头挂左右拼片），
 * 结构由操作史定（确定性——同操作序列同根同中序）；
 * remove 后根=被删键的前驱（左子树最右节点——确定性锚）。
 *
 * <p>与 PairingHeap（spec 5048）同族不同面：优先序堆 vs
 * 全序映射；与 TreeMap（JDK）不同面：自调整摊还界 vs 严格
 * 平衡界。
 */
public final class SplayTree {

    private static final class Node {
        long key;
        long value;
        Node left;
        Node right;

        Node(long key, long value) {
            this.key = key;
            this.value = value;
        }
    }

    /** 自顶向下伸展的工作表头（复用免分配；进入前双臂清零）。 */
    private final Node header = new Node(0L, 0L);

    private Node root;
    private int size;

    /** 插入/覆盖（upsert——同键覆盖不增位）。 */
    public void put(long key, long value) {
        if (root == null) {
            root = new Node(key, value);
            size++;
            return;
        }
        splay(key);
        if (root.key == key) {
            root.value = value;
            return;
        }
        Node fresh = new Node(key, value);
        if (key < root.key) {
            fresh.left = root.left;
            fresh.right = root;
            root.left = null;
        } else {
            fresh.right = root.right;
            fresh.left = root;
            root.right = null;
        }
        root = fresh;
        size++;
    }

    /** 取值（缺席返回 null；访问后最近键上浮至根——摊还界来源）。 */
    public Long get(long key) {
        if (root == null) {
            return null;
        }
        splay(key);
        return root.key == key ? root.value : null;
    }

    /** 是否包含键（含访问伸展语义）。 */
    public boolean containsKey(long key) {
        return get(key) != null;
    }

    /** 删除（嫌席 IAE fail-fast；删除后根=被删键前驱）。 */
    public void remove(long key) {
        if (root == null) {
            throw new IllegalArgumentException("空树不可删除");
        }
        splay(key);
        if (root.key != key) {
            throw new IllegalArgumentException("键不在树不可删除: " + key);
        }
        Node leftTree = root.left;
        Node rightTree = root.right;
        if (leftTree == null) {
            root = rightTree;
        } else {
            Node max = leftTree;
            Node parentOfMax = null;
            while (max.right != null) {
                parentOfMax = max;
                max = max.right;
            }
            if (parentOfMax == null) {
                max.right = rightTree;
                root = max;
            } else {
                parentOfMax.right = max.left;
                max.left = leftTree;
                max.right = rightTree;
                root = max;
            }
        }
        size--;
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 是否空树。 */
    public boolean isEmpty() {
        return size == 0;
    }

    /** 根键读数（结构确定性——最后被伸展键；空树 IAE）。 */
    public long rootKey() {
        if (root == null) {
            throw new IllegalArgumentException("空树不可取根");
        }
        return root.key;
    }

    /** 中序全序导出（左-根-右；确定性）。 */
    public long[] keysInOrder() {
        long[] out = new long[size];
        java.util.Deque<Node> stack = new java.util.ArrayDeque<>();
        Node cur = root;
        int idx = 0;
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();
            out[idx++] = cur.key;
            cur = cur.right;
        }
        return out;
    }

    /** 自顶向下伸展（Sleator 经典式：单次遍历同时完成分裂）。 */
    private void splay(long key) {
        Node left = header;
        Node right = header;
        Node cur = root;
        header.left = null;
        header.right = null;
        while (cur.key != key) {
            if (key < cur.key) {
                if (cur.left == null) {
                    break;
                }
                if (key < cur.left.key) {
                    Node lifted = cur.left;
                    cur.left = lifted.right;
                    lifted.right = cur;
                    cur = lifted;
                    if (cur.left == null) {
                        break;
                    }
                }
                right.left = cur;
                right = cur;
                cur = cur.left;
            } else {
                if (cur.right == null) {
                    break;
                }
                if (key > cur.right.key) {
                    Node lifted = cur.right;
                    cur.right = lifted.left;
                    lifted.left = cur;
                    cur = lifted;
                    if (cur.right == null) {
                        break;
                    }
                }
                left.right = cur;
                left = cur;
                cur = cur.right;
            }
        }
        left.right = cur.left;
        right.left = cur.right;
        cur.left = header.right;
        cur.right = header.left;
        root = cur;
    }
}
