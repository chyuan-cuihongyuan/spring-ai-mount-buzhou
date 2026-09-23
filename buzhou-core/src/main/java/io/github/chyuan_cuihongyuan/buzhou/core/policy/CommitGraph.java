package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 提交图世代号（spec 4043 / T6087 / impl 2144）——Git
 * commit-graph generation number 思想：gen(v) = max(父 gen)+1
 * （根 = 1），O(1) 读；祖先裁决 `isAncestor(a, b)` 先走**世代
 * 号剪枝快道**——gen(a) ≥ gen(b) 即确定非祖先（不遍历），否则
 * 有界 BFS（只走 gen &gt; gen(a) 的节点）精确判定。世代号精确
 * 单调无时钟偏斜——裸时间戳比对（commit-date 启发式被时钟倒
 * 挂打爆的教训）与全图遍历（大图代价爆炸）的病解。
 *
 * <p>拓扑序注册（父先注册）——DAG-by-construction；与
 * TopologicalSorter 同族不同面：全序排程 vs 血缘裁决。
 */
public final class CommitGraph {

    private final Map<String, Integer> generations = new HashMap<>();
    private final Map<String, List<String>> parents = new HashMap<>();

    /** 注册提交（拓扑序：父需已注册；未知父/重复 id fail-fast）。 */
    public void add(String id, List<String> parentIds) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id 非空");
        }
        if (generations.containsKey(id)) {
            throw new IllegalArgumentException("重复 id：" + id);
        }
        int generation = 0;
        for (String parent : parentIds) {
            Integer parentGeneration = generations.get(parent);
            if (parentGeneration == null) {
                throw new IllegalArgumentException("未知父引用：" + id + " → " + parent);
            }
            generation = Math.max(generation, parentGeneration);
        }
        generations.put(id, generation + 1);
        parents.put(id, List.copyOf(parentIds));
    }

    /** 世代号读数（未知 id fail-fast）。 */
    public int generationOf(String id) {
        Integer generation = generations.get(id);
        if (generation == null) {
            throw new IllegalArgumentException("未注册 id：" + id);
        }
        return generation;
    }

    /**
     * 祖先裁决（candidate 是否 descendant 的严格祖先；相等非祖先）。
     * gen 剪枝快道：gen(candidate) ≥ gen(descendant) 立即 false。
     */
    public boolean isAncestor(String candidate, String descendant) {
        int candidateGeneration = generationOf(candidate);
        int descendantGeneration = generationOf(descendant);
        if (candidateGeneration >= descendantGeneration) {
            return false;   // 世代号剪枝——候选不可能是后裔的祖先
        }
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(parents.get(descendant));
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (current.equals(candidate)) {
                return true;
            }
            if (generations.get(current) <= candidateGeneration) {
                continue;   // 世代号有界——此支不再可能经过 candidate
            }
            if (visited.add(current)) {
                pending.addAll(parents.get(current));
            }
        }
        return false;
    }

    /** 已注册提交数读数。 */
    public int size() {
        return generations.size();
    }
}
