package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * Stoer–Wagner 无向全局最小割（spec 9003 / W9007 / impl 2356）——
 * Stoer–Wagner 1997 思想：**最大邻接序（MAO）阶段制——每阶段
 * 「最紧连接点依次入序」，末位 t 与前位 s 的割 = t 到已入序集的
 * 边权和为候选割；s 并入 t 收缩两」n−1 阶段取最小** O(V³
 * 邻接矩阵）——逐对枚举点对割（2^V 子集枚举指数爆炸）的病解。
 * 无向边 {u,v,w}（u≠v、w>0、平行边合并）；割值唯一承诺
 * （割侧分布可能不唯一——明示）；同权取最低编号（完全确定）；
 * 节点数≥2/自环拒绝/端点越域/非正权 fail-fast。
 *
 * <p>与 EdmondsKarpMaxFlow/DinicMaxFlow（spec 9002/8006）同根
 * 不同面：无向无源汇全局割 vs 有向有源汇 s-t 最大流（max-flow
 * min-cut 定理的另一半）。
 */
public final class StoerWagnerMinCut {

    private StoerWagnerMinCut() {
    }

    /**
     * 无向全局最小割值（0..nodeCount-1 编号；edges 行 = {u,v,weight}）。
     *
     * @throws IllegalArgumentException 节点数<2、自环、端点越域、非正权
     */
    public static long minCut(int nodeCount, int[][] edges) {
        if (nodeCount < 2) {
            throw new IllegalArgumentException("节点数≥2（实际 " + nodeCount + "）");
        }
        long[][] weight = new long[nodeCount][nodeCount];
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            long w = edge[2];
            if (u < 0 || u >= nodeCount || v < 0 || v >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + u + "—" + v + "）");
            }
            if (u == v) {
                throw new IllegalArgumentException("自环不入割（" + u + "）");
            }
            if (w <= 0) {
                throw new IllegalArgumentException("权为正（实际 " + w + "）");
            }
            weight[u][v] += w;
            weight[v][u] += w;
        }
        boolean[] active = new boolean[nodeCount];
        Arrays.fill(active, true);
        long[] cohesion = new long[nodeCount];
        boolean[] added = new boolean[nodeCount];
        int[] order = new int[nodeCount];
        long best = Long.MAX_VALUE;
        for (int phase = 0; phase < nodeCount - 1; phase++) {
            Arrays.fill(added, false);
            Arrays.fill(cohesion, 0);
            int activeCount = nodeCount - phase;
            int prev = -1;
            int last = -1;
            for (int step = 0; step < activeCount; step++) {
                int chosen = -1;
                for (int v = 0; v < nodeCount; v++) {
                    if (active[v] && !added[v]
                            && (chosen == -1 || cohesion[v] > cohesion[chosen])) {
                        chosen = v;
                    }
                }
                added[chosen] = true;
                prev = last;
                last = chosen;
                for (int v = 0; v < nodeCount; v++) {
                    if (active[v] && !added[v]) {
                        cohesion[v] += weight[chosen][v];
                    }
                }
            }
            long cutOfPhase = cohesion[last];
            best = Math.min(best, cutOfPhase);
            // 收缩：prev 并入 last（同权取最低编号已保确定性）
            active[prev] = false;
            for (int v = 0; v < nodeCount; v++) {
                if (active[v] && v != last) {
                    weight[last][v] += weight[prev][v];
                    weight[v][last] = weight[last][v];
                }
            }
        }
        return best;
    }
}
