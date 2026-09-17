package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Tarjan 强连通分量（spec 3018 / T5037 / impl 2019）——Tarjan 1982
 * 思想（index/lowlink + 显式栈单遍）：SCC 集合 + **环成员定位**
 * （TopologicalSorter 只报「有环」不指认谁的留白补位）——组件按
 * **凝聚图反拓扑序**输出（sink 侧先出——依赖图的被依赖方先列）；
 * 自环单点亦算环成员。**迭代实现**（显式帧栈——万级链深不爆
 * 调用栈）。依赖环排查（工具/hook/初始化循环依赖）的显形件。
 *
 * <p>确定性可复算（顶点升序起扫+邻接插入序）；不改图（components
 * 幂等可重放）。
 */
public final class TarjanSccFinder {

    private final int vertexCount;
    private final List<List<Integer>> adjacency;
    private int edgeCount;

    /** 定容构造：顶点 0..vertexCount−1。 */
    public TarjanSccFinder(int vertexCount) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("vertexCount 非负：" + vertexCount);
        }
        this.vertexCount = vertexCount;
        this.adjacency = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    /** 有向边 from → to。 */
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

    /**
     * SCC 列表（凝聚图反拓扑序——sink 侧组件先出；组件内成员为
     * 显式栈弹出序，确定性）。
     */
    public List<List<Integer>> components() {
        int[] index = new int[vertexCount];
        int[] lowlink = new int[vertexCount];
        boolean[] onStack = new boolean[vertexCount];
        java.util.Arrays.fill(index, -1);
        Deque<Integer> stack = new ArrayDeque<>();
        List<List<Integer>> components = new ArrayList<>();
        int counter = 0;
        for (int root = 0; root < vertexCount; root++) {
            if (index[root] != -1) {
                continue;
            }
            counter = strongConnectIterative(root, index, lowlink, onStack, stack, components, counter);
        }
        return components;
    }

    /** 有环判定（任一组件 >1 成员或自环单点）。 */
    public boolean hasCycle() {
        return !cyclicVertices().isEmpty();
    }

    /** 环成员顶点集（升序——多组件环成员全并）。 */
    public List<Integer> cyclicVertices() {
        List<Integer> cyclic = new ArrayList<>();
        boolean[] selfLoop = new boolean[vertexCount];
        for (int v = 0; v < vertexCount; v++) {
            if (adjacency.get(v).contains(v)) {
                selfLoop[v] = true;
            }
        }
        for (List<Integer> component : components()) {
            if (component.size() > 1) {
                cyclic.addAll(component);
            } else {
                int solo = component.get(0);
                if (selfLoop[solo]) {
                    cyclic.add(solo);
                }
            }
        }
        return cyclic.stream().sorted().toList();
    }

    /** 迭代版强连通（帧栈代递归——深链不爆调用栈）。 */
    private int strongConnectIterative(int start, int[] index, int[] lowlink, boolean[] onStack,
            Deque<Integer> stack, List<List<Integer>> components, int counter) {
        record Frame(int vertex, int edgePos) {
        }
        Deque<Frame> frames = new ArrayDeque<>();
        index[start] = lowlink[start] = counter++;
        stack.push(start);
        onStack[start] = true;
        frames.push(new Frame(start, 0));
        while (!frames.isEmpty()) {
            Frame frame = frames.peek();
            int v = frame.vertex();
            List<Integer> edges = adjacency.get(v);
            if (frame.edgePos() < edges.size()) {
                int w = edges.get(frame.edgePos());
                frames.pop();
                frames.push(new Frame(v, frame.edgePos() + 1));
                if (index[w] == -1) {
                    index[w] = lowlink[w] = counter++;
                    stack.push(w);
                    onStack[w] = true;
                    frames.push(new Frame(w, 0));
                } else if (onStack[w]) {
                    lowlink[v] = Math.min(lowlink[v], index[w]);
                }
                continue;
            }
            frames.pop();
            if (lowlink[v] == index[v]) {
                List<Integer> component = new ArrayList<>();
                int w;
                do {
                    w = stack.pop();
                    onStack[w] = false;
                    component.add(w);
                } while (w != v);
                components.add(List.copyOf(component));
            }
            if (!frames.isEmpty()) {
                int parent = frames.peek().vertex();
                lowlink[parent] = Math.min(lowlink[parent], lowlink[v]);
            }
        }
        return counter;
    }

    private void requireInUniverse(int v) {
        if (v < 0 || v >= vertexCount) {
            throw new IndexOutOfBoundsException("顶点越界：" + v + "（宇宙 0.." + (vertexCount - 1) + "）");
        }
    }
}
