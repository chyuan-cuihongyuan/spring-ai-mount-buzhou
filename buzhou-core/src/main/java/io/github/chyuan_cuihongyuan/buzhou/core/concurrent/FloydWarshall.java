package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * Floyd-Warshall 全对最短路（spec 7007 / U7215 / impl 2259）——
 * Floyd 1962 / Warshall 1962 动态规划思想：**经中转点 k
 * 逐层闭包** d[i][j]=min(d[i][j], d[i][k]+d[k][j])，三重
 * 循环 O(V³) 一次得全对距离——单源跑 n 遍 Bellman-Ford
 * O(V²E)（稠密图放大）的病解。支持负权边；**对角负值即
 * 负环 fail-fast**（距离无定义诚实拒绝）；不可达 = {@link #UNREACHABLE}。
 *
 * <p>与 BellmanFord（同包）同族不同面：单源全松弛 O(VE)
 * vs 全对中转闭包 O(V³)；与 DijkstraShortestPath 不同面：
 * 贪心单源 vs 动规全对。
 */
public final class FloydWarshall {

    private final long[][] dist;
    private int edgeCount;

    /** 不可达哨兵（负权下 -1 会与真实距离冲突——专用哨兵）。 */
    public static final long UNREACHABLE = BellmanFord.UNREACHABLE;

    /** nodeCount≥1 建零图（越域 fail-fast）。 */
    public FloydWarshall(int nodeCount) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("节点数须为正: " + nodeCount);
        }
        this.dist = new long[nodeCount][nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            java.util.Arrays.fill(dist[i], UNREACHABLE);
            dist[i][i] = 0L;
        }
    }

    /** 加有向边（多边取最小折叠；负权合法；越域 fail-fast）。 */
    public void addEdge(int from, int to, long weight) {
        checkNode(from);
        checkNode(to);
        if (dist[from][to] == UNREACHABLE || weight < dist[from][to]) {
            if (dist[from][to] == UNREACHABLE) {
                edgeCount++;
            }
            dist[from][to] = weight;
        }
    }

    /**
     * 全对最短距离矩阵（不可达=-1；对角负值=负环
     * fail-fast——距离无定义诚实拒绝）。返回副本，内部状态
     * 不外泄。
     */
    public long[][] allDistances() {
        int n = dist.length;
        long[][] d = new long[n][];
        for (int i = 0; i < n; i++) {
            d[i] = dist[i].clone();
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (d[i][k] == UNREACHABLE) {
                    continue;
                }
                for (int j = 0; j < n; j++) {
                    if (d[k][j] == UNREACHABLE) {
                        continue;
                    }
                    long candidate = d[i][k] + d[k][j];
                    if (d[i][j] == UNREACHABLE || candidate < d[i][j]) {
                        d[i][j] = candidate;
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (d[i][i] < 0) {
                throw new IllegalArgumentException("负环——距离无定义（对角 " + i + "）");
            }
        }
        return d;
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return dist.length;
    }

    /** 边数读数（多边折叠后）。 */
    public int edgeCount() {
        return edgeCount;
    }

    private void checkNode(int node) {
        if (node < 0 || node >= dist.length) {
            throw new IllegalArgumentException("节点越域 [0," + dist.length + "): " + node);
        }
    }
}
