package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * Hopcroft–Karp 二分图最大匹配（spec 9001 / W9003 / impl 2354）——
 * Hopcroft–Karp 1973 思想：**分层 BFS 定最短增广路长度 + 当前弧
 * DFS 一轮增广出该长度的全部不相交增广路（阶段制）**——阶段数
 * O(√V)、总 O(E√V)——单条增广路逐条 BFS/DFS（V 轮×每轮 E 的
 * O(VE) 放大）的病解。左集/右集 0..n-1 编号；边行 {left,right}；
 * 返回左点匹配右点的数组（未匹配 -1）；边按插入序处理（同图同
 * 匹配大小完全确定——匹配分布不唯一，取序固定）；端点越域
 * fail-fast。
 *
 * <p>与 HungarianMatcher（spec 8013）同族不同面：最大匹配数 vs
 * 最小代价完美指派；与 DinicMaxFlow（spec 8006）同根：HK 即二分
 * 单位容量网的阶段化增广特例。
 */
public final class HopcroftKarpMatcher {

    private HopcroftKarpMatcher() {
    }

    /**
     * 最大匹配（matchLeft[i] = 匹配的右点或 -1）。
     *
     * @throws IllegalArgumentException 节点数/端点越域
     */
    public static int[] matching(int leftCount, int rightCount, int[][] edges) {
        if (leftCount < 0 || rightCount < 0) {
            throw new IllegalArgumentException("节点数非负（实际 " + leftCount + "×" + rightCount + "）");
        }
        int m = edges.length;
        int[] edgeTo = new int[m];
        int[] next = new int[m];
        int[] head = new int[leftCount];
        Arrays.fill(head, -1);
        for (int i = 0; i < m; i++) {
            int left = edges[i][0];
            int right = edges[i][1];
            if (left < 0 || left >= leftCount || right < 0 || right >= rightCount) {
                throw new IllegalArgumentException("边端点越域（" + left + "→" + right + "）");
            }
            edgeTo[i] = right;
            next[i] = head[left];
            head[left] = i;
        }
        int[] matchLeft = new int[leftCount];
        int[] matchRight = new int[rightCount];
        Arrays.fill(matchLeft, -1);
        Arrays.fill(matchRight, -1);
        int[] dist = new int[leftCount + 1];
        int[] queue = new int[leftCount + 1];
        int[] iter = new int[leftCount];
        int matched = 0;
        while (bfs(leftCount, head, edgeTo, next, matchLeft, matchRight, dist, queue)) {
            System.arraycopy(head, 0, iter, 0, leftCount);
            for (int u = 0; u < leftCount; u++) {
                if (matchLeft[u] == -1 && dfs(u, head, edgeTo, next, matchLeft, matchRight, dist, iter)) {
                    matched++;
                }
            }
        }
        return matchLeft;
    }

    /** 匹配大小（便利面）。 */
    public static int maxMatchingSize(int leftCount, int rightCount, int[][] edges) {
        int[] matchLeft = matching(leftCount, rightCount, edges);
        int size = 0;
        for (int v : matchLeft) {
            if (v != -1) {
                size++;
            }
        }
        return size;
    }

    private static boolean bfs(int leftCount, int[] head, int[] edgeTo, int[] next,
                               int[] matchLeft, int[] matchRight, int[] dist, int[] queue) {
        int qt = 0;
        for (int u = 0; u < leftCount; u++) {
            if (matchLeft[u] == -1) {
                dist[u] = 0;
                queue[qt++] = u;
            } else {
                dist[u] = -1;
            }
        }
        dist[leftCount] = -1;
        boolean found = false;
        for (int qh = 0; qh < qt; qh++) {
            int u = queue[qh];
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = edgeTo[e];
                int paired = matchRight[v] == -1 ? leftCount : matchRight[v];
                if (dist[paired] == -1) {
                    dist[paired] = dist[u] + 1;
                    if (paired == leftCount) {
                        found = true;
                    } else {
                        queue[qt++] = paired;
                    }
                }
            }
        }
        return found;
    }

    private static boolean dfs(int u, int[] head, int[] edgeTo, int[] next,
                               int[] matchLeft, int[] matchRight, int[] dist, int[] iter) {
        if (u == -1) {
            return true;
        }
        for (int e = iter[u]; e != -1; e = iter[u]) {
            int v = edgeTo[e];
            int paired = matchRight[v] == -1 ? head.length : matchRight[v];
            if (paired == head.length
                    || (dist[paired] == dist[u] + 1 && dfs(paired, head, edgeTo, next, matchLeft, matchRight, dist, iter))) {
                matchLeft[u] = v;
                matchRight[v] = u;
                return true;
            }
            iter[u] = next[e];
        }
        dist[u] = -1;
        return false;
    }
}
