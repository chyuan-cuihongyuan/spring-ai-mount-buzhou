package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 并查集（spec 3002 / T5005 / impl 2003）——Union-Find 思想
 * （路径减半 + 按秩合并，Tarjan 均摊近似 O(α(n)) 反阿克曼）：动态
 * 等价类的传递闭包单遍口径——union 声明同源、find 判同根、
 * connected 连通性传递闭环；组件计数只随**有效**合并递减（本已
 * 同根的冗余合并返回 false 不动账面）。会话/实体归并、重复键
 * 消解（「A=B、B=C ⇒ A=C」类闭环）的地基件。
 *
 * <p>固定 int 宇宙（capacity 定容）；非线程安全（单线程合并口径，
 * 并行分片各自建集后按根合并归后续轮）。
 */
public final class DisjointSet {

    private final int[] parent;
    private final int[] rank;
    private final int[] size;
    private int componentCount;

    /** 定容构造：0..capacity−1 各自为单例组件。 */
    public DisjointSet(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity 非负：" + capacity);
        }
        parent = new int[capacity];
        rank = new int[capacity];
        size = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            parent[i] = i;
            size[i] = 1;
        }
        componentCount = capacity;
    }

    /** 根查找（路径减半：每步跳到祖父——摊还压平树，均摊近似 O(α(n))）。 */
    public int find(int x) {
        requireInUniverse(x);
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    /**
     * 合并声明：a、b 同源。本已同根返回 false（账面不动）；有效
     * 合并返回 true——小组件挂大/等秩组件下（按秩），组件计数递减。
     */
    public boolean union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) {
            return false;
        }
        if (rank[rootA] < rank[rootB]) {
            int tmp = rootA;
            rootA = rootB;
            rootB = tmp;
        }
        parent[rootB] = rootA;
        size[rootA] += size[rootB];
        if (rank[rootA] == rank[rootB]) {
            rank[rootA]++;
        }
        componentCount--;
        return true;
    }

    /** 连通判定（同根即连通——传递性由单根结构保证）。 */
    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    /** 当前组件数（自 capacity 起，每有效合并减一）。 */
    public int componentCount() {
        return componentCount;
    }

    /** x 所在组件的大小（成员数）。 */
    public int sizeOf(int x) {
        return size[find(x)];
    }

    /** 定容宇宙大小。 */
    public int capacity() {
        return parent.length;
    }

    private void requireInUniverse(int x) {
        if (x < 0 || x >= parent.length) {
            throw new IndexOutOfBoundsException("元素越界：" + x + "（宇宙 0.." + (parent.length - 1) + "）");
        }
    }
}
