package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * 推重标最大流（spec 10032 / X10065 / impl 2435）——Goldberg–Tarjan 1988 思想
 * （LEMON/Boost Graph 同源）：**源点 preflow 满推（height[s]=n）+FIFO 活跃队列
 * 逐点 discharge——残量正且 h[u]=h[v]+1 的 admissible 边推进、否则重标为最小
 * 可推邻高度 +1**——超额/高度两个局部量驱动，无增广路搜索。与
 * EdmondsKarpMaxFlow/DinicMaxFlow（已占）同域不同面：预流推进局部高度 vs
 * 分层增广路。
 *
 * <p>流值唯一承诺（流量分布不唯一——沿 Dinic 口径明示）；边按插入序处理
 * （同图同流值完全确定）；重标总数护栏防病态循环（超出即 IllegalStateException
 * ——算法层 bug 哨兵）。long 容量域；负容量/源汇同点/端点越界 fail-fast。
 */
public final class PushRelabelMaxFlow {

    /** 重标总数护栏（FIFO 推重标理论上界 O(V²E) 内的宽松病态哨兵）。 */
    private static final int MAX_TOTAL_RELABELS = 1 << 20;

    private PushRelabelMaxFlow() {
    }

    /**
     * 最大流值（0..nodeCount−1 编号；edges 行 = {from,to,capacity}）。
     *
     * @throws IllegalArgumentException 节点数非正/端点越界/源汇同点/负容量
     * @throws IllegalStateException 重标护栏击穿（算法层病态哨兵）
     */
    public static long maxFlow(int nodeCount, int[][] edges, int source, int sink) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数为正（实际 " + nodeCount + "）");
        }
        if (source < 0 || source >= nodeCount || sink < 0 || sink >= nodeCount) {
            throw new IllegalArgumentException("源/汇越域（n=" + nodeCount + "）");
        }
        if (source == sink) {
            throw new IllegalArgumentException("源汇同点（流语义无定义）");
        }
        int[] edgeTo = new int[2 * edges.length];
        long[] cap = new long[2 * edges.length];
        int[] next = new int[2 * edges.length];
        int[] head = new int[nodeCount];
        Arrays.fill(head, -1);
        int edgeCount = 0;
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
            edgeTo[edgeCount] = to;
            cap[edgeCount] = capacity;
            next[edgeCount] = head[from];
            head[from] = edgeCount++;
            edgeTo[edgeCount] = from;
            cap[edgeCount] = 0;
            next[edgeCount] = head[to];
            head[to] = edgeCount++;
        }
        return preflowPush(nodeCount, head, next, edgeTo, cap, source, sink);
    }

    /** FIFO 推重标主循环（preflow 初始化内联）。 */
    private static long preflowPush(int nodeCount, int[] head, int[] next, int[] edgeTo,
            long[] cap, int source, int sink) {
        int[] height = new int[nodeCount];
        long[] excess = new long[nodeCount];
        boolean[] inQueue = new boolean[nodeCount];
        height[source] = nodeCount;
        for (int edge = head[source]; edge != -1; edge = next[edge]) {
            if (cap[edge] > 0) {
                excess[edgeTo[edge]] += cap[edge];
                cap[edge ^ 1] += cap[edge];
                cap[edge] = 0;
            }
        }
        excess[source] = 0;
        Deque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < nodeCount; vertex++) {
            if (vertex != source && vertex != sink && excess[vertex] > 0) {
                queue.addLast(vertex);
                inQueue[vertex] = true;
            }
        }
        long totalDischarges = 0;
        while (!queue.isEmpty()) {
            int vertex = queue.pollFirst();
            inQueue[vertex] = false;
            if (excess[vertex] <= 0) {
                continue;
            }
            discharge(vertex, height, excess, head, next, edgeTo, cap,
                    queue, inQueue, source, sink);
            totalDischarges++;
            if (totalDischarges > MAX_TOTAL_RELABELS) {
                throw new IllegalStateException("discharge 护栏击穿（" + totalDischarges
                        + "）——算法层病态哨兵");
            }
        }
        return excess[sink];
    }

    /** 单点 discharge：admissible 推进耗尽后重标回队。 */
    private static void discharge(int vertex, int[] height, long[] excess, int[] head,
            int[] next, int[] edgeTo, long[] cap, Deque<Integer> queue,
            boolean[] inQueue, int source, int sink) {
        int cursor = head[vertex];
        while (excess[vertex] > 0) {
            if (cursor == -1) {
                relabel(vertex, height, head, next, edgeTo, cap);
                cursor = head[vertex];
                continue;
            }
            int to = edgeTo[cursor];
            if (cap[cursor] > 0 && height[vertex] == height[to] + 1) {
                long moved = Math.min(excess[vertex], cap[cursor]);
                cap[cursor] -= moved;
                cap[cursor ^ 1] += moved;
                excess[vertex] -= moved;
                excess[to] += moved;
                if (to != source && to != sink && !inQueue[to]) {
                    queue.addLast(to);
                    inQueue[to] = true;
                }
                if (excess[vertex] == 0) {
                    return;
                }
            }
            cursor = next[cursor];
        }
    }

    /** 重标：抬到最小可推邻高度 +1（有超额即必有残量出边——回推边保底）。 */
    private static void relabel(int vertex, int[] height, int[] head, int[] next,
            int[] edgeTo, long[] cap) {
        int minHeight = Integer.MAX_VALUE;
        for (int edge = head[vertex]; edge != -1; edge = next[edge]) {
            if (cap[edge] > 0) {
                minHeight = Math.min(minHeight, height[edgeTo[edge]]);
            }
        }
        height[vertex] = minHeight + 1;
    }
}
