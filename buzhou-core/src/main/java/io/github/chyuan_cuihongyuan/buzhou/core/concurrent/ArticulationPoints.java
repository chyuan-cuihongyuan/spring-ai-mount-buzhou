package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 割点/桥检测（spec 7009 / U7219 / impl 2261）——Tarjan
 * 低链接思想（Hopcroft 1973 DFS 一次遍历）：**disc/low
 * 回溯**——low[child]≥disc[u] 则 u 为割点（子树无法绕过
 * u）、low[child]>disc[u] 则边 (u,child) 为桥——离线一次
 * O(V+E)。根节点特判（两个孩子即割点）。邻接按 id 升序
 * 遍历——同图同结果完全确定。移除割点才连通性下降的图
 * 审计面（单点故障识别）。
 *
 * <p>与 TarjanSccFinder（同包）同族不同面：有向强连通
 * 分量 vs 无向割点桥（低链接判据不同面）。
 */
public final class ArticulationPoints {

    private final int nodeCount;
    private final Map<Integer, TreeSet<Integer>> adjacency = new HashMap<>();
    private int edgeCount;

    /** nodeCount≥1（越域 fail-fast）。 */
    public ArticulationPoints(int nodeCount) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        this.nodeCount = nodeCount;
        for (int i = 0; i < nodeCount; i++) {
            adjacency.put(i, new TreeSet<>());
        }
    }

    /** 加无向边（自环 fail-fast；重边幂等——重边不产生桥语义）。 */
    public void addEdge(int a, int b) {
        checkNode(a);
        checkNode(b);
        if (a == b) {
            throw new IllegalArgumentException("自环不参与连通审计: " + a);
        }
        if (adjacency.get(a).add(b)) {
            adjacency.get(b).add(a);
            edgeCount++;
        }
    }

    /** 全部割点（升序——确定性）。 */
    public List<Integer> articulationPoints() {
        int[] disc = new int[nodeCount];
        int[] low = new int[nodeCount];
        int[] parent = new int[nodeCount];
        java.util.Arrays.fill(disc, -1);
        java.util.Arrays.fill(parent, -1);
        boolean[] isCut = new boolean[nodeCount];
        int[] timer = {0};
        for (int start = 0; start < nodeCount; start++) {
            if (disc[start] == -1) {
                dfs(start, disc, low, parent, isCut, timer);
            }
        }
        List<Integer> cuts = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            if (isCut[i]) {
                cuts.add(i);
            }
        }
        return cuts;
    }

    /** 全部桥（[小端,大端] 升序——确定性）。 */
    public List<long[]> bridges() {
        int[] disc = new int[nodeCount];
        int[] low = new int[nodeCount];
        int[] parent = new int[nodeCount];
        java.util.Arrays.fill(disc, -1);
        java.util.Arrays.fill(parent, -1);
        boolean[] ignored = new boolean[nodeCount];
        List<long[]> bridgeList = new ArrayList<>();
        int[] timer = {0};
        for (int start = 0; start < nodeCount; start++) {
            if (disc[start] == -1) {
                dfsBridge(start, disc, low, parent, ignored, bridgeList, timer);
            }
        }
        bridgeList.sort((x, y) -> x[0] != y[0]
                ? Long.compare(x[0], y[0]) : Long.compare(x[1], y[1]));
        return bridgeList;
    }

    private void dfs(int u, int[] disc, int[] low, int[] parent,
            boolean[] isCut, int[] timer) {
        disc[u] = low[u] = ++timer[0];
        int children = 0;
        for (int v : adjacency.get(u)) {
            if (disc[v] == -1) {
                children++;
                parent[v] = u;
                dfs(v, disc, low, parent, isCut, timer);
                low[u] = Math.min(low[u], low[v]);
                if (parent[u] == -1) {
                    if (children >= 2) {
                        isCut[u] = true;
                    }
                } else if (low[v] >= disc[u]) {
                    isCut[u] = true;
                }
            } else if (v != parent[u]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    private void dfsBridge(int u, int[] disc, int[] low, int[] parent,
            boolean[] ignored, List<long[]> bridgeList, int[] timer) {
        disc[u] = low[u] = ++timer[0];
        for (int v : adjacency.get(u)) {
            if (disc[v] == -1) {
                parent[v] = u;
                dfsBridge(v, disc, low, parent, ignored, bridgeList, timer);
                low[u] = Math.min(low[u], low[v]);
                if (low[v] > disc[u]) {
                    bridgeList.add(new long[]{Math.min(u, v), Math.max(u, v)});
                }
            } else if (v != parent[u]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return nodeCount;
    }

    /** 边数读数（去重后）。 */
    public int edgeCount() {
        return edgeCount;
    }

    private void checkNode(int node) {
        if (node < 0 || node >= nodeCount) {
            throw new IllegalArgumentException("节点越域 [0," + nodeCount + "): " + node);
        }
    }
}
