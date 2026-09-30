package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 笛卡尔树（spec 9009 / W9019 / impl 2362）——Cartesian tree
 * 思想（Vuillemin 1980）：**值小根堆序 + 下标中序 BST 序的双
 * 不变量树，单调栈 O(n) 一次成型（右链弹栈挂左孩）**——逐点
 * 插入重平衡 O(n log n) 起步的病解。经典用途：RMQ↔LCA 桥
 * （区间最小值 = 笛卡尔树 LCA——CP-algorithms 同源）；并列值
 * 取低下标定胜（同数组同树完全确定）；空数组 fail-fast。
 * 面向查询：parentOf/leftChildOf/rightChildOf/lca（父链对齐）。
 *
 * <p>与 Treap（spec 6002）同构不同源：随机优先级动态平衡 vs
 * 数组值静态一次性成型；与 SparseTable（metrics 域）组合：
 * 本类出 LCA 即得静态 RMQ。
 */
public final class CartesianTree {

    private final int[] parent;
    private final int[] left;
    private final int[] right;
    private final int[] depth;
    private final int[] values;
    private final int root;

    private CartesianTree(int[] parent, int[] left, int[] right, int[] depth,
                          int[] values, int root) {
        this.parent = parent;
        this.left = left;
        this.right = right;
        this.depth = depth;
        this.values = values;
        this.root = root;
    }

    /**
     * O(n) 单调栈建树（并列值取低下标定胜）。
     *
     * @throws IllegalArgumentException null 或空数组
     */
    public static CartesianTree of(int[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("数组非空（建树前提）");
        }
        int n = values.length;
        int[] parent = new int[n];
        int[] left = new int[n];
        int[] right = new int[n];
        java.util.Arrays.fill(parent, -1);
        java.util.Arrays.fill(left, -1);
        java.util.Arrays.fill(right, -1);
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            int last = -1;
            while (!stack.isEmpty() && less(values[i], values[stack.peek()])) {
                last = stack.pop();
            }
            if (last != -1) {
                parent[last] = i;
                left[i] = last;
            }
            if (!stack.isEmpty()) {
                right[stack.peek()] = i;
                parent[i] = stack.peek();
            }
            stack.push(i);
        }
        int root = -1;
        for (int i = 0; i < n; i++) {
            if (parent[i] == -1) {
                root = i;
            }
        }
        int[] depth = new int[n];
        depth[root] = 0;
        // BFS 定深（免长左脊递归栈溢出）
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            if (left[u] != -1) {
                depth[left[u]] = depth[u] + 1;
                queue.add(left[u]);
            }
            if (right[u] != -1) {
                depth[right[u]] = depth[u] + 1;
                queue.add(right[u]);
            }
        }
        return new CartesianTree(parent, left, right, depth, values, root);
    }

    /** 堆序比较：候选值更小才弹栈（同值不弹——低下标定胜）。 */
    private static boolean less(int candidate, int incumbent) {
        return candidate < incumbent;
    }

    /** 根（全数组最小值首个出现位）。 */
    public int root() {
        return root;
    }

    public int parentOf(int v) {
        return parent[v];
    }

    public int leftChildOf(int v) {
        return left[v];
    }

    public int rightChildOf(int v) {
        return right[v];
    }

    /** 最近公共祖先（父链对齐）。 */
    public int lca(int u, int v) {
        while (depth[u] > depth[v]) {
            u = parent[u];
        }
        while (depth[v] > depth[u]) {
            v = parent[v];
        }
        while (u != v) {
            u = parent[u];
            v = parent[v];
        }
        return u;
    }

    /** 区间 [l,r] 最小值下标（RMQ↔LCA 桥：lca(l,r) 即答案）。 */
    public int rangeMinIndex(int l, int r) {
        if (l < 0 || r >= values.length || l > r) {
            throw new IllegalArgumentException("区间越域（[" + l + "," + r + "]）");
        }
        return lca(l, r);
    }
}
