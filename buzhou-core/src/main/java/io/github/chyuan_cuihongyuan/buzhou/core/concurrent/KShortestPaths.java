package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Yen 偏离 K 最短路（spec 10033 / X10067 / impl 2436）——Yen 1971 思想
 * （networkx/PG Routing 同源）：**确定性 Dijkstra 求首路，再对已取路径逐
 * spur 节点禁共享边+禁根点、以偏离子路拼根成候选，候选池择小逐条产出**——
 * 单最短路（DijkstraShortestPath 已占）的次优备选面（绕行/备选路由）。
 *
 * <p>平行同向边取最小口径：同 (u,v) 对多边并入最小权边——路径节点序不变、
 * K 最短权重面不受损（任一多重边路径可被最小权平行边替换不增重）。
 * 确定性契约：Dijkstra 堆 (dist,node) 并列按点号、候选池 (权重,节点序
 * 字典序) 择小，可复算圣像。Path(totalWeight,nodes) 不可变 record；不足
 * k 条如数返回。null 边表/负权重/非正 k/源汇同点/越界 fail-fast。
 */
public final class KShortestPaths {

    private KShortestPaths() {
    }

    /** 一条路径：总权重 + 节点序（不可变）。 */
    public record Path(long totalWeight, int[] nodes) {
    }

    /**
     * K 条最短路（总权重升序；权重并列按节点序字典序）。
     *
     * @param nodeCount 节点数（0..n−1）
     * @param edges 有向边 {from,to,weight}（权重非负；平行同向边取最小）
     * @param source 源
     * @param sink 汇
     * @param k 需求条数（≥1）
     * @throws IllegalArgumentException null 边表/负权重/非正 k/源汇同点/端点越界
     */
    public static List<Path> kShortest(int nodeCount, int[][] edges, int source,
            int sink, int k) {
        validate(nodeCount, edges, source, sink, k);
        Graph graph = Graph.of(nodeCount, edges);
        List<Path> accepted = new ArrayList<>();
        Path first = graph.dijkstra(source, sink, null, null);
        if (first == null) {
            return List.of();
        }
        accepted.add(first);
        java.util.Set<String> acceptedKeys = new java.util.HashSet<>();
        acceptedKeys.add(pathKey(first));
        PriorityQueue<Path> candidates = new PriorityQueue<>(
                java.util.Comparator.comparingLong(Path::totalWeight)
                        .thenComparing((p, q) -> Arrays.compare(p.nodes(), q.nodes())));
        while (accepted.size() < k) {
            Path previous = accepted.get(accepted.size() - 1);
            for (int spurIndex = 0; spurIndex < previous.nodes().length - 1; spurIndex++) {
                int spurNode = previous.nodes()[spurIndex];
                int[] root = Arrays.copyOf(previous.nodes(), spurIndex + 1);
                boolean[] bannedNodes = new boolean[nodeCount];
                for (int i = 0; i < spurIndex; i++) {
                    bannedNodes[root[i]] = true;
                }
                boolean[] bannedEdges = new boolean[graph.edgeCount];
                for (Path taken : accepted) {
                    if (sharesPrefix(taken.nodes(), root)) {
                        graph.banEdgeFrom(spurIndex, taken, bannedEdges);
                    }
                }
                Path spurPath = graph.dijkstra(spurNode, sink, bannedNodes, bannedEdges);
                if (spurPath == null) {
                    continue;
                }
                long rootWeight = 0;
                for (int i = 0; i < spurIndex; i++) {
                    rootWeight += graph.pairWeight(root[i], root[i + 1]);
                }
                int[] combinedNodes = new int[root.length + spurPath.nodes().length - 1];
                System.arraycopy(root, 0, combinedNodes, 0, root.length);
                int[] tail = spurPath.nodes();
                System.arraycopy(tail, 1, combinedNodes, root.length, tail.length - 1);
                Path candidate = new Path(rootWeight + spurPath.totalWeight(),
                        combinedNodes);
                if (!acceptedKeys.contains(pathKey(candidate))) {
                    candidates.add(candidate);
                }
            }
            Path next = candidates.poll();
            while (next != null && acceptedKeys.contains(pathKey(next))) {
                next = candidates.poll();
            }
            if (next == null) {
                break;
            }
            accepted.add(next);
            acceptedKeys.add(pathKey(next));
        }
        return List.copyOf(accepted);
    }

    private static void validate(int nodeCount, int[][] edges, int source, int sink, int k) {
        if (nodeCount < 2) {
            throw new IllegalArgumentException("节点数 ≥2（源汇异点语义，实际 " + nodeCount + "）");
        }
        if (k < 1) {
            throw new IllegalArgumentException("k 为正（实际 " + k + "）");
        }
        if (source < 0 || source >= nodeCount || sink < 0 || sink >= nodeCount) {
            throw new IllegalArgumentException("源/汇越域（n=" + nodeCount + "）");
        }
        if (source == sink) {
            throw new IllegalArgumentException("源汇同点（平凡路径面不涉）");
        }
        if (edges == null) {
            throw new IllegalArgumentException("边表非 null");
        }
        for (int i = 0; i < edges.length; i++) {
            int[] edge = edges[i];
            if (edge == null || edge.length != 3) {
                throw new IllegalArgumentException("边须为 int[3]（第 " + i + " 条）");
            }
            if (edge[0] < 0 || edge[0] >= nodeCount || edge[1] < 0 || edge[1] >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（第 " + i + " 条 "
                        + edge[0] + "→" + edge[1] + "）");
            }
            if (edge[2] < 0) {
                throw new IllegalArgumentException("权重非负（第 " + i + " 条实际 " + edge[2] + "）");
            }
        }
    }

    private static boolean sharesPrefix(int[] nodes, int[] root) {
        if (nodes.length < root.length) {
            return false;
        }
        for (int i = 0; i < root.length; i++) {
            if (nodes[i] != root[i]) {
                return false;
            }
        }
        return true;
    }

    private static String pathKey(Path path) {
        StringBuilder sb = new StringBuilder();
        for (int node : path.nodes()) {
            sb.append(node).append(',');
        }
        return sb.toString();
    }

    /** 去重平行边后的图：邻接（插入序）+ 逐 spur 禁面 + 确定性 Dijkstra。 */
    private static final class Graph {
        private final int nodeCount;
        private final List<List<Integer>> incident = new ArrayList<>();
        private int[] edgeFrom = new int[0];
        private int[] edgeTo = new int[0];
        private long[] edgeWeight = new long[0];
        private int edgeCount = 0;
        private final Map<Long, Long> pairWeights = new HashMap<>();

        private Graph(int nodeCount) {
            this.nodeCount = nodeCount;
            for (int i = 0; i < nodeCount; i++) {
                incident.add(new ArrayList<>());
            }
        }

        static Graph of(int nodeCount, int[][] edges) {
            Graph graph = new Graph(nodeCount);
            for (int[] edge : edges) {
                graph.addDeduplicated(edge[0], edge[1], edge[2]);
            }
            return graph;
        }

        /** 平行同向边取最小（后见更小覆盖权值，边槽位复用）。 */
        private void addDeduplicated(int from, int to, int weight) {
            int existingEdgeId = -1;
            for (int edgeId : incident.get(from)) {
                if (edgeTo[edgeId] == to) {
                    existingEdgeId = edgeId;
                    break;
                }
            }
            if (existingEdgeId != -1) {
                if (weight < edgeWeight[existingEdgeId]) {
                    edgeWeight[existingEdgeId] = weight;
                }
            } else {
                edgeFrom = Arrays.copyOf(edgeFrom, edgeCount + 1);
                edgeTo = Arrays.copyOf(edgeTo, edgeCount + 1);
                edgeWeight = Arrays.copyOf(edgeWeight, edgeCount + 1);
                edgeFrom[edgeCount] = from;
                edgeTo[edgeCount] = to;
                edgeWeight[edgeCount] = weight;
                incident.get(from).add(edgeCount);
                edgeCount++;
            }
            pairWeights.merge(pairKey(from, to), (long) weight, Math::min);
        }

        private long pairKey(int from, int to) {
            return (long) from * nodeCount + to;
        }

        private long pairWeight(int from, int to) {
            return pairWeights.get(pairKey(from, to));
        }

        /** Yen 禁边：已取路径在 spurIndex 处的出边（spur→next）禁用。 */
        private void banEdgeFrom(int spurIndex, Path taken, boolean[] bannedEdges) {
            int spurNode = taken.nodes()[spurIndex];
            int nextNode = taken.nodes()[spurIndex + 1];
            for (int edgeId : incident.get(spurNode)) {
                if (edgeTo[edgeId] == nextNode) {
                    bannedEdges[edgeId] = true;
                }
            }
        }

        /** 确定性 Dijkstra（(dist,node) 堆并列按点号；禁点/禁边掩码可空）。 */
        private Path dijkstra(int source, int sink, boolean[] bannedNodes,
                boolean[] bannedEdges) {
            long[] dist = new long[nodeCount];
            int[] prevEdge = new int[nodeCount];
            Arrays.fill(dist, Long.MAX_VALUE);
            Arrays.fill(prevEdge, -1);
            PriorityQueue<long[]> heap = new PriorityQueue<>((a, b) ->
                    a[0] != b[0] ? Long.compare(a[0], b[0])
                            : Long.compare(a[1], b[1]));
            dist[source] = 0;
            heap.add(new long[]{0L, source});
            while (!heap.isEmpty()) {
                long[] top = heap.poll();
                int node = (int) top[1];
                if (top[0] > dist[node]) {
                    continue;
                }
                if (node == sink) {
                    break;
                }
                for (int edgeId : incident.get(node)) {
                    if (bannedEdges != null && bannedEdges[edgeId]) {
                        continue;
                    }
                    int to = edgeTo[edgeId];
                    if (bannedNodes != null && bannedNodes[to]) {
                        continue;
                    }
                    long relaxed = dist[node] + edgeWeight[edgeId];
                    if (relaxed < dist[to]) {
                        dist[to] = relaxed;
                        prevEdge[to] = edgeId;
                        heap.add(new long[]{relaxed, to});
                    }
                }
            }
            if (dist[sink] == Long.MAX_VALUE) {
                return null;
            }
            int[] reversed = new int[nodeCount];
            int length = 0;
            for (int node = sink; node != -1; node = prevEdge[node] == -1 ? -1
                    : edgeFrom[prevEdge[node]]) {
                reversed[length++] = node;
                if (node == source) {
                    break;
                }
            }
            int[] nodes = new int[length];
            for (int i = 0; i < length; i++) {
                nodes[i] = reversed[length - 1 - i];
            }
            return new Path(dist[sink], nodes);
        }
    }
}
