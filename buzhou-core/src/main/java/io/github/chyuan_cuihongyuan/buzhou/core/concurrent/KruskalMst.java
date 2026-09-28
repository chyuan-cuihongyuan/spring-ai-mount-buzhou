package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * Kruskal 最小生成树（spec 7008 / U7217 / impl 2260）——
 * Kruskal 1956 贪心思想：**边权升序扫描，两端点不连通即
 * 采纳**（并查集判环——复用同包 DisjointSet），n−1 边即止
 * ——总权最小且可证（割性质：每步采纳的是跨当前割的最小
 * 边）。边排序键 (weight,from,to) 全序——同图同树完全确定
 * （同权边按端点字典序，不靠排序器情绪）。不连通图 fail-fast
 * （生成树不存在——森林语义明示不做）。自环 fail-fast
 * （无环图不变量）。
 *
 * <p>与 DisjointSet（同包）同族不同面：判环原语 vs 原语的
 * 经典消费方（MST 本尊）；与 DijkstraShortestPath 不同面：
 * 全树最小连接 vs 单源最短路。
 */
public final class KruskalMst {

    private final int nodeCount;
    private final List<long[]> edges = new ArrayList<>();

    /** nodeCount≥1（越域 fail-fast）。 */
    public KruskalMst(int nodeCount) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        this.nodeCount = nodeCount;
    }

    /** 加无向边（自环 fail-fast；重边合法——排序折叠交给扫描）。 */
    public void addEdge(int a, int b, long weight) {
        checkNode(a);
        checkNode(b);
        if (a == b) {
            throw new IllegalArgumentException("自环不参与生成树: " + a);
        }
        edges.add(new long[]{weight, Math.min(a, b), Math.max(a, b)});
    }

    /**
     * 最小生成树边集（[weight,小端,大端]，扫描采纳序；
     * 不连通 fail-fast——生成树不存在）。
     */
    public List<long[]> minimumSpanningTree() {
        List<long[]> sorted = new ArrayList<>(edges);
        sorted.sort((x, y) -> {
            for (int i = 0; i < 3; i++) {
                if (x[i] != y[i]) {
                    return Long.compare(x[i], y[i]);
                }
            }
            return 0;
        });
        DisjointSet dsu = new DisjointSet(nodeCount);
        List<long[]> picked = new ArrayList<>();
        for (long[] edge : sorted) {
            if (dsu.union((int) edge[1], (int) edge[2])) {
                picked.add(edge);
                if (picked.size() == nodeCount - 1) {
                    return picked;
                }
            }
        }
        throw new IllegalArgumentException("图不连通——生成树不存在（采纳 "
                + picked.size() + "/" + (nodeCount - 1) + "）");
    }

    /** 最小生成树总权（不连通 fail-fast）。 */
    public long totalWeight() {
        long total = 0;
        for (long[] edge : minimumSpanningTree()) {
            total += edge[0];
        }
        return total;
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return nodeCount;
    }

    /** 边数读数。 */
    public int edgeCount() {
        return edges.size();
    }

    private void checkNode(int node) {
        if (node < 0 || node >= nodeCount) {
            throw new IllegalArgumentException("节点越域 [0," + nodeCount + "): " + node);
        }
    }
}
