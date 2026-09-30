package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 重链剖分（spec 9007 / W9015 / impl 2360）——HLD 思想
 * （Heavy-Light Decomposition，树上路径问题线段树化的经典
 * 前置）：**重儿子（子树最大者）延续父链、轻儿子开新链，dfs
 * 序保证每条链在基数组上连续；任意路径被切成 O(log V) 段链
 * 区间**——逐点遍历路径 O(V)/次查询的病解。建剖 O(V)；纯剖解
 * 面：position/head 只读 + pathSegments(u,v) 返回基数组区间
 * 段（升序区间、u 侧向 v 侧）+ lca 副产；边表 n−1 连通无环
 * 校验（自环/多边/断图 fail-fast）；同链同序（同树同剖完全
 * 确定）。
 *
 * <p>与 SegmentTree（metrics 域）组合成树上路径/子树查询闭环
 * （本类只剖不解——区间聚合归消费方）；与 LcaLifting（spec
 * 7013）同域不同面：倍增跳祖先 vs 链顶对齐（后者附赠路径切分）。
 */
public final class HeavyLightDecomposition {

    private final int[] position;
    private final int[] head;
    private final int[] parent;
    private final int[] depth;
    private final int[] order;
    private final int[] size;

    private HeavyLightDecomposition(int[] position, int[] head, int[] parent,
                                    int[] depth, int[] order, int[] size) {
        this.position = position;
        this.head = head;
        this.parent = parent;
        this.depth = depth;
        this.order = order;
        this.size = size;
    }

    /**
     * 建剖（edges 行 = {u,v} 无向树边；根固定入参）。
     *
     * @throws IllegalArgumentException 节点数<1、边数≠n−1、端点越域、自环、非连通/带环
     */
    public static HeavyLightDecomposition of(int nodeCount, int[][] edges, int root) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        if (root < 0 || root >= nodeCount) {
            throw new IllegalArgumentException("根越域（n=" + nodeCount + "）");
        }
        if (edges.length != nodeCount - 1) {
            throw new IllegalArgumentException("树边数须为 n−1（实际 " + edges.length + "）");
        }
        int[] headAdj = new int[nodeCount];
        int[] next = new int[edges.length * 2];
        int[] to = new int[edges.length * 2];
        Arrays.fill(headAdj, -1);
        for (int i = 0; i < edges.length; i++) {
            int u = edges[i][0];
            int v = edges[i][1];
            if (u < 0 || u >= nodeCount || v < 0 || v >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + u + "—" + v + "）");
            }
            if (u == v) {
                throw new IllegalArgumentException("自环不入树（" + u + "）");
            }
            to[i * 2] = v;
            next[i * 2] = headAdj[u];
            headAdj[u] = i * 2;
            to[i * 2 + 1] = u;
            next[i * 2 + 1] = headAdj[v];
            headAdj[v] = i * 2 + 1;
        }
        int[] parent = new int[nodeCount];
        int[] depth = new int[nodeCount];
        int[] size = new int[nodeCount];
        int[] order = new int[nodeCount];
        int[] stack = new int[nodeCount];
        // 第一遍（迭代）：父序/深度/访问序——seen 标记每点恰推一次（环/断图安全）
        DisjointSet dsu = new DisjointSet(nodeCount);
        java.util.Arrays.fill(size, 1);
        boolean[] seen = new boolean[nodeCount];
        parent[root] = -1;
        seen[root] = true;
        int sp = 0;
        stack[sp++] = root;
        int visited = 0;
        while (sp > 0) {
            int u = stack[--sp];
            order[visited++] = u;
            for (int e = headAdj[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (!seen[v]) {
                    seen[v] = true;
                    parent[v] = u;
                    depth[v] = depth[u] + 1;
                    stack[sp++] = v;
                }
            }
        }
        for (int[] edge : edges) {
            dsu.union(edge[0], edge[1]);
        }
        if (visited != nodeCount || dsu.componentCount() != 1) {
            throw new IllegalArgumentException("非连通或带环（访问 " + visited + "/" + nodeCount + "）");
        }
        // 逆访问序累计子树规模
        for (int i = nodeCount - 1; i >= 0; i--) {
            int u = order[i];
            if (parent[u] != -1) {
                size[parent[u]] += size[u];
            }
        }
        // 第二遍：重儿优先的 dfs——链头延续
        int[] position = new int[nodeCount];
        int[] head = new int[nodeCount];
        int[] heavy = new int[nodeCount];
        java.util.Arrays.fill(heavy, -1);
        for (int u : order) {
            int best = -1;
            for (int e = headAdj[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (v != parent[u] && (best == -1 || size[v] > size[best])) {
                    best = v;
                }
            }
            heavy[u] = best;
        }
        int timer = 0;
        sp = 0;
        stack[sp++] = root;
        head[root] = root;
        while (sp > 0) {
            int u = stack[--sp];
            position[u] = timer++;
            // 先压轻儿子、末压重儿（弹栈重儿最先——链连续）
            for (int e = headAdj[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (v != parent[u] && v != heavy[u]) {
                    head[v] = v;
                    stack[sp++] = v;
                }
            }
            if (heavy[u] != -1) {
                head[heavy[u]] = head[u];
                stack[sp++] = heavy[u];
            }
        }
        return new HeavyLightDecomposition(position, head, parent, depth, order, size);
    }

    /** 基数组位（0..n−1 双射）。 */
    public int positionOf(int v) {
        return position[v];
    }

    /** v 所在链的链头。 */
    public int headOf(int v) {
        return head[v];
    }

    /** 最近公共祖先（链顶对齐）。 */
    public int lca(int u, int v) {
        while (head[u] != head[v]) {
            if (depth[head[u]] >= depth[head[v]]) {
                u = parent[head[u]];
            } else {
                v = parent[head[v]];
            }
        }
        return depth[u] <= depth[v] ? u : v;
    }

    /**
     * u→v 路径的基数组区间段（{l,r} 闭区间；u 侧向 v 侧；段内
     * 基数组连续）——O(log V) 段，消费方按段聚合。
     */
    public List<int[]> pathSegments(int u, int v) {
        List<int[]> segments = new ArrayList<>();
        while (head[u] != head[v]) {
            if (depth[head[u]] >= depth[head[v]]) {
                segments.add(new int[]{position[head[u]], position[u]});
                u = parent[head[u]];
            } else {
                segments.add(new int[]{position[head[v]], position[v]});
                v = parent[head[v]];
            }
        }
        if (depth[u] <= depth[v]) {
            segments.add(new int[]{position[u], position[v]});
        } else {
            segments.add(new int[]{position[v], position[u]});
        }
        return segments;
    }
}
