package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * Bellman-Ford 最短路（spec 7006 / U7213 / impl 2258）——
 * Bellman 1958 / Ford 1956 思想：**全边松弛 n−1 轮**——
 * 支持负权边（Dijkstra 贪心前提不满足的域），第 n 轮仍可
 * 松弛即**可达负环 fail-fast**（距离语义无定义，诚实拒绝
 * 而非吐出振荡值）——负权图错用 Dijkstra（贪心定影被负边
 * 回改，结果错误且无声）的病解。不可达 = {@link #UNREACHABLE}（负权下 -1 会与真实距离冲突——专用哨兵，勘误入档）。
 *
 * <p>与 DijkstraShortestPath（同包）同族不同面：非负权
 * 贪心已决集 O((V+E) log V) vs 负权容忍全松弛 O(VE)+
 * 负环检测。
 */
public final class BellmanFord {

    private static final class Edge {
        final int from;
        final int to;
        final long weight;

        Edge(int from, int to, long weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }

    /** 不可达哨兵（负权下 -1 会与真实距离冲突——专用哨兵）。 */
    public static final long UNREACHABLE = Long.MIN_VALUE / 2;

    private final int nodeCount;
    private final List<Edge> edges = new ArrayList<>();

    /** nodeCount≥1（越域 fail-fast）。 */
    public BellmanFord(int nodeCount) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        this.nodeCount = nodeCount;
    }

    /** 加有向边（负权合法；越域 fail-fast）。 */
    public void addEdge(int from, int to, long weight) {
        checkNode(from);
        checkNode(to);
        edges.add(new Edge(from, to, weight));
    }

    /**
     * 单源最短距离（source=0，不可达=-1；**可达负环
     * fail-fast**——距离无定义，诚实拒绝）。
     */
    public long[] distancesFrom(int source) {
        checkNode(source);
        long[] dist = new long[nodeCount];
        boolean[] reached = new boolean[nodeCount];
        java.util.Arrays.fill(dist, UNREACHABLE);
        reached[source] = true;
        dist[source] = 0L;
        for (int round = 0; round < nodeCount - 1; round++) {
            boolean changed = false;
            for (Edge edge : edges) {
                if (!reached[edge.from]) {
                    continue;
                }
                long candidate = dist[edge.from] + edge.weight;
                if (!reached[edge.to] || candidate < dist[edge.to]) {
                    reached[edge.to] = true;
                    dist[edge.to] = candidate;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        for (Edge edge : edges) {
            if (reached[edge.from]) {
                long candidate = dist[edge.from] + edge.weight;
                if (!reached[edge.to] || candidate < dist[edge.to]) {
                    throw new IllegalArgumentException("可达负环——距离无定义");
                }
            }
        }
        return dist;
    }

    /** 全图是否存在负环（虚拟超源全零起跑——与单源可达性无关）。 */
    public boolean hasNegativeCycle() {
        long[] dist = new long[nodeCount];
        for (int round = 0; round < nodeCount; round++) {
            boolean changed = false;
            for (Edge edge : edges) {
                long candidate = dist[edge.from] + edge.weight;
                if (candidate < dist[edge.to]) {
                    dist[edge.to] = candidate;
                    changed = true;
                }
            }
            if (!changed) {
                return false;
            }
        }
        for (Edge edge : edges) {
            if (dist[edge.from] + edge.weight < dist[edge.to]) {
                return true;
            }
        }
        return false;
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return nodeCount;
    }

    /** 边数读数。 */
    public int edgeCount() {
        return edges.size();
    }

    private void checkNode(int node) {
        if (node < 0 || node >= nodeCount) {
            throw new IllegalArgumentException("节点越域 [0," + nodeCount + "): " + node);
        }
    }
}
