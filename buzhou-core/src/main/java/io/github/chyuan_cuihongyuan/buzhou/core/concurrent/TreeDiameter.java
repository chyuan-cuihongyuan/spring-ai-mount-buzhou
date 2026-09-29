package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * 树的直径（spec 8009 / V8019 / impl 2311）——后序子树 DP
 * 经典思想（网络拓扑最长链规划同源）：**每节点保子树内
 * 最深与次深（各带端点），跨节点拼链取 max**——一次遍历
 * O(V²)（邻接扫描实现；比较面 O(V)）得直径——全点对最短路
 * 取 max O(V²) 每对（拓扑审计放大）的病解。无权边语义
 * （边长 1）；diameter/path 双面（端点并列取小编号
 * canonical——同树同结果）；单节点 0 合法；null/空树/
 * 多点父（非树形）fail-fast。
 *
 * <p>与 LcaLifting（spec 7010）同族不同面：祖先查询面 vs
 * 全局最长链面。
 */
public final class TreeDiameter {

    private TreeDiameter() {
    }

    /**
     * 直径路径（含两端点；父数组语义 parent[root]=−1；单节点返回单点）。
     *
     * @throws IllegalArgumentException 空树/自父环/多根/越域父/多点父（非树形）
     */
    public static List<Integer> path(int[] parent) {
        if (parent == null || parent.length == 0) {
            throw new IllegalArgumentException("树非空（父数组至少一节点）");
        }
        int n = parent.length;
        if (n == 1) {
            if (parent[0] != -1) {
                throw new IllegalArgumentException("单节点父应为 −1（实际 " + parent[0] + "）");
            }
            return List.of(0);
        }
        validate(parent);
        int[][] top = topTwoWithEndpoints(parent);
        int peak = 0;
        int best = top[0][0] + Math.max(top[0][2], 0);
        for (int node = 1; node < n; node++) {
            int through = top[node][0] + Math.max(top[node][2], 0);
            if (through > best) {
                best = through;
                peak = node;
            }
        }
        if (best == 0) {
            return List.of(0);
        }
        int endDeep = top[peak][1];
        List<Integer> upChain = new ArrayList<>();
        int cursor = endDeep;
        while (cursor != peak) {
            upChain.add(cursor);
            cursor = parent[cursor];
        }
        upChain.add(peak);
        List<Integer> downChain = new ArrayList<>();
        if (top[peak][2] > 0) {
            cursor = top[peak][3];
            while (cursor != peak) {
                downChain.add(cursor);
                cursor = parent[cursor];
            }
        }
        java.util.Collections.reverse(downChain);
        List<Integer> full = new ArrayList<>(upChain);
        full.addAll(downChain);
        return List.copyOf(full);
    }

    /** 直径边数（单节点 0）。 */
    public static int diameter(int[] parent) {
        return path(parent).size() - 1;
    }

    /** 单亲树形校验（自父环/多根/越域父/不可达多点父 fail-fast）。 */
    private static void validate(int[] parent) {
        int n = parent.length;
        int root = -1;
        boolean[] hasParent = new boolean[n];
        for (int node = 0; node < n; node++) {
            int p = parent[node];
            if (p == node) {
                throw new IllegalArgumentException("自父环（节点 " + node + "）");
            }
            if (p == -1) {
                if (root >= 0) {
                    throw new IllegalArgumentException("多根非树（" + root + " 与 " + node + "）");
                }
                root = node;
            } else if (p < 0 || p >= n) {
                throw new IllegalArgumentException("父指针越域（节点 " + node + " 父 " + p + "）");
            } else {
                hasParent[p] = true;
            }
        }
        if (root < 0) {
            throw new IllegalArgumentException("无根非树（父数组无 −1 项）");
        }
        Deque<Integer> queue = new ArrayDeque<>();
        boolean[] seen = new boolean[n];
        seen[root] = true;
        queue.add(root);
        int reached = 1;
        List<List<Integer>> children = childrenOf(parent, n);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int child : children.get(u)) {
                if (!seen[child]) {
                    seen[child] = true;
                    reached++;
                    queue.add(child);
                }
            }
        }
        if (reached != n) {
            throw new IllegalArgumentException("多点父非树（仅可达 " + reached + "/" + n + " 节点）");
        }
    }

    /** 后序 DP：每节点 {top1 深, top1 端点, top2 深, top2 端点}（叶 = {0,自,−1,−1}）。 */
    private static int[][] topTwoWithEndpoints(int[] parent) {
        int n = parent.length;
        List<List<Integer>> children = childrenOf(parent, n);
        int[] depth = new int[n];
        int[] order = new int[n];
        int filled = 0;
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(rootOf(parent));
        while (!stack.isEmpty()) {
            int node = stack.pop();
            order[filled++] = node;
            for (int child : children.get(node)) {
                depth[child] = depth[node] + 1;
                stack.push(child);
            }
        }
        int[][] top = new int[n][4];
        for (int i = n - 1; i >= 0; i--) {
            int node = order[i];
            top[node][0] = 0;
            top[node][1] = node;
            top[node][2] = -1;
            top[node][3] = -1;
            for (int child : children.get(node)) {
                int candidateDepth = top[child][0] + 1;
                int candidateEnd = top[child][1];
                if (candidateDepth > top[node][0]) {
                    top[node][2] = top[node][0];
                    top[node][3] = top[node][1];
                    top[node][0] = candidateDepth;
                    top[node][1] = candidateEnd;
                } else if (candidateDepth > top[node][2]) {
                    top[node][2] = candidateDepth;
                    top[node][3] = candidateEnd;
                }
            }
        }
        return top;
    }

    private static int rootOf(int[] parent) {
        for (int node = 0; node < parent.length; node++) {
            if (parent[node] == -1) {
                return node;
            }
        }
        throw new IllegalArgumentException("无根非树（父数组无 −1 项）");
    }

    private static List<List<Integer>> childrenOf(int[] parent, int n) {
        List<List<Integer>> children = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            children.add(new ArrayList<>());
        }
        for (int node = 0; node < n; node++) {
            int p = parent[node];
            if (p >= 0 && p < n) {
                children.get(p).add(node);
            }
        }
        return children;
    }
}
