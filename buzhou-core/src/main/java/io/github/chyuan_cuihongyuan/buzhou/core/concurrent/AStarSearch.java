package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.ToIntFunction;

/**
 * A\* 启发式最短路（spec 9048 / W9097 / impl 2400）——Hart–Nilsson–
 * Raphael 1968 思想（「启发式决定论证明」——游戏寻路/导航/
 * 求解器同源）：**f(n)=g(n)+h(n) 优先队列扩展——g 已走实距、h
 * 目标估计（可采纳 h≤真值时最优性保持），f 单调队列聚焦朝目标
 * 的前沿**——Dijkstra 无向四撒（均匀探索全圆）与贪心最佳优先
 * （h 不可采纳可绕远）的中间形态。可采纳性契约明示（过估 h
 * 结果可能次优——诚实边界入档）；同 f 平局按节点号（确定性
 * 取序）；负权拒绝（契约同 Dijkstra）；null/端点越域/
 * 启发函数 null fail-fast。
 *
 * <p>与 DijkstraShortestPath（同包）同根不同面：h≡0 时恰退化为
 * Dijkstra（互证圣像）；与 FloydWarshall（同包）不同面：单源
 * 单汇 vs 全对。
 */
public final class AStarSearch {

    private AStarSearch() {
    }

    /**
     * 最短距离（edges 行 = {from,to,weight}；heuristic = 到目标估计，须可采纳）。
     *
     * @throws IllegalArgumentException null/负权/端点越域/不可达目标
     */
    public static long shortestPath(int nodeCount, int[][] edges, int source, int target,
                                    ToIntFunction<Integer> heuristic) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数为正（实际 " + nodeCount + "）");
        }
        if (source < 0 || source >= nodeCount || target < 0 || target >= nodeCount) {
            throw new IllegalArgumentException("端点越域（" + source + "→" + target + "）");
        }
        if (heuristic == null) {
            throw new IllegalArgumentException("启发函数非空引用");
        }
        List<int[]>[] adjacency = new List[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            adjacency[i] = new ArrayList<>();
        }
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            long weight = edge[2];
            if (from < 0 || from >= nodeCount || to < 0 || to >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + from + "→" + to + "）");
            }
            if (weight < 0) {
                throw new IllegalArgumentException("权非负（实际 " + weight + "）——h 契约同");
            }
            adjacency[from].add(new int[]{to, (int) weight});
            adjacency[to].add(new int[]{from, (int) weight});
        }
        long[] distance = new long[nodeCount];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0;
        // 队列元素 {f, g, node}——同 f 平局按节点号（数组自然序，确定取序）
        PriorityQueue<long[]> frontier = new PriorityQueue<>((a, b) -> {
            if (a[0] != b[0]) {
                return Long.compare(a[0], b[0]);
            }
            if (a[1] != b[1]) {
                return Long.compare(a[1], b[1]);
            }
            return Long.compare(a[2], b[2]);
        });
        frontier.add(new long[]{heuristic.applyAsInt(source), 0, source});
        while (!frontier.isEmpty()) {
            long[] top = frontier.poll();
            int node = (int) top[2];
            if (top[1] > distance[node]) {
                continue; // 陈旧条目（g 已被更短路改写）
            }
            if (node == target) {
                return top[1];
            }
            for (int[] edge : adjacency[node]) {
                int next = edge[0];
                long candidate = top[1] + edge[1];
                if (candidate < distance[next]) {
                    distance[next] = candidate;
                    frontier.add(new long[]{candidate + heuristic.applyAsInt(next), candidate, next});
                }
            }
        }
        throw new IllegalArgumentException("目标不可达（" + source + "→" + target + "）");
    }
}
