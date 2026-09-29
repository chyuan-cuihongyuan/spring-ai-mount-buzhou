package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 图着色（spec 8010 / V8021 / impl 2312）——
 * Welsh & Powell 1967 思想（寄存器分配/排课表同源）：
 * **度降序贪心给 (Δ+1) 上限内的可行着色**——最优色数是
 * NP-hard，工程面要的是有界可行而非理论最优（诚实边界
 * 明示）：节点按度降序（并列按编号升序 canonical）逐个
 * 分配「邻居未占的最小色」。colors/colorCount 双面；
 * 自环/端点越域 fail-fast；确定性纯函数（同图同分配）。
 *
 * <p>与 BipartiteChecker（spec 8008）同族不同面：二染色
 * 判定面 vs 一般 k 着色构造面。
 */
public final class GraphColoring {

    private GraphColoring() {
    }

    /** 每节点色号（从 0 起；并列度按编号序——同图同结果）。 */
    public static int[] colors(int nodeCount, int[][] edges) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数非负（实际 " + nodeCount + "）");
        }
        List<java.util.Set<Integer>> neighbors = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            neighbors.add(new java.util.HashSet<>());
        }
        for (int[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            if (from < 0 || from >= nodeCount || to < 0 || to >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（" + from + "—" + to + "）");
            }
            if (from == to) {
                throw new IllegalArgumentException("自环 fail-fast（节点 " + from + "）");
            }
            neighbors.get(from).add(to);
            neighbors.get(to).add(from);
        }
        Integer[] order = new Integer[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> {
            int byDegree = Integer.compare(neighbors.get(b).size(), neighbors.get(a).size());
            return byDegree != 0 ? byDegree : Integer.compare(a, b);
        });
        int[] colors = new int[nodeCount];
        Arrays.fill(colors, -1);
        for (int node : order) {
            boolean[] used = new boolean[nodeCount];
            for (int neighbor : neighbors.get(node)) {
                if (colors[neighbor] >= 0) {
                    used[colors[neighbor]] = true;
                }
            }
            int color = 0;
            while (used[color]) {
                color++;
            }
            colors[node] = color;
        }
        return colors;
    }

    /** 色数（colors 的不同值个数——零基连续）。 */
    public static int colorCount(int[] colors) {
        if (colors == null || colors.length == 0) {
            throw new IllegalArgumentException("色表非空");
        }
        int max = 0;
        for (int color : colors) {
            if (color < 0) {
                throw new IllegalArgumentException("色号非负（实际 " + color + "）");
            }
            max = Math.max(max, color);
        }
        return max + 1;
    }
}
