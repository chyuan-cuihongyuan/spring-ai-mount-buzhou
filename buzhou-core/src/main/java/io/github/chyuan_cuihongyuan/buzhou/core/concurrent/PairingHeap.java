package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * Pairing Heap 配对堆（spec 5048 / T6197 / impl 2199）——
 * Fredman-Sedgewick 配对堆思想（Haskell containers/Boost
 * 同源）：多叉树最小堆，插入与 **meld（O(1) 指针挂钩）**
 * 为一等公民；extractMin 后对根的孩子做**两趟合并**
 （先左到右两两配对、再右到左依次并回——摊还 O(log n)）——
 * 二叉堆 meld O(n) 复制与二项堆复杂实现（结构不变量多）
 * 的病解。孩子序由操作史定（确定性——同操作序列同结构
 * 同出序），无时间依赖。
 *
 * <p>与 AgingPriorityQueue（exec，老化优先队列）同族不同面：
 * 策略面（老化防饿死） vs 结构面（可合并堆）；与
 * DisruptorRingBuffer（spec 5008）不同面：序标环 FIFO vs
 * 优先级出序。
 */
public final class PairingHeap {

    private static final class Node {
        long key;
        Node firstChild;
        Node nextSibling;

        Node(long key) {
            this.key = key;
        }
    }

    private Node root;
    private int size;

    /** 插入（与单节点堆 meld，O(1)）。 */
    public void insert(long key) {
        root = meldNodes(root, new Node(key));
        size++;
    }

    /** 取最小但不弹出（空堆 IAE）。 */
    public long peekMin() {
        requireNonEmpty();
        return root.key;
    }

    /** 弹出最小（孩子两趟合并——先两两配对再依次并回）。 */
    public long extractMin() {
        requireNonEmpty();
        long min = root.key;
        root = twoPassMerge(root.firstChild);
        size--;
        return min;
    }

    /**
     * 合并另一堆（O(1) 根比较挂钩；他堆被清空——所有权
     * 语义，self-meld fail-fast）。
     */
    public void meld(PairingHeap other) {
        if (other == null) {
            throw new IllegalArgumentException("other 非空");
        }
        if (other == this) {
            throw new IllegalArgumentException("不可与自身 meld");
        }
        root = meldNodes(root, other.root);
        size += other.size;
        other.root = null;
        other.size = 0;
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 是否空堆。 */
    public boolean isEmpty() {
        return size == 0;
    }

    private Node meldNodes(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.key <= b.key) {
            b.nextSibling = a.firstChild;
            a.firstChild = b;
            return a;
        }
        a.nextSibling = b.firstChild;
        b.firstChild = a;
        return b;
    }

    private Node twoPassMerge(Node firstChild) {
        if (firstChild == null || firstChild.nextSibling == null) {
            return firstChild;
        }
        java.util.ArrayList<Node> pairs = new java.util.ArrayList<>();
        Node current = firstChild;
        while (current != null && current.nextSibling != null) {
            Node second = current.nextSibling;
            Node after = second.nextSibling;
            current.nextSibling = null;
            second.nextSibling = null;
            pairs.add(meldNodes(current, second));
            current = after;
        }
        if (current != null) {
            pairs.add(current);
        }
        Node merged = pairs.get(pairs.size() - 1);
        for (int i = pairs.size() - 2; i >= 0; i--) {
            merged = meldNodes(pairs.get(i), merged);
        }
        return merged;
    }

    private void requireNonEmpty() {
        if (root == null || size == 0) {
            throw new IllegalArgumentException("空堆不可取/弹");
        }
    }
}
