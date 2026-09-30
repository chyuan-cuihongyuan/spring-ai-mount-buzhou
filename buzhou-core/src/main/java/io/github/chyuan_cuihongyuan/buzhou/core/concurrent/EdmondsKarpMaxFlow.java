package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * Edmonds–Karp 最大流（spec 9002 / W9005 / impl 2355）——
 * Edmonds–Karp 1972 思想：**BFS 逐条找最短增广路（弧数最少），
 * 沿路推饱和流量再找下一条**——最短增广保证每条边饱和至多
 * O(V) 次、总 O(VE²)——DFS 盲目找路（可被长路绕行钉成指数
 * 步数）的病解。邻接矩阵容量域 long；流值唯一承诺（流量分布
 * 不唯一——明示）；同点对同图 BFS 取序固定（完全确定）；
 * 节点数≥2/源汇异点/端点越域/负容量 fail-fast。
 *
 * <p>与 DinicMaxFlow（spec 8006）同族不同面：逐条最短路增广
 * vs 分层阶段阻塞流——EK 简单性优先（邻接矩阵直查），Dinic
 * 阶段聚合复杂度更优；两面对同一最大流值（互证圣像）。
 */
public final class EdmondsKarpMaxFlow {

    private EdmondsKarpMaxFlow() {
    }

    /**
     * 最大流值（0..nodeCount-1 编号；edges 行 = {from,to,capacity}）。
     *
     * @throws IllegalArgumentException 节点数<2/端点越域、源汇同点、负容量
     */
    public static long maxFlow(int nodeCount, int[][] edges, int source, int sink) {
        if (nodeCount < 2) {
            throw new IllegalArgumentException("节点数≥2（实际 " + nodeCount + "）");
        }
        if (source < 0 || source >= nodeCount || sink < 0 || sink >= nodeCount) {
            throw new IllegalArgumentException("源/汇越域（n=" + nodeCount + "）");
        }
        if (source == sink) {
            throw new IllegalArgumentException("源汇同点（流语义无定义）");
        }
        long[][] capacity = new long[nodeCount][nodeCount];
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            long cap = edge[2];
            if (from < 0 || from >= nodeCount || to < 0 || to >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + from + "→" + to + "）");
            }
            if (cap < 0) {
                throw new IllegalArgumentException("容量非负（实际 " + cap + "）");
            }
            capacity[from][to] += cap;
        }
        long[][] residual = new long[nodeCount][];
        for (int i = 0; i < nodeCount; i++) {
            residual[i] = Arrays.copyOf(capacity[i], nodeCount);
        }
        long flow = 0;
        int[] parent = new int[nodeCount];
        int[] queue = new int[nodeCount];
        while (true) {
            Arrays.fill(parent, -1);
            parent[source] = source;
            int qt = 0;
            queue[qt++] = source;
            for (int qh = 0; qh < qt && parent[sink] == -1; qh++) {
                int u = queue[qh];
                for (int v = 0; v < nodeCount; v++) {
                    if (parent[v] == -1 && residual[u][v] > 0) {
                        parent[v] = u;
                        queue[qt++] = v;
                    }
                }
            }
            if (parent[sink] == -1) {
                return flow;
            }
            long bottleneck = Long.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v]) {
                bottleneck = Math.min(bottleneck, residual[parent[v]][v]);
            }
            for (int v = sink; v != source; v = parent[v]) {
                residual[parent[v]][v] -= bottleneck;
                residual[v][parent[v]] += bottleneck;
            }
            flow += bottleneck;
        }
    }
}
