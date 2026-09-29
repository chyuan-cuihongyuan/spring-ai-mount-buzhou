package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 二项堆（spec 8020 / V8041 / impl 2322）——
 * Vuillemin 1978 思想（可合并堆经典；勘误：原拟
 * FibonacciHeap 复杂度超时预算退雾区——U24 同款纪律，同族
 * 二项堆补位）：**度互异二项树森林——度 k 树恰 2^k 节点、
 * 根最小——合并即二进制进位**（同度两树小根挂大根孩子）
 * O(log n)——二叉堆合并全量重排 O(n)（频繁合并放大）的
 * 病解。merge/offer/poll/peek 四面+size 读数+森林结构审计
 * 面（根表度严格递增+节点数=Σ2^k）；空堆 poll null 诚实
 * 缺省；同操作序同森林形态完全确定。
 *
 * <p>与 LeftistHeap（spec 8019）同族不同面：单树 npl 右
 * 路径 vs 森林二项树进位。
 */
public final class BinomialHeap {

    private static final class Node {
        private long value;
        private Node sibling;
        private Node child;
        private int degree;

        private Node(long value) {
            this.value = value;
        }
    }

    private Node head;
    private int size;

    /** 合并另一堆（搬空对方——所有权转移语义）。 */
    public void merge(BinomialHeap other) {
        if (other == null || other == this) {
            throw new IllegalArgumentException("被合并堆非空且非自身");
        }
        head = union(head, other.head);
        size += other.size;
        other.head = null;
        other.size = 0;
    }

    /** 入队。 */
    public void offer(long value) {
        head = union(head, new Node(value));
        size++;
    }

    /** 弹最小（空堆 null 诚实缺省）。 */
    public Long poll() {
        if (head == null) {
            return null;
        }
        Node minPrev = null;
        Node min = head;
        Node prev = head;
        Node cursor = head.sibling;
        while (cursor != null) {
            if (cursor.value < min.value) {
                min = cursor;
                minPrev = prev;
            }
            prev = cursor;
            cursor = cursor.sibling;
        }
        long value = min.value;
        if (minPrev == null) {
            head = min.sibling;
        } else {
            minPrev.sibling = min.sibling;
        }
        Node reversedChildren = null;
        Node child = min.child;
        while (child != null) {
            Node next = child.sibling;
            child.sibling = reversedChildren;
            reversedChildren = child;
            child = next;
        }
        head = union(head, reversedChildren);
        size--;
        return value;
    }

    /** 看最小（扫根表——空堆 null 诚实缺省）。 */
    public Long peek() {
        if (head == null) {
            return null;
        }
        long min = head.value;
        for (Node cursor = head.sibling; cursor != null; cursor = cursor.sibling) {
            min = Math.min(min, cursor.value);
        }
        return min;
    }

    /** 元素数。 */
    public int size() {
        return size;
    }

    /** 森林结构审计：根表度严格递增且节点数=Σ2^度（测试/自检面）。 */
    public boolean forestInvariantHolds() {
        int total = 0;
        int lastDegree = -1;
        for (Node root = head; root != null; root = root.sibling) {
            if (root.degree <= lastDegree) {
                return false;
            }
            lastDegree = root.degree;
            int nodes = treeNodes(root);
            if (nodes != (1 << root.degree)) {
                return false;
            }
            total += nodes;
        }
        return total == size;
    }

    private int treeNodes(Node root) {
        int count = 1;
        for (Node child = root.child; child != null; child = child.sibling) {
            count += treeNodes(child);
        }
        return count;
    }

    private static Node union(Node first, Node second) {
        Node merged = mergeRootLists(first, second);
        if (merged == null) {
            return null;
        }
        Node resultHead = merged;
        Node prev = null;
        Node current = merged;
        Node next = current.sibling;
        while (next != null) {
            boolean carryThird = next.sibling != null && next.sibling.degree == current.degree;
            if (current.degree != next.degree || carryThird) {
                prev = current;
                current = next;
            } else if (current.value <= next.value) {
                current.sibling = next.sibling;
                attachUnder(current, next);
            } else {
                if (prev == null) {
                    resultHead = next;
                } else {
                    prev.sibling = next;
                }
                attachUnder(next, current);
                current = next;
            }
            next = current.sibling;
        }
        return resultHead;
    }

    private static void attachUnder(Node parent, Node childTree) {
        childTree.sibling = parent.child;
        parent.child = childTree;
        parent.degree++;
    }

    private static Node mergeRootLists(Node first, Node second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        Node head;
        Node tail;
        if (first.degree <= second.degree) {
            head = first;
            first = first.sibling;
        } else {
            head = second;
            second = second.sibling;
        }
        tail = head;
        while (first != null && second != null) {
            if (first.degree <= second.degree) {
                tail.sibling = first;
                first = first.sibling;
            } else {
                tail.sibling = second;
                second = second.sibling;
            }
            tail = tail.sibling;
        }
        tail.sibling = first != null ? first : second;
        return head;
    }
}
