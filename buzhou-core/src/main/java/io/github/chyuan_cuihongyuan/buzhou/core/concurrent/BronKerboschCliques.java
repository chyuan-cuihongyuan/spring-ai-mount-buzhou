package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * Bron–Kerbosch 极大团枚举（spec 9004 / W9009 / impl 2357）——
 * Bron–Kerbosch 1973 + Tomita 2006 pivot 思想：**三集递归
 * （R 固定部/P 候选部/X 排除部）+ pivot 剪枝（从 P∪X 选 P 中
 * 邻居最多者，只递归 P∖N(pivot)）**——最坏 3^(n/3) 但 pivot
 * 实战最省分支——全子集扫描判极大（2^n 子集×团判定）的病解。
 * 无向边 {u,v}（u≠v、重复边归一）；每团内部升序、枚举序
 * 确定（同图同输出完全确定）；自环拒绝/端点越域 fail-fast。
 *
 * <p>与 TarjanSccFinder（spec 7020）同域不同面：强连通分量
 * （有向可达）vs 极大团（无向两两相邻）；与 HopcroftKarpMatcher
 * （spec 9001）对偶：团 = 补图独立集。
 */
public final class BronKerboschCliques {

    private BronKerboschCliques() {
    }

    /**
     * 全部极大团（每团顶点升序；枚举序确定）。
     *
     * @throws IllegalArgumentException 自环、端点越域
     */
    public static List<int[]> maximalCliques(int nodeCount, int[][] edges) {
        if (nodeCount < 0) {
            throw new IllegalArgumentException("节点数非负（实际 " + nodeCount + "）");
        }
        if (nodeCount == 0) {
            return List.of();
        }
        boolean[][] adj = new boolean[nodeCount][nodeCount];
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            if (u < 0 || u >= nodeCount || v < 0 || v >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + u + "—" + v + "）");
            }
            if (u == v) {
                throw new IllegalArgumentException("自环不入团（" + u + "）");
            }
            adj[u][v] = true;
            adj[v][u] = true;
        }
        List<int[]> cliques = new ArrayList<>();
        boolean[] all = new boolean[nodeCount];
        for (int v = 0; v < nodeCount; v++) {
            all[v] = true;
        }
        expand(new int[0], all, new boolean[nodeCount], adj, cliques, nodeCount);
        return cliques;
    }

    private static void expand(int[] r, boolean[] p, boolean[] x, boolean[][] adj,
                               List<int[]> cliques, int n) {
        boolean pEmpty = true;
        boolean xEmpty = true;
        for (int v = 0; v < n; v++) {
            if (p[v]) {
                pEmpty = false;
            }
            if (x[v]) {
                xEmpty = false;
            }
        }
        if (pEmpty && xEmpty) {
            cliques.add(r);
            return;
        }
        // pivot：P∪X 中 P 邻居最多者（同数取最低编号）
        int pivot = -1;
        int pivotDegree = -1;
        for (int u = 0; u < n; u++) {
            if (p[u] || x[u]) {
                int degree = 0;
                for (int v = 0; v < n; v++) {
                    if (p[v] && adj[u][v]) {
                        degree++;
                    }
                }
                if (degree > pivotDegree) {
                    pivotDegree = degree;
                    pivot = u;
                }
            }
        }
        for (int v = 0; v < n; v++) {
            if (p[v] && !adj[pivot][v]) {
                boolean[] nextP = new boolean[n];
                boolean[] nextX = new boolean[n];
                for (int w = 0; w < n; w++) {
                    nextP[w] = p[w] && adj[v][w];
                    nextX[w] = x[w] && adj[v][w];
                }
                int[] nextR = new int[r.length + 1];
                int pos = 0;
                int insert = 0;
                while (pos < r.length && r[pos] < v) {
                    nextR[insert++] = r[pos++];
                }
                nextR[insert++] = v;
                while (pos < r.length) {
                    nextR[insert++] = r[pos++];
                }
                expand(nextR, nextP, nextX, adj, cliques, n);
                p[v] = false;
                x[v] = true;
            }
        }
    }
}
