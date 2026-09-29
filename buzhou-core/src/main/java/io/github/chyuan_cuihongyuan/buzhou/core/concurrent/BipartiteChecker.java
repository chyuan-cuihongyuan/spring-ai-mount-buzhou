package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 二分图判定（spec 8008 / V8017 / impl 2310）——
 * Kőnig 1931 二分图定理思想（任务分组/冲突排除同源）：
 * **交替 0/1 染色、撞色即非二分**一次线性判定——暴力枚举
 * 双集合划分 O(2^V)（奇圈存在必撞色——病根）的病解。
 * isBipartite/sides 双面（非二分 sides 返回空集——诚实缺省）；
 * 孤立节点归 0 侧 canonical（同图同划分完全确定）；自环
 * fail-fast（自指节点自相矛盾）；端点越域 fail-fast。
 *
 * <p>与 GraphColoring（spec 8010）同族不同面：二染色判定面
 * vs 一般 k 着色构造面。
 */
public final class BipartiteChecker {

    private BipartiteChecker() {
    }

    /** 双集合划分（sides[0]/sides[1] 升序；非二分返回空 list——诚实缺省）。 */
    public static List<List<Integer>> sides(int nodeCount, int[][] edges) {
        int[] color = colorBy(nodeCount, edges);
        if (color == null) {
            return List.of();
        }
        List<Integer> zero = new ArrayList<>();
        List<Integer> one = new ArrayList<>();
        for (int node = 0; node < nodeCount; node++) {
            (color[node] == 0 ? zero : one).add(node);
        }
        return List.of(List.copyOf(zero), List.copyOf(one));
    }

    /** 是否二分（nodeCount≥1；自环/端点越域 fail-fast）。 */
    public static boolean isBipartite(int nodeCount, int[][] edges) {
        return colorBy(nodeCount, edges) != null;
    }

    /** 交替染色；非二分返回 null。 */
    private static int[] colorBy(int nodeCount, int[][] edges) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数非负（实际 " + nodeCount + "）");
        }
        List<List<Integer>> adjacency = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            if (from < 0 || from >= nodeCount || to < 0 || to >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + from + "—" + to + "）");
            }
            if (from == to) {
                throw new IllegalArgumentException("自环 fail-fast（节点 " + from + " 自指自相矛盾）");
            }
            adjacency.get(from).add(to);
            adjacency.get(to).add(from);
        }
        int[] color = new int[nodeCount];
        java.util.Arrays.fill(color, -1);
        Deque<Integer> queue = new ArrayDeque<>();
        for (int start = 0; start < nodeCount; start++) {
            if (color[start] >= 0) {
                continue;
            }
            color[start] = 0;
            queue.add(start);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v : adjacency.get(u)) {
                    if (color[v] < 0) {
                        color[v] = color[u] ^ 1;
                        queue.add(v);
                    } else if (color[v] == color[u]) {
                        return null;
                    }
                }
            }
        }
        return color;
    }
}
