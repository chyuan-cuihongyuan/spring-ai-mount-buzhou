package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * 拓扑排序器（spec 3012 / T5025 / impl 2013）——Kahn 算法思想
 * （入度归零入队）：DAG 依赖序的确定性原语——**字典序最小**贪心
 * （同批可选取最小下标——确定性可复算，非入队序依赖）；**环诚实
 * 报告**：有环时 acyclic=false 且 order 给出环外前缀（已排定的
 * 传递闭包子集），不臆造半吊子全序。「工具/hook/模型回退链依赖
 * 解析、初始化次序」的地基件。
 *
 * <p>定容 int 宇宙；只排序不改图（sort 幂等可重放）。
 */
public final class TopologicalSorter {

    /** 排序结果：order（无环=全集；有环=环外已排前缀）+ acyclic 判定。 */
    public record SortResult(List<Integer> order, boolean acyclic) {
    }

    private final int vertexCount;
    private final List<List<Integer>> adjacency;
    private int edgeCount;

    /** 定容构造：顶点 0..vertexCount−1。 */
    public TopologicalSorter(int vertexCount) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("vertexCount 非负：" + vertexCount);
        }
        this.vertexCount = vertexCount;
        this.adjacency = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    /** 有向边 from → to（to 依赖 from——from 先行）。 */
    public void addEdge(int from, int to) {
        requireInUniverse(from);
        requireInUniverse(to);
        adjacency.get(from).add(to);
        edgeCount++;
    }

    /** 顶点数。 */
    public int vertexCount() {
        return vertexCount;
    }

    /** 边数。 */
    public int edgeCount() {
        return edgeCount;
    }

    /** Kahn 排序（最小下标优先队列——字典序最小拓扑序，确定性）。 */
    public SortResult sort() {
        int[] indegree = new int[vertexCount];
        for (int from = 0; from < vertexCount; from++) {
            for (int to : adjacency.get(from)) {
                indegree[to]++;
            }
        }
        Queue<Integer> ready = new PriorityQueue<>();
        for (int v = 0; v < vertexCount; v++) {
            if (indegree[v] == 0) {
                ready.add(v);
            }
        }
        List<Integer> order = new ArrayList<>(vertexCount);
        while (!ready.isEmpty()) {
            int v = ready.poll();
            order.add(v);
            for (int to : adjacency.get(v)) {
                if (--indegree[to] == 0) {
                    ready.add(to);
                }
            }
        }
        boolean acyclic = order.size() == vertexCount;
        return new SortResult(List.copyOf(order), acyclic);
    }

    private void requireInUniverse(int v) {
        if (v < 0 || v >= vertexCount) {
            throw new IndexOutOfBoundsException("顶点越界：" + v + "（宇宙 0.." + (vertexCount - 1) + "）");
        }
    }
}
