package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * HNSW 贪心层搜索（spec 4016 / T6033 / impl 2117）——近似最近邻
 * 分层图思想（Malkov-Yashunin 2016；hnswlib/faiss 同款）：节点按
 * 指数分布随机落层（高层稀疏长边跳大局、低层稠密短边精定位），
 * 查询自顶向下**贪心下降**到第 1 层，第 0 层 **beam 搜索**（ef 宽度
 * 候选池——比纯贪心不困于局部最优）；插入同样逐层 beam 找
 * efConstruction 候选取 M 近邻双向连接。
 *
 * <p>暴力 kNN O(n·d) 每查的全扫病在语义记忆/去重检索的规模场景
 * 不可持续——本件以对数级跳数换近似召回。距离用欧氏；与
 * MinHashSketch（哈希近重复）互补：本件稠密向量近邻、彼件集合
 * 相似度。
 */
public final class HnswBeamSearch {

    private record Neighbor(double dist, int id) {
    }

    private final int m;
    private final int efConstruction;
    private final Random random;
    private final Map<Integer, double[]> vectors = new HashMap<>();
    private final List<Map<Integer, Set<Integer>>> layers = new ArrayList<>();
    private Integer entryPoint;
    private int maxLevel = -1;

    /** 定构（m≥2、efConstruction≥m、random 非 null 否则 fail-fast）。 */
    public HnswBeamSearch(int m, int efConstruction, Random random) {
        if (m < 2 || efConstruction < m || random == null) {
            throw new IllegalArgumentException("m≥2 / ef≥m / random 非空：" + m + "/" + efConstruction);
        }
        this.m = m;
        this.efConstruction = efConstruction;
        this.random = random;
    }

    /** 插入（随机落层 + 逐层 beam 连 M 近邻；重复 id fail-fast）。 */
    public synchronized void add(int id, double[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("vector 非空");
        }
        if (vectors.containsKey(id)) {
            throw new IllegalArgumentException("id 重复：" + id);
        }
        if (entryPoint != null && vectors.get(entryPoint).length != vector.length) {
            throw new IllegalArgumentException("维度不一致");
        }
        vectors.put(id, vector.clone());
        int level = (int) Math.floor(-Math.log(Math.max(random.nextDouble(), 1e-12)) / Math.log(m));
        while (layers.size() <= level) {
            layers.add(new HashMap<>());
        }
        if (entryPoint == null) {
            entryPoint = id;
            maxLevel = level;
            for (int lc = 0; lc <= level; lc++) {
                layers.get(lc).put(id, new HashSet<>());
            }
            return;
        }
        int ep = entryPoint;
        for (int lc = maxLevel; lc > level; lc--) {   // 高层贪心下降（不连边）
            ep = searchLayer(vector, ep, 1, lc).get(0);
        }
        for (int lc = Math.min(level, maxLevel); lc >= 0; lc--) {   // 落层内 beam 连边
            layers.get(lc).putIfAbsent(id, new HashSet<>());
            List<Integer> candidates = searchLayer(vector, ep, efConstruction, lc);
            connect(id, candidates, vector, lc);
            ep = candidates.isEmpty() ? ep : candidates.get(0);
        }
        if (level > maxLevel) {
            maxLevel = level;
            entryPoint = id;
        }
    }

    /** 近邻查询（顶降贪心 + 第 0 层 beam；k>规模返全量）。 */
    public synchronized List<Integer> search(double[] query, int k) {
        if (query == null || query.length == 0) {
            throw new IllegalArgumentException("query 非空");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k>0：" + k);
        }
        if (entryPoint == null) {
            return List.of();
        }
        if (vectors.get(entryPoint).length != query.length) {
            throw new IllegalArgumentException("维度不一致");
        }
        int ep = entryPoint;
        for (int lc = maxLevel; lc >= 1; lc--) {
            ep = searchLayer(query, ep, 1, lc).get(0);
        }
        List<Integer> found = searchLayer(query, ep, Math.max(k, m), 0);
        return found.subList(0, Math.min(k, found.size()));
    }

    /** 节点数读数。 */
    public synchronized int size() {
        return vectors.size();
    }

    /** 当前最高层读数。 */
    public synchronized int maxLevel() {
        return maxLevel;
    }

    /** 单层 beam 搜索（ef 宽度候选池；返回按距离升序）。 */
    private List<Integer> searchLayer(double[] query, int entry, int ef, int layer) {
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Neighbor> candidates = new PriorityQueue<>(
                (a, b) -> a.dist() != b.dist() ? Double.compare(a.dist(), b.dist())
                        : Integer.compare(a.id(), b.id()));
        PriorityQueue<Neighbor> results = new PriorityQueue<>(
                (a, b) -> a.dist() != b.dist() ? Double.compare(b.dist(), a.dist())
                        : Integer.compare(b.id(), a.id()));
        double d0 = distance(query, vectors.get(entry));
        visited.add(entry);
        candidates.add(new Neighbor(d0, entry));
        results.add(new Neighbor(d0, entry));
        while (!candidates.isEmpty()) {
            Neighbor c = candidates.poll();
            double worst = results.peek().dist();
            if (c.dist() > worst && results.size() >= ef) {
                break;
            }
            for (int e : layers.get(layer).getOrDefault(c.id(), Set.of())) {
                if (!visited.add(e)) {
                    continue;
                }
                double d = distance(query, vectors.get(e));
                if (results.size() < ef || d < results.peek().dist()) {
                    candidates.add(new Neighbor(d, e));
                    results.add(new Neighbor(d, e));
                    if (results.size() > ef) {
                        results.poll();
                    }
                }
            }
        }
        List<Integer> out = new ArrayList<>();
        results.stream().sorted((a, b) -> a.dist() != b.dist() ? Double.compare(a.dist(), b.dist())
                : Integer.compare(a.id(), b.id())).forEach(n -> out.add(n.id()));
        return out;
    }

    /** 双向连 M 近邻（对端超限时按距离裁远端）。 */
    private void connect(int id, List<Integer> candidates, double[] vector, int layer) {
        Set<Integer> links = layers.get(layer).get(id);
        for (int i = 0; i < candidates.size() && links.size() < m; i++) {
            int other = candidates.get(i);
            if (other == id) {
                continue;
            }
            links.add(other);
            Set<Integer> reverse = layers.get(layer).computeIfAbsent(other, k -> new HashSet<>());
            reverse.add(id);
            if (reverse.size() > m) {
                int farthest = other;
                double far = -1;
                for (int r : reverse) {
                    double d = distance(vectors.get(other), vectors.get(r));
                    if (d > far) {
                        far = d;
                        farthest = r;
                    }
                }
                reverse.remove(farthest);
                if (farthest == id) {
                    links.remove(other);
                }
            }
        }
    }

    private static double distance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
