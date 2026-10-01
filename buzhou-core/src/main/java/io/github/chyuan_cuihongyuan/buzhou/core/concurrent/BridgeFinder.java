package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 桥检测（spec 11019 / Y11039 / impl 2472）——Tarjan 1974 思想（网络可靠性
 * 同源；BiconnectedComponents 已占异面：割边列表 vs 块划分）：**DFS disc/low
 * 树边 low[child]>disc[u] 即桥**——删除后图不连通的关键边。重边非桥口径
 * （平行边成回边——父边仅跳一条）。
 *
 * <p>孤立顶点无涉；null/越界 fail-fast；复算确定（邻接插入序+输出升序）。
 */
public final class BridgeFinder {

    private BridgeFinder() {
    }

    /**
     * 桥列表（{from,to} 字典序升序）。
     *
     * @param vertexCount 顶点数（0..n−1）
     * @param edges 无向边 {from,to}
     * @throws IllegalArgumentException null/越界
     */
    public static List<int[]> bridges(int vertexCount, int[][] edges) {
        validate(vertexCount, edges);
        List<List<Integer>> adjacency = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int i = 0; i < edges.length; i++) {
            adjacency.get(edges[i][0]).add(i);
            adjacency.get(edges[i][1]).add(i);
        }
        int[] disc = new int[vertexCount];
        int[] low = new int[vertexCount];
        Arrays.fill(disc, -1);
        List<Integer> bridgeIds = new ArrayList<>();
        int counter = 0;
        for (int root = 0; root < vertexCount; root++) {
            if (disc[root] != -1) {
                continue;
            }
            counter = dfs(root, -1, disc, low, adjacency, edges, bridgeIds, counter);
        }
        List<int[]> bridges = new ArrayList<>();
        for (int edgeId : bridgeIds) {
            int from = Math.min(edges[edgeId][0], edges[edgeId][1]);
            int to = Math.max(edges[edgeId][0], edges[edgeId][1]);
            bridges.add(new int[]{from, to});
        }
        bridges.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                : Integer.compare(a[1], b[1]));
        return bridges;
    }

    /** 迭代 DFS（显栈：顶点+父边+邻接游标）。 */
    private static int dfs(int root, int unusedParent, int[] disc, int[] low,
            List<List<Integer>> adjacency, int[][] edges, List<Integer> bridgeIds,
            int counter) {
        int[] stack = new int[disc.length];
        int[] parentEdge = new int[disc.length];
        int[] cursor = new int[disc.length];
        int top = 0;
        stack[top] = root;
        parentEdge[top] = -1;
        disc[root] = low[root] = counter++;
        while (top >= 0) {
            int vertex = stack[top];
            List<Integer> incident = adjacency.get(vertex);
            if (cursor[vertex] < incident.size()) {
                int edgeId = incident.get(cursor[vertex]++);
                int other = edges[edgeId][0] == vertex ? edges[edgeId][1] : edges[edgeId][0];
                if (disc[other] == -1) {
                    disc[other] = low[other] = counter++;
                    stack[++top] = other;
                    parentEdge[top] = edgeId;
                } else if (edgeId != parentEdge[top] && disc[other] < disc[vertex]) {
                    low[vertex] = Math.min(low[vertex], disc[other]);
                }
            } else {
                top--;
                if (top < 0) {
                    continue;
                }
                int parent = stack[top];
                low[parent] = Math.min(low[parent], low[vertex]);
                if (low[vertex] > disc[parent]) {
                    bridgeIds.add(parentEdge[top + 1]);
                }
            }
        }
        return counter;
    }

    private static void validate(int vertexCount, int[][] edges) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("顶点数非负（实际 " + vertexCount + "）");
        }
        if (edges == null) {
            throw new IllegalArgumentException("边表非 null");
        }
        for (int i = 0; i < edges.length; i++) {
            int[] edge = edges[i];
            if (edge == null || edge.length != 2) {
                throw new IllegalArgumentException("边须为 int[2]（第 " + i + " 条）");
            }
            if (edge[0] < 0 || edge[0] >= vertexCount || edge[1] < 0
                    || edge[1] >= vertexCount) {
                throw new IllegalArgumentException("边端点越界（第 " + i + " 条 "
                        + edge[0] + "→" + edge[1] + "）");
            }
        }
    }
}
