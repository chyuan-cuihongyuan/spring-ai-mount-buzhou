package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Borůvka 森林合并最小生成树（spec 9006 / W9013 / impl 2359）——
 * Borůvka 1926 思想：**每轮所有连通分量同时选各自最小出边、
 * 一次性合并全部（O(log V) 轮×每轮 O(E) 扫描）**——单分量逐步
 * 长大的 Prim 式轮次 O(V) 起步的病解；多分量同步合并天然是
 * 并行 MST 骨架（MapReduce/Parallel MST 思想）。边键
 * (weight,from,to) 全序定胜（同图同树完全确定）；边行
 * {from,to,weight}；不连通 fail-fast（生成树不存在——森林语义
 * 明示不做）；自环/端点越域 fail-fast。
 *
 * <p>与 KruskalMst（spec 7008）同根不同面：边排序扫描+并查集
 * （序列式）vs 全分量同步出边合并（阶段式）；同图同权总量
 * （互证圣像）。
 */
public final class BoruvkaMst {

    private BoruvkaMst() {
    }

    /**
     * MST 边序列（{from,to,weight}，合并轮序+轮内分量根升序——确定）。
     *
     * @throws IllegalArgumentException 节点数<1、自环、端点越域、图不连通
     */
    public static List<long[]> minimumSpanningTree(int nodeCount, int[][] edges) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        int m = edges.length;
        int[] parent = new int[nodeCount];
        for (int v = 0; v < nodeCount; v++) {
            parent[v] = v;
        }
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            if (from < 0 || from >= nodeCount || to < 0 || to >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + from + "—" + to + "）");
            }
            if (from == to) {
                throw new IllegalArgumentException("自环不入树（" + from + "）");
            }
        }
        List<long[]> tree = new ArrayList<>();
        int components = nodeCount;
        long[] best = new long[nodeCount];
        int[] bestEdge = new int[nodeCount];
        while (components > 1) {
            Arrays.fill(bestEdge, -1);
            for (int i = 0; i < m; i++) {
                int rootFrom = find(parent, edges[i][0]);
                int rootTo = find(parent, edges[i][1]);
                if (rootFrom == rootTo) {
                    continue;
                }
                long weight = edges[i][2];
                consider(parent, best, bestEdge, rootFrom, weight, edges[i][0], edges[i][1], i);
                consider(parent, best, bestEdge, rootTo, weight, edges[i][1], edges[i][0], i);
            }
            int merged = 0;
            for (int root = 0; root < nodeCount; root++) {
                int i = bestEdge[root];
                if (i == -1 || find(parent, root) != root) {
                    continue;
                }
                int from = edges[i][0];
                int to = edges[i][1];
                if (find(parent, from) == find(parent, to)) {
                    continue;
                }
                tree.add(new long[]{from, to, edges[i][2]});
                union(parent, from, to);
                merged++;
                components--;
            }
            if (merged == 0) {
                throw new IllegalArgumentException("图不连通（生成树不存在）");
            }
        }
        return List.copyOf(tree);
    }

    /** MST 总权。 */
    public static long totalWeight(int nodeCount, int[][] edges) {
        long total = 0;
        for (long[] edge : minimumSpanningTree(nodeCount, edges)) {
            total += edge[2];
        }
        return total;
    }

    private static void consider(int[] parent, long[] best, int[] bestEdge, int root,
                                 long weight, int from, int to, int edgeIndex) {
        if (bestEdge[root] == -1) {
            bestEdge[root] = edgeIndex;
            best[root] = weight;
            return;
        }
        long bw = best[root];
        if (weight < bw || (weight == bw && less(edgeIndex, bestEdge[root]))) {
            bestEdge[root] = edgeIndex;
            best[root] = weight;
        }
    }

    /** 边键全序：低索引胜（插入序即确定性——同权取先见者）。 */
    private static boolean less(int candidate, int incumbent) {
        return candidate < incumbent;
    }

    private static int find(int[] parent, int v) {
        while (parent[v] != v) {
            parent[v] = parent[parent[v]];
            v = parent[v];
        }
        return v;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[rb] = ra;
        }
    }
}
