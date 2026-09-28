package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * Dijkstra 最短路（spec 6049 / T6297 / impl 2249）——
 * Dijkstra 1959 思想：**非负权单源最短路的贪心已决集扩张**——
 * 每步取未决集中 dist 最小者定影（已决集 dist 永不回改），
 * 松弛沿出边推进——负权边破坏贪心前提（fail-fast 拒绝），
 * 无权 BFS（浪费权重信息）与每次全扫取最小 O(V²)（稠密图
 * 之外的浪费）的病解。堆用同包 IndexedHeap（decrease-key
 * 一等公民——updatePriority 松弛直接降键，O((V+E) log V)）；
 * 堆序确定性（同 dist 按 id 小者先出）——同图同结果。
 * 不可达 = -1（诚实缺省）。
 *
 * <p>与 IndexedHeap（spec 6026）同族不同面：图算法原语 vs
 * 原语的经典消费方（Dijkstra 本尊）；与 TopologicalSorter
 * 不同面：DAG 序 vs 边权最优化。
 */
public final class DijkstraShortestPath {

    private static final class Edge {
        final int to;
        final long weight;

        Edge(int to, long weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    private final int nodeCount;
    private final List<List<Edge>> adjacency;

    /** nodeCount≥1 建空图（越域 fail-fast）。 */
    public DijkstraShortestPath(int nodeCount) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        this.nodeCount = nodeCount;
        this.adjacency = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    /** 加有向边（权重≥0——负权破坏贪心前提 fail-fast；越域 fail-fast）。 */
    public void addEdge(int from, int to, long weight) {
        checkNode(from);
        checkNode(to);
        if (weight < 0) {
            throw new IllegalArgumentException("负权边破坏 Dijkstra 前提: " + weight);
        }
        adjacency.get(from).add(new Edge(to, weight));
    }

    /**
     * 单源最短距离（dist 数组：source=0，不可达=-1）。
     * 贪心已决集扩张 + IndexedHeap decrease-key 松弛。
     */
    public long[] distancesFrom(int source) {
        checkNode(source);
        long[] dist = new long[nodeCount];
        java.util.Arrays.fill(dist, -1L);
        dist[source] = 0L;
        IndexedHeap heap = new IndexedHeap();
        heap.push(source, 0L);
        while (!heap.isEmpty()) {
            int settled = (int) heap.popMin();
            long settledDist = dist[settled];
            for (Edge edge : adjacency.get(settled)) {
                long candidate = settledDist + edge.weight;
                int next = edge.to;
                if (dist[next] == -1L) {
                    dist[next] = candidate;
                    heap.push(next, candidate);
                } else if (candidate < dist[next]) {
                    dist[next] = candidate;
                    if (heap.contains(next)) {
                        heap.updatePriority(next, candidate);
                    } else {
                        heap.push(next, candidate);
                    }
                }
            }
        }
        return dist;
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return nodeCount;
    }

    /** 边数读数。 */
    public int edgeCount() {
        int total = 0;
        for (List<Edge> edges : adjacency) {
            total += edges.size();
        }
        return total;
    }

    private void checkNode(int node) {
        if (node < 0 || node >= nodeCount) {
            throw new IllegalArgumentException("节点越域 [0," + nodeCount + "): " + node);
        }
    }
}
