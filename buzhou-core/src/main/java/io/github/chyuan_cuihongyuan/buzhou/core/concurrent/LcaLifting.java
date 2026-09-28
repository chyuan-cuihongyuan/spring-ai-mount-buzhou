package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * 倍增 LCA（spec 7010 / U7221 / impl 2262）——二倍增祖先
 * 跳思想（up[k][u] = u 的 2^k 级祖先，逐层倍增拼表）：
 * 预处理 O(n log n)，单次 LCA/kthAncestor O(log n)——
 * 每次查询沿父链爬升 O(depth)（深树反复查询放大）的病解。
 * 深度对齐 + 同层同步上跳，确定性无随机。构建期校验树形
 * （单亲/可达/无环）fail-fast。
 *
 * <p>与 TopologicalSorter（同包）同族不同面：DAG 序面 vs
 * 树祖先查询面；与 VanEmdeBoas 不同面：有界宇宙后继 vs
 * 无界树 LCA。
 */
public final class LcaLifting {

    private final int nodeCount;
    private final int root;
    private final int[] parentOf;
    private final int[] depth;
    private int[][] up;
    private int levels;

    /** 建树骨架（root 必先于子女登记；越域 fail-fast）。 */
    public LcaLifting(int nodeCount, int root) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        if (root < 0 || root >= nodeCount) {
            throw new IllegalArgumentException("根越域 [0," + nodeCount + "): " + root);
        }
        this.nodeCount = nodeCount;
        this.root = root;
        this.parentOf = new int[nodeCount];
        this.depth = new int[nodeCount];
        Arrays.fill(parentOf, -1);
        parentOf[root] = root;
        depth[root] = 0;
    }

    /** 登记父子边（parent 须已登记；child 单亲/未占用；越域 fail-fast）。 */
    public void addEdge(int parent, int child) {
        requireBuilt("addEdge 前禁止");
        checkNode(parent);
        checkNode(child);
        if (parentOf[parent] == -1) {
            throw new IllegalArgumentException("父节点未登记: " + parent);
        }
        if (parentOf[child] != -1) {
            throw new IllegalArgumentException("子节点已有单亲: " + child);
        }
        if (child == root) {
            throw new IllegalArgumentException("根不得有单亲: " + child);
        }
        parentOf[child] = parent;
        depth[child] = depth[parent] + 1;
    }

    /** 预处理倍增表（O(n log n)；树不完整 fail-fast）。 */
    public void build() {
        requireBuilt("build 前禁止");
        for (int i = 0; i < nodeCount; i++) {
            if (parentOf[i] == -1) {
                throw new IllegalArgumentException("树不完整——节点未达: " + i);
            }
        }
        levels = 1;
        while ((1 << levels) < nodeCount) {
            levels++;
        }
        levels = Math.max(levels, 1);
        up = new int[levels][nodeCount];
        up[0] = parentOf.clone();
        for (int k = 1; k < levels; k++) {
            for (int v = 0; v < nodeCount; v++) {
                up[k][v] = up[k - 1][up[k - 1][v]];
            }
        }
    }

    /** 最近公共祖先（build 前调用 fail-fast）。 */
    public int lca(int u, int v) {
        requireReady();
        checkNode(u);
        checkNode(v);
        if (depth[u] < depth[v]) {
            int t = u;
            u = v;
            v = t;
        }
        u = kthAncestor(u, depth[u] - depth[v]);
        if (u == v) {
            return u;
        }
        for (int k = levels - 1; k >= 0; k--) {
            if (up[k][u] != up[k][v]) {
                u = up[k][u];
                v = up[k][v];
            }
        }
        return up[0][u];
    }

    /** k 级祖先（k>深度 fail-fast——根之上无祖先诚实拒绝）。 */
    public int kthAncestor(int u, int k) {
        requireReady();
        checkNode(u);
        if (k < 0 || k > depth[u]) {
            throw new IllegalArgumentException("k 越深度域 [0," + depth[u] + "]: " + k);
        }
        for (int level = 0; level < levels && k > 0; level++) {
            if ((k & (1 << level)) != 0) {
                u = up[level][u];
                k &= ~(1 << level);
            }
        }
        return u;
    }

    /** 深度读数。 */
    public int depth(int u) {
        requireReady();
        checkNode(u);
        return depth[u];
    }

    /** 两点树上距离（边数）。 */
    public int distance(int u, int v) {
        int ancestor = lca(u, v);
        return depth(u) + depth(v) - 2 * depth(ancestor);
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return nodeCount;
    }

    private void requireBuilt(String where) {
        if (up != null) {
            throw new IllegalArgumentException("build 后不可再 " + where);
        }
    }

    private void requireReady() {
        if (up == null) {
            throw new IllegalArgumentException("build 前查询非法");
        }
    }

    private void checkNode(int node) {
        if (node < 0 || node >= nodeCount) {
            throw new IllegalArgumentException("节点越域 [0," + nodeCount + "): " + node);
        }
    }
}
