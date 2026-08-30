package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 平滑加权路由器（spec 199 / T571，Nginx smooth weighted round-robin 借鉴）：
 * 每候选 current += weight，取最大者选中并 current -= totalWeight——比例精确、
 * 时间平滑（5:1:1 不连五爆发）、同序可复现。setWeight 动态调权零重建；
* 泛型候选（模型/工具实例）；单锁轻临界区。
 */
public final class WeightedRouter<T> {

    private static final class Candidate<T> {
        final T value;
        int weight;
        int current;

        Candidate(T value, int weight) {
            this.value = value;
            this.weight = weight;
        }
    }

    private final Map<T, Candidate<T>> candidates = new LinkedHashMap<>();

    /** 构造（有序 (候选, 权重>0) 表；重复候选后者覆盖）。 */
    @SafeVarargs
    public static <T> WeightedRouter<T> of(Pair<T>... entries) {
        WeightedRouter<T> router = new WeightedRouter<>();
        for (Pair<T> entry : entries) {
            router.add(entry.value(), entry.weight());
        }
        return router;
    }

    /** 候选-权重对。 */
    public record Pair<T>(T value, int weight) {
        public static <T> Pair<T> of(T value, int weight) {
            return new Pair<>(value, weight);
        }
    }

    public synchronized void add(T value, int weight) {
        if (value == null || weight <= 0) {
            throw new IllegalArgumentException("候选非空、权重>0（当前 " + weight + "）");
        }
        candidates.put(value, new Candidate<>(value, weight));
    }

    /** 动态调权（未知候选即 add；权重<=0 拒绝）。 */
    public synchronized void setWeight(T value, int weight) {
        if (value == null || weight <= 0) {
            throw new IllegalArgumentException("候选非空、权重>0");
        }
        Candidate<T> candidate = candidates.get(value);
        if (candidate == null) {
            candidates.put(value, new Candidate<>(value, weight));
        } else {
            candidate.weight = weight;
        }
    }

    /** 平滑加权选一个（空表 = empty）。 */
    public synchronized Optional<T> pick() {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        int total = 0;
        Candidate<T> best = null;
        for (Candidate<T> candidate : candidates.values()) {
            candidate.current += candidate.weight;
            total += candidate.weight;
            if (best == null || candidate.current > best.current) {
                best = candidate;
            }
        }
        best.current -= total;
        return Optional.of(best.value);
    }

    /** 权重观测（稳定序）。 */
    public synchronized Map<T, Integer> weights() {
        Map<T, Integer> out = new LinkedHashMap<>();
        candidates.forEach((value, candidate) -> out.put(value, candidate.weight));
        return out;
    }

    /** 候选数。 */
    public synchronized int size() {
        return candidates.size();
    }

    /** 连续 pick n 次的序列（测试/复现辅助）。 */
    public synchronized List<T> pickSequence(int n) {
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            pick().ifPresent(out::add);
        }
        return out;
    }
}
