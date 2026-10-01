package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * 双 DFS 强连通分量（spec 10030 / X10061 / impl 2433）——Kosaraju–Sharir 思想
 * （CLRS 22.5 同源）：**第一遍在正图按顶点升序起扫迭代 DFS 压完成序，第二遍在
 * 逆图按完成序逆序 DFS 收桶**——逆图上每棵搜索树恰为一个 SCC（凝聚图定理）。
 * 与 TarjanSccFinder（已占）同域不同面：两次平凡 DFS 完成序 vs 单次 DFS lowlink。
 *
 * <p>确定性契约：分量内顶点升序、分量表按最小顶点升序（可复算圣像）；不修改
 * 输入边表。null 边表/null 边/顶点越界 fail-fast。自环与重边按平凡语义参与
 * （自环顶点仍为所在分量成员）。
 */
public final class KosarajuScc {

    private KosarajuScc() {
    }

    /**
     * 强连通分量表（每分量顶点升序，分量按最小顶点升序）。
     *
     * @param vertexCount 顶点数（顶点域 0..vertexCount−1）
     * @param edges 有向边 from→to（int[2]；可含自环/重边）
     * @return 分量表（不可变外层、分量升序）
     * @throws IllegalArgumentException null 边表/null 边/端点越界/负顶点数
     */
    public static List<List<Integer>> components(int vertexCount, List<int[]> edges) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("顶点数非负（实际 " + vertexCount + "）");
        }
        if (edges == null) {
            throw new IllegalArgumentException("边表非 null");
        }
        validateEdges(vertexCount, edges);
        List<List<Integer>> forward = adjacency(vertexCount, edges, false);
        List<List<Integer>> backward = adjacency(vertexCount, edges, true);
        int[] finishOrder = finishOrder(vertexCount, forward);
        boolean[] visited = new boolean[vertexCount];
        List<List<Integer>> components = new ArrayList<>();
        for (int i = vertexCount - 1; i >= 0; i--) {
            int root = finishOrder[i];
            if (!visited[root]) {
                List<Integer> component = collect(root, backward, visited);
                component.sort(Comparator.naturalOrder());
                components.add(component);
            }
        }
        components.sort(Comparator.comparingInt(c -> c.get(0)));
        return List.copyOf(components);
    }

    private static void validateEdges(int vertexCount, List<int[]> edges) {
        for (int i = 0; i < edges.size(); i++) {
            int[] edge = edges.get(i);
            if (edge == null || edge.length != 2) {
                throw new IllegalArgumentException("边须为 int[2]（第 " + i + " 条）");
            }
            requireInUniverse(vertexCount, edge[0], i);
            requireInUniverse(vertexCount, edge[1], i);
        }
    }

    private static void requireInUniverse(int vertexCount, int vertex, int edgeIndex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IllegalArgumentException(
                    "顶点越界（第 " + edgeIndex + " 条边端点 " + vertex
                    + "，域 [0," + (vertexCount - 1) + "]）");
        }
    }

    /** 邻接表（reverse=true 建逆图）。 */
    private static List<List<Integer>> adjacency(int vertexCount, List<int[]> edges,
            boolean reverse) {
        List<List<Integer>> adjacency = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            int from = reverse ? edge[1] : edge[0];
            int to = reverse ? edge[0] : edge[1];
            adjacency.get(from).add(to);
        }
        return adjacency;
    }

    /**
     * 迭代 DFS 完成序（显式栈+每顶点邻接游标——深图无递归溢出）；
     * 顶点升序起扫保证确定性。
     */
    private static int[] finishOrder(int vertexCount, List<List<Integer>> adjacency) {
        boolean[] visited = new boolean[vertexCount];
        int[] cursor = new int[vertexCount];
        int[] stack = new int[vertexCount];
        int[] order = new int[vertexCount];
        int filled = 0;
        for (int source = 0; source < vertexCount; source++) {
            if (visited[source]) {
                continue;
            }
            int top = 0;
            stack[top] = source;
            visited[source] = true;
            while (top >= 0) {
                int vertex = stack[top];
                List<Integer> neighbors = adjacency.get(vertex);
                if (cursor[vertex] < neighbors.size()) {
                    int next = neighbors.get(cursor[vertex]++);
                    if (!visited[next]) {
                        visited[next] = true;
                        stack[++top] = next;
                    }
                } else {
                    order[filled++] = vertex;
                    top--;
                }
            }
        }
        return order;
    }

    /** 逆图上从 root 收一个分量（栈式 DFS）。 */
    private static List<Integer> collect(int root, List<List<Integer>> reverseAdjacency,
            boolean[] visited) {
        List<Integer> component = new ArrayList<>();
        Deque<Integer> stack = new ArrayDeque<>();
        visited[root] = true;
        stack.push(root);
        while (!stack.isEmpty()) {
            int vertex = stack.pop();
            component.add(vertex);
            for (int next : reverseAdjacency.get(vertex)) {
                if (!visited[next]) {
                    visited[next] = true;
                    stack.push(next);
                }
            }
        }
        return component;
    }
}
