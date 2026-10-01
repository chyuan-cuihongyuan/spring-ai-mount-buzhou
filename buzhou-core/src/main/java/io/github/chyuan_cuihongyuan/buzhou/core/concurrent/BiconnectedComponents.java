package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * 双连通分量（spec 11018 / Y11037 / impl 2471）——Tarjan 1972 思想
 * （Hopcroft–Tarjan 同源；KosarajuScc 已占异面：有向 SCC vs 无向 BCC）：
 * **DFS disc/low+显式边栈——回边压栈、low[child]≥disc[u] 触发弹栈至当前边
 * 成块**——无向图 2-边连通块划分（割点删除后的块结构；边集划分承诺：
 * Σ块边数=总边数不重不漏）。
 *
 * <p>孤立顶点不产出（无边即无块）；重边按独立边参与；null/越界 fail-fast；
 * 复算确定（邻接插入序）。
 */
public final class BiconnectedComponents {

    private BiconnectedComponents() {
    }

    /**
     * 双连通分量表（每块为边集 {from,to} 列表）。
     *
     * @param vertexCount 顶点数（0..n−1）
     * @param edges 无向边 {from,to}
     * @throws IllegalArgumentException null/越界
     */
    public static List<List<int[]>> components(int vertexCount, int[][] edges) {
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
        Deque<Integer> edgeStack = new ArrayDeque<>();
        List<List<int[]>> components = new ArrayList<>();
        int counter = 0;
        for (int root = 0; root < vertexCount; root++) {
            if (disc[root] != -1) {
                continue;
            }
            counter = dfs(root, -1, disc, low, adjacency, edges, edgeStack,
                    components, counter);
        }
        return components;
    }

    /** 迭代 DFS（显栈：顶点+父边+邻接游标）。 */
    private static int dfs(int root, int unusedParent, int[] disc, int[] low,
            List<List<Integer>> adjacency, int[][] edges, Deque<Integer> edgeStack,
            List<List<int[]>> components, int counter) {
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
                    edgeStack.push(edgeId);
                    disc[other] = low[other] = counter++;
                    stack[++top] = other;
                    parentEdge[top] = edgeId;
                } else if (edgeId != parentEdge[top] && disc[other] < disc[vertex]) {
                    // 回边只从后发现侧压栈一次（前向侧跳过——不重复成块）
                    edgeStack.push(edgeId);
                    low[vertex] = Math.min(low[vertex], disc[other]);
                }
            } else {
                top--;
                if (top < 0) {
                    continue;
                }
                int parent = stack[top];
                low[parent] = Math.min(low[parent], low[vertex]);
                if (low[vertex] >= disc[parent]) {
                    List<int[]> component = new ArrayList<>();
                    while (!edgeStack.isEmpty()) {
                        int edgeId = edgeStack.pop();
                        component.add(new int[]{edges[edgeId][0], edges[edgeId][1]});
                        if (edgeId == parentEdge[top + 1]) {
                            break;
                        }
                    }
                    components.add(component);
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
