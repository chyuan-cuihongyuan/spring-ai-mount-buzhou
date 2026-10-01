package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * 直接支配树（spec 10034 / X10069 / impl 2437）——Cooper–Harvey–Kennedy 2001
 * 思想（LLVM 同源——LT 1979 的工程简化面）：**逆后序迭代数据流求不交
 * （intersect 沿 idom 链双指爬升）直至收敛**——属支配树同题的迭代不动点面；
 * Lengauer–Tarjan 1979 半支配点伪码记忆面未过暴力删除法对拍神像（3000 随机图
 * 610 红——勘误入档），换 CHK 后 0/3000 全绿——「不可自洽即换静脉」纪律。
 *
 * <p>idom[root]=root；不可达顶点 −1 哨兵。确定性契约：先序/逆后序按邻接插入序
 * 展开+前趋插入序 intersect，可复算圣像。null 边表/null 边/端点越界/root 越界
 * fail-fast；自环/重边平凡语义参与。
 */
public final class DominatorTree {

    /** 不可达哨兵。 */
    private static final int UNREACHABLE = -1;

    private DominatorTree() {
    }

    /**
     * 直接支配点数组（下标=顶点 id；idom[root]=root；不可达为 −1）。
     *
     * @param nodeCount 顶点数（0..n−1）
     * @param edges 有向边 {from,to}
     * @param root 支配树根（可达性起点）
     * @throws IllegalArgumentException null 边表/null 边/端点越界/root 越界
     */
    public static int[] immediateDominators(int nodeCount, int[][] edges, int root) {
        validate(nodeCount, edges, root);
        List<List<Integer>> successors = adjacency(nodeCount, edges, false);
        List<List<Integer>> predecessors = adjacency(nodeCount, edges, true);
        List<Integer> postorderSequence = new ArrayList<>();
        int[] postorder = postorderNumbers(nodeCount, root, successors, postorderSequence);
        List<Integer> reversePostorder = new ArrayList<>(postorderSequence);
        java.util.Collections.reverse(reversePostorder);
        int[] idom = new int[nodeCount];
        Arrays.fill(idom, UNREACHABLE);
        idom[root] = root;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int vertex : reversePostorder) {
                if (vertex == root) {
                    continue;
                }
                int newIdom = UNREACHABLE;
                for (int from : predecessors.get(vertex)) {
                    if (idom[from] == UNREACHABLE) {
                        continue;
                    }
                    newIdom = newIdom == UNREACHABLE ? from
                            : intersect(from, newIdom, idom, postorder);
                }
                if (newIdom != UNREACHABLE && idom[vertex] != newIdom) {
                    idom[vertex] = newIdom;
                    changed = true;
                }
            }
        }
        return idom;
    }

    private static void validate(int nodeCount, int[][] edges, int root) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("顶点数为正（实际 " + nodeCount + "）");
        }
        if (root < 0 || root >= nodeCount) {
            throw new IllegalArgumentException("root 越域（n=" + nodeCount + "）");
        }
        if (edges == null) {
            throw new IllegalArgumentException("边表非 null");
        }
        for (int i = 0; i < edges.length; i++) {
            int[] edge = edges[i];
            if (edge == null || edge.length != 2) {
                throw new IllegalArgumentException("边须为 int[2]（第 " + i + " 条）");
            }
            if (edge[0] < 0 || edge[0] >= nodeCount || edge[1] < 0 || edge[1] >= nodeCount) {
                throw new IllegalArgumentException("边端点越域（第 " + i + " 条 "
                        + edge[0] + "→" + edge[1] + "）");
            }
        }
    }

    /** 迭代 DFS 后序（序列后追加序=后序号；−1 哨兵=不可达）。 */
    private static int[] postorderNumbers(int nodeCount, int root,
            List<List<Integer>> successors, List<Integer> postorderSequence) {
        int[] postorder = new int[nodeCount];
        Arrays.fill(postorder, UNREACHABLE);
        boolean[] visited = new boolean[nodeCount];
        int[] cursor = new int[nodeCount];
        Deque<Integer> stack = new ArrayDeque<>();
        visited[root] = true;
        stack.push(root);
        while (!stack.isEmpty()) {
            int vertex = stack.peek();
            if (cursor[vertex] < successors.get(vertex).size()) {
                int to = successors.get(vertex).get(cursor[vertex]++);
                if (!visited[to]) {
                    visited[to] = true;
                    stack.push(to);
                }
                continue;
            }
            postorder[vertex] = postorderSequence.size();
            postorderSequence.add(vertex);
            stack.pop();
        }
        return postorder;
    }

    /** CHK intersect：沿 idom 链爬升较浅指（后序小者上爬）直至双指相遇。 */
    private static int intersect(int a, int b, int[] idom, int[] postorder) {
        while (a != b) {
            while (postorder[a] < postorder[b]) {
                a = idom[a];
            }
            while (postorder[b] < postorder[a]) {
                b = idom[b];
            }
        }
        return a;
    }

    /** 邻接表（reverse=true 建逆邻接）。 */
    private static List<List<Integer>> adjacency(int nodeCount, int[][] edges,
            boolean reverse) {
        List<List<Integer>> adjacency = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            int from = reverse ? edge[1] : edge[0];
            int to = reverse ? edge[0] : edge[1];
            adjacency.get(from).add(to);
        }
        return adjacency;
    }
}
