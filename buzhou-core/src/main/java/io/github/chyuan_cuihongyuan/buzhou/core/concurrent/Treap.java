package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * Treap 树堆（spec 6002 / T6205 / impl 2203）——
 * Seidel-Aragon 树堆思想（随机优先级平衡 BST）：
 * **BST 键序 + 最小堆优先级**双不变量的有序映射——新节点
 * 优先级由种子化 SplitMix64 生成（同种子同优先级同结构，
 * 确定性可回放），插入后沿路径按优先级**单旋**上浮、remove
 * 将目标旋降至叶再摘除——期望 O(log n) 平衡只靠「优先级
 * 随机 + 单旋转」拿到，AVL/红黑的旋转不变量（色/平衡因子/
 * 双旋分情形）全免——普通 BST 顺序插入退化链（O(n)）与
 * 平衡树实现复杂（不变量繁多）的病解。
 *
 * <p>与 SplayTree（spec 6001）同族不同面：访问自调整
 * （无先验随机） vs 先验随机平衡（无访问历史依赖）；与
 * SkipList（spec 5025）同族不同面：概率多层链 vs 随机
 * 优先级旋转树。
 */
public final class Treap {

    private static final long DEFAULT_SEED = 6002L;

    private static final class Node {
        long key;
        long value;
        long priority;
        Node left;
        Node right;

        Node(long key, long value, long priority) {
            this.key = key;
            this.value = value;
            this.priority = priority;
        }
    }

    private Node root;
    private int size;
    private long seed;

    /** 默认种子（固定——缺省构造也确定性）。 */
    public Treap() {
        this(DEFAULT_SEED);
    }

    /** 种子注入（同种子同操作序列同结构）。 */
    public Treap(long seed) {
        this.seed = seed;
    }

    /** 插入/覆盖（upsert——同键覆盖不增位、不换优先级）。 */
    public void put(long key, long value) {
        root = insert(root, key, value);
    }

    /** 取值（缺席 null）。 */
    public Long get(long key) {
        Node cur = root;
        while (cur != null) {
            if (key == cur.key) {
                return cur.value;
            }
            cur = key < cur.key ? cur.left : cur.right;
        }
        return null;
    }

    /** 是否包含键。 */
    public boolean containsKey(long key) {
        return get(key) != null;
    }

    /** 删除（嫌席 IAE fail-fast）：旋降至叶再摘除。 */
    public void remove(long key) {
        if (!containsKey(key)) {
            throw new IllegalArgumentException("键不在树不可删除: " + key);
        }
        root = delete(root, key);
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

    /** 高度读数（平衡度可见；空树 0）。 */
    public int height() {
        return height(root);
    }

    /** 中序全序导出（确定性）。 */
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

    private Node insert(Node node, long key, long value) {
        if (node == null) {
            size++;
            return new Node(key, value, splitMix64());
        }
        if (key == node.key) {
            node.value = value;
            return node;
        }
        if (key < node.key) {
            node.left = insert(node.left, key, value);
            if (node.left.priority < node.priority) {
                node = rotateRight(node);
            }
        } else {
            node.right = insert(node.right, key, value);
            if (node.right.priority < node.priority) {
                node = rotateLeft(node);
            }
        }
        return node;
    }

    private Node delete(Node node, long key) {
        if (key < node.key) {
            node.left = delete(node.left, key);
            return node;
        }
        if (key > node.key) {
            node.right = delete(node.right, key);
            return node;
        }
        return rotateDownAndDetach(node);
    }

    /** 目标节点旋降至叶（子优先级小者上旋）后摘除。 */
    private Node rotateDownAndDetach(Node node) {
        if (node.left == null) {
            return node.right;
        }
        if (node.right == null) {
            return node.left;
        }
        if (node.left.priority < node.right.priority) {
            Node lifted = rotateRight(node);
            lifted.right = delete(lifted.right, node.key);
            return lifted;
        }
        Node lifted = rotateLeft(node);
        lifted.left = delete(lifted.left, node.key);
        return lifted;
    }

    private Node rotateRight(Node node) {
        Node lifted = node.left;
        node.left = lifted.right;
        lifted.right = node;
        return lifted;
    }

    private Node rotateLeft(Node node) {
        Node lifted = node.right;
        node.right = lifted.left;
        lifted.left = node;
        return lifted;
    }

    private int height(Node node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    /** SplitMix64 逐节点前向状态推进（确定性优先级源）。 */
    private long splitMix64() {
        seed += 0x9E3779B97F4A7C15L;
        long z = seed;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
