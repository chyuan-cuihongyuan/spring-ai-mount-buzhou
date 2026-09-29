package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * Dinic 最大流（spec 8006 / V8013 / impl 2308）——
 * Dinic 1970 思想（网络路由带宽规划同源）：**分层图（BFS
 * 残量分层的最短增广路层级）+ 当前弧阻塞流（DFS 沿层增 1
 * 且失败弧本轮不再重扫）阶段循环**——阶段数 O(V)、稠密
 * 容量网 O(V²E) 一次成型——Edmonds-Karp 增广路轮数 O(VE)
 * （稠密容量网放大）的病解。long 容量域；流值唯一承诺
 * （流量分布不唯一——明示）；边按插入序处理（同图同流值
 * 完全确定）；负容量/源汇同点/节点越界 fail-fast。
 *
 * <p>与 BellmanFord（spec 7006）同族不同面：路径代价 vs
 * 容量承载上限。
 */
public final class DinicMaxFlow {

    private DinicMaxFlow() {
    }

    /**
     * 最大流值（0..nodeCount-1 编号；edges 行 = {from,to,capacity}）。
     *
     * @throws IllegalArgumentException 节点数/端点越界、源汇同点、负容量
     */
    public static long maxFlow(int nodeCount, int[][] edges, int source, int sink) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数非负（实际 " + nodeCount + "）");
        }
        if (source < 0 || source >= nodeCount || sink < 0 || sink >= nodeCount) {
            throw new IllegalArgumentException("源/汇越域（n=" + nodeCount + "）");
        }
        if (source == sink) {
            throw new IllegalArgumentException("源汇同点（流语义无定义）");
        }
        int m = edges.length;
        int[] edgeTo = new int[2 * m];
        long[] cap = new long[2 * m];
        int[] next = new int[2 * m];
        int[] head = new int[nodeCount];
        Arrays.fill(head, -1);
        int ec = 0;
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            long capacity = edge[2];
            if (from < 0 || from >= nodeCount || to < 0 || to >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + from + "→" + to + "）");
            }
            if (capacity < 0) {
                throw new IllegalArgumentException("容量非负（实际 " + capacity + "）");
            }
            edgeTo[ec] = to;
            cap[ec] = capacity;
            next[ec] = head[from];
            head[from] = ec++;
            edgeTo[ec] = from;
            cap[ec] = 0;
            next[ec] = head[to];
            head[to] = ec++;
        }
        long flow = 0;
        int[] level = new int[nodeCount];
        int[] iter = new int[nodeCount];
        int[] queue = new int[nodeCount];
        while (bfsLevels(source, sink, nodeCount, head, edgeTo, cap, next, level, queue)) {
            System.arraycopy(head, 0, iter, 0, nodeCount);
            long pushed;
            while ((pushed = dfs(source, sink, Long.MAX_VALUE, iter, level, edgeTo, cap, next)) > 0) {
                flow += pushed;
            }
        }
        return flow;
    }

    private static boolean bfsLevels(int source, int sink, int n, int[] head, int[] edgeTo,
                                     long[] cap, int[] next, int[] level, int[] queue) {
        Arrays.fill(level, -1);
        level[source] = 0;
        int qt = 0;
        queue[qt++] = source;
        for (int qh = 0; qh < qt; qh++) {
            int u = queue[qh];
            for (int e = head[u]; e != -1; e = next[e]) {
                if (cap[e] > 0 && level[edgeTo[e]] < 0) {
                    level[edgeTo[e]] = level[u] + 1;
                    queue[qt++] = edgeTo[e];
                }
            }
        }
        return level[sink] >= 0;
    }

    private static long dfs(int u, int sink, long limit, int[] iter, int[] level,
                            int[] edgeTo, long[] cap, int[] next) {
        if (u == sink) {
            return limit;
        }
        for (int e = iter[u]; e != -1; e = iter[u]) {
            int v = edgeTo[e];
            if (cap[e] > 0 && level[v] == level[u] + 1) {
                long pushed = dfs(v, sink, Math.min(limit, cap[e]), iter, level, edgeTo, cap, next);
                if (pushed > 0) {
                    cap[e] -= pushed;
                    cap[e ^ 1] += pushed;
                    return pushed;
                }
            }
            iter[u] = next[e];
        }
        return 0;
    }
}
