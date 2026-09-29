package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 左偏可合并堆（spec 8019 / V8039 / impl 2321）——
 * Crane 1972 左偏树思想：**节点缓存零路径长（npl），左孩子
 * npl ≥ 右孩子 npl，合并沿右路径递归、违例即换左右**——
 * 单次合并 O(log n)（右路径长度界）——二叉堆合并要全量
 * 重排 O(n)（频繁合并放大）的病解。merge/offer/poll/peek
 * 四面+size 读数；null 元素 fail-fast（原语面 long 无 null
 * ——装箱面空堆 poll null 诚实缺省）；同操作序同构完全
 * 确定（并列取左——先入侧）。
 *
 * <p>与 FibonacciHeap（spec 8020）同族不同面：单次合并
 * O(log n) 简单结构 vs 摊均 O(1) 合并的复杂结构。
 */
public final class LeftistHeap {

    private static final class Node {
        private final long value;
        private Node left;
        private Node right;
        private int npl;

        private Node(long value) {
            this.value = value;
        }
    }

    private Node root;
    private int size;

    /** 合并另一堆（搬空对方——所有权转移语义）。 */
    public void merge(LeftistHeap other) {
        if (other == null || other == this) {
            throw new IllegalArgumentException("被合并堆非空且非自身");
        }
        root = mergeNodes(root, other.root);
        size += other.size;
        other.root = null;
        other.size = 0;
    }

    /** 入队。 */
    public void offer(long value) {
        root = mergeNodes(root, new Node(value));
        size++;
    }

    /** 弹最小（空堆 null 诚实缺省）。 */
    public Long poll() {
        if (root == null) {
            return null;
        }
        long value = root.value;
        root = mergeNodes(root.left, root.right);
        size--;
        return value;
    }

    /** 看最小（空堆 null 诚实缺省）。 */
    public Long peek() {
        return root == null ? null : root.value;
    }

    /** 元素数。 */
    public int size() {
        return size;
    }

    /** npl 不变量审计：左 npl ≥ 右 npl 且 npl 值真实（测试/自检面）。 */
    public boolean leftistInvariantHolds() {
        return checkNpl(root) != VIOLATED;
    }

    private static final int VIOLATED = Integer.MIN_VALUE;

    private int checkNpl(Node node) {
        if (node == null) {
            return -1;
        }
        int left = checkNpl(node.left);
        if (left == VIOLATED) {
            return VIOLATED;
        }
        int right = checkNpl(node.right);
        if (right == VIOLATED) {
            return VIOLATED;
        }
        if (left < right || node.npl != right + 1) {
            return VIOLATED;
        }
        return node.npl;
    }

    private static int npl(Node node) {
        return node == null ? -1 : node.npl;
    }

    private static Node mergeNodes(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.value > b.value) {
            Node tmp = a;
            a = b;
            b = tmp;
        }
        a.right = mergeNodes(a.right, b);
        if (npl(a.left) < npl(a.right)) {
            Node tmp = a.left;
            a.left = a.right;
            a.right = tmp;
        }
        a.npl = npl(a.right) + 1;
        return a;
    }
}
