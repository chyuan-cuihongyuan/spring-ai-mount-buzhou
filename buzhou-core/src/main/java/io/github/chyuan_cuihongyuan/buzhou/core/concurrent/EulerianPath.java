package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 欧拉路径/回路（spec 7031 / U7263 / impl 2283）——Hierholzer
 * 1873 思想：**度数判存在 + 后序栈拼路**——一笔画路径当且
 * 仅当连通且奇度点数为 0（回路）或 2（路径）——回溯式
 * 暴力找路（指数级）的病解。邻接按 id 升序（确定性同图
 * 同路）；路径边数=边总数（性质钉）；不满足度数条件
 * fail-fast（存在性前置诚实拒绝——跑一半失败的病根排除）。
 *
 * <p>与 TopologicalSorter（同包）同族不同面：DAG 依赖序 vs
 * 每边恰走一次的一笔画。
 */
public final class EulerianPath {

    private final int nodeCount;
    private final Map<Integer, TreeSet<Integer>> adjacency = new java.util.HashMap<>();
    private int edgeCount;

    /** nodeCount≥1（越域 fail-fast）。 */
    public EulerianPath(int nodeCount) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        for (int i = 0; i < nodeCount; i++) {
            adjacency.put(i, new TreeSet<>());
        }
        this.nodeCount = nodeCount;
    }

    /** 加有向边（越域 fail-fast；重边合法——重边各走一次）。 */
    public void addEdge(int from, int to) {
        checkNode(from);
        checkNode(to);
        adjacency.get(from).add(to);
        edgeCount++;
    }

    /** 欧拉路径/回路（节点序；无解 fail-fast——度数条件前置）。 */
    public List<Integer> eulerianPath() {
        int[] outDegree = new int[nodeCount];
        int[] inDegree = new int[nodeCount];
        for (Map.Entry<Integer, TreeSet<Integer>> entry : adjacency.entrySet()) {
            outDegree[entry.getKey()] += entry.getValue().size();
            for (int to : entry.getValue()) {
                inDegree[to]++;
            }
        }
        int start = -1;
        int oddOut = 0;
        for (int i = 0; i < nodeCount; i++) {
            if (outDegree[i] - inDegree[i] == 1) {
                oddOut++;
                start = i;
            } else if (inDegree[i] - outDegree[i] == 1) {
                oddOut++;
            } else if (inDegree[i] != outDegree[i]) {
                throw new IllegalArgumentException("度数不平衡——欧拉路径不存在");
            }
        }
        boolean hasEdges = edgeCount > 0;
        if (hasEdges && !(oddOut == 0 || oddOut == 2)) {
            throw new IllegalArgumentException("奇度点数 " + oddOut + "——欧拉路径不存在");
        }
        if (hasEdges && start == -1) {
            for (int i = 0; i < nodeCount; i++) {
                if (outDegree[i] > 0) {
                    start = i;
                    break;
                }
            }
        }
        if (start == -1) {
            return List.of(0);
        }
        Map<Integer, ArrayDeque<Integer>> pending = new java.util.HashMap<>();
        for (Map.Entry<Integer, TreeSet<Integer>> entry : adjacency.entrySet()) {
            pending.put(entry.getKey(), new ArrayDeque<>(entry.getValue()));
        }
        Deque<Integer> stack = new ArrayDeque<>();
        List<Integer> path = new ArrayList<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int node = stack.peek();
            ArrayDeque<Integer> next = pending.get(node);
            if (next != null && !next.isEmpty()) {
                stack.push(next.poll());
            } else {
                path.add(stack.pop());
            }
        }
        if (path.size() != edgeCount + 1) {
            throw new IllegalArgumentException("图不连通——欧拉路径不存在");
        }
        java.util.Collections.reverse(path);
        return path;
    }

    /** 边数读数。 */
    public int edgeCount() {
        return edgeCount;
    }

    private void checkNode(int node) {
        if (!adjacency.containsKey(node)) {
            throw new IllegalArgumentException("节点越域 [0," + nodeCount + "): " + node);
        }
    }
}
