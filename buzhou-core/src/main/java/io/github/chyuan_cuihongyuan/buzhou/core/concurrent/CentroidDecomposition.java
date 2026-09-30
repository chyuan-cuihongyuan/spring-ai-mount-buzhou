package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 重心剖分（spec 9008 / W9017 / impl 2361）——点分治思想
 * （Centroid Decomposition，树上路径统计分治经典前置）：**
 * 每次取连通块重心（删去后最大子块最小）为块代表，重心递归
 * 分块成「重心树」——树高 O(log V)，任何路径过某重心被统计
 * 恰一次**——整树逐对扫描 O(V²) 起步的病解。纯剖解面：
 * centroidParent 只读 + rootCentroid + componentSizeOf +
 * childrenInCentroidTree；同块同取序（重儿并列取最低编号——
 * 同树同剖完全确定）；边数≠n−1/自环/端点越域/非连通带环
 * fail-fast。
 *
 * <p>与 HeavyLightDecomposition（spec 9007）同域不同面：链剖
 * （路径→区间）vs 重心剖（路径→分治重心）；与
 * LcaLifting（spec 7013）互补：跳祖先 vs 重心分治。
 */
public final class CentroidDecomposition {

    private final int[] centroidParent;
    private final int[] componentSize;
    private final int root;

    private CentroidDecomposition(int[] centroidParent, int[] componentSize, int root) {
        this.centroidParent = centroidParent;
        this.componentSize = componentSize;
        this.root = root;
    }

    /**
     * 建剖（edges 行 = {u,v} 无向树边；块内取序确定）。
     *
     * @throws IllegalArgumentException 节点数<1、边数≠n−1、端点越域、自环、非连通/带环
     */
    public static CentroidDecomposition of(int nodeCount, int[][] edges) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        if (edges.length != nodeCount - 1) {
            throw new IllegalArgumentException("树边数须为 n−1（实际 " + edges.length + "）");
        }
        int[] head = new int[nodeCount];
        int[] next = new int[edges.length * 2];
        int[] to = new int[edges.length * 2];
        Arrays.fill(head, -1);
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
            next[i * 2] = head[u];
            head[u] = i * 2;
            to[i * 2 + 1] = u;
            next[i * 2 + 1] = head[v];
            head[v] = i * 2 + 1;
        }
        boolean[] removed = new boolean[nodeCount];
        int[] parent = new int[nodeCount];
        int[] size = new int[nodeCount];
        int[] centroidParent = new int[nodeCount];
        int[] componentSize = new int[nodeCount];
        Arrays.fill(centroidParent, -1);
        int[] rootBox = {-1};
        decompose(0, -1, nodeCount, head, next, to, removed, parent, size,
                centroidParent, componentSize, rootBox);
        return new CentroidDecomposition(centroidParent, componentSize, rootBox[0]);
    }

    /** 重心树根（首块重心）。 */
    public int rootCentroid() {
        return root;
    }

    /** v 在重心树中的父（根为 -1）。 */
    public int parentInCentroidTree(int v) {
        return centroidParent[v];
    }

    /** v 作为重心时的块大小（根块 = n）。 */
    public int componentSizeOf(int v) {
        return componentSize[v];
    }

    /** v 在重心树中的孩子（升序）。 */
    public List<Integer> childrenInCentroidTree(int v) {
        List<Integer> children = new ArrayList<>();
        for (int u = 0; u < centroidParent.length; u++) {
            if (centroidParent[u] == v) {
                children.add(u);
            }
        }
        return children;
    }

    private static void decompose(int entry, int parentCentroid, int totalNodes, int[] head,
                                  int[] next, int[] to, boolean[] removed, int[] parent, int[] size,
                                  int[] centroidParent, int[] componentSize, int[] rootBox) {
        // 以 entry 为根累计存活块大小
        java.util.Arrays.fill(size, 0);
        java.util.Arrays.fill(parent, -1);
        int[] stack = new int[totalNodes];
        int sp = 0;
        stack[sp++] = entry;
        parent[entry] = entry;
        int alive = 0;
        int[] order = new int[totalNodes];
        int ordered = 0;
        while (sp > 0) {
            int u = stack[--sp];
            size[u] = 1;
            order[ordered++] = u;
            alive++;
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (!removed[v] && parent[v] == -1) {
                    parent[v] = u;
                    stack[sp++] = v;
                }
            }
        }
        for (int i = ordered - 1; i >= 0; i--) {
            int u = order[i];
            if (parent[u] != u) {
                size[parent[u]] += size[u];
            }
        }
        // 找重心：最大剩余子块最小（并列取最低编号）
        int centroid = -1;
        int bestMax = Integer.MAX_VALUE;
        for (int i = 0; i < ordered; i++) {
            int u = order[i];
            int maxPiece = alive - size[u];
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (!removed[v] && parent[v] == u) {
                    maxPiece = Math.max(maxPiece, size[v]);
                }
            }
            if (maxPiece < bestMax) {
                bestMax = maxPiece;
                centroid = u;
            }
        }
        componentSize[centroid] = alive;
        centroidParent[centroid] = parentCentroid;
        if (parentCentroid == -1) {
            rootBox[0] = centroid;
        }
        removed[centroid] = true;
        for (int e = head[centroid]; e != -1; e = next[e]) {
            int v = to[e];
            if (!removed[v]) {
                decompose(v, centroid, totalNodes, head, next, to, removed, parent, size,
                        centroidParent, componentSize, rootBox);
            }
        }
    }
}
