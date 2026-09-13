package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据集近重复读数（spec 847 / T1195，Cleanlab 数据质量思想；714 字符
 * trigram Jaccard 同源扩散到数据集治理）：对数据集用例输入做两两近重复
 * 对账——重复对/最大簇/唯一率——「评估集被重复用例稀释（分数虚高）」
 * 结构化。
 *
 * <p>纯函数：两两比较 O(n²)——条目封顶 {@value #MAX_ITEMS}（超出截断+
 * truncated 如实——诚实口径：大集先采样再审）；相似 = trigram Jaccard ≥
 * threshold（714 同款字符 trigram）；簇用并查集归并。
 */
public final class DatasetNearDuplicateStats {

    /** 参与两两比较的条目封顶。 */
    public static final int MAX_ITEMS = 200;
    /** 重复对明细封顶。 */
    public static final int MAX_PAIRS = 32;

    /** 不可变报告。 */
    public record Report(int itemsConsidered, int itemsTotal, boolean truncated,
                         int duplicatePairs, double uniqueRatio, int largestCluster,
                         List<String> duplicatePairsSample) {
    }

    private DatasetNearDuplicateStats() {
    }

    /** 近重复对账（threshold ∈ (0,1]；null/空白条目跳过——条目数照计）。 */
    public static Report analyze(List<String> items, double threshold) {
        if (threshold <= 0 || threshold > 1) {
            throw new IllegalArgumentException("threshold ∈ (0,1]（当前 " + threshold + "）");
        }
        int itemsTotal = items == null ? 0 : items.size();
        List<String> considered = new ArrayList<>();
        if (items != null) {
            for (String item : items) {
                if (item != null && !item.isBlank() && considered.size() < MAX_ITEMS) {
                    considered.add(item);
                }
            }
        }
        boolean truncated = itemsTotal > considered.size();

        int n = considered.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (trigramJaccard(considered.get(i), considered.get(j)) >= threshold) {
                    union(parent, i, j);
                    if (pairs.size() < MAX_PAIRS) {
                        pairs.add("#" + i + "~#" + j);
                    }
                }
            }
        }

        Map<Integer, Integer> clusterSizes = new HashMap<>();
        for (int i = 0; i < n; i++) {
            clusterSizes.merge(find(parent, i), 1, Integer::sum);
        }
        int largest = 0;
        for (int size : clusterSizes.values()) {
            largest = Math.max(largest, size);
        }
        int duplicatePairs = pairs.size() < MAX_PAIRS ? countPairs(parent, n) : pairs.size();
        double uniqueRatio = n == 0 ? 1.0 : (double) countRoots(parent, n) / n;
        return new Report(n, itemsTotal, truncated, duplicatePairs, uniqueRatio, largest,
                List.copyOf(pairs));
    }

    private static int countPairs(int[] parent, int n) {
        int pairs = 0;
        Map<Integer, Integer> sizes = new HashMap<>();
        for (int i = 0; i < n; i++) {
            sizes.merge(find(parent, i), 1, Integer::sum);
        }
        for (int size : sizes.values()) {
            pairs += size * (size - 1) / 2;
        }
        return pairs;
    }

    private static int countRoots(int[] parent, int n) {
        int roots = 0;
        for (int i = 0; i < n; i++) {
            if (find(parent, i) == i) {
                roots++;
            }
        }
        return roots;
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[rb] = ra;
        }
    }

    /** 字符 trigram Jaccard（714 同款；空集对 = 0）。 */
    static double trigramJaccard(String a, String b) {
        if (a.length() < 3 || b.length() < 3) {
            return a.equals(b) ? 1.0 : 0.0;
        }
        Map<String, Integer> ga = grams(a);
        Map<String, Integer> gb = grams(b);
        int intersection = 0;
        int union = 0;
        for (Map.Entry<String, Integer> e : ga.entrySet()) {
            int other = gb.getOrDefault(e.getKey(), 0);
            intersection += Math.min(e.getValue(), other);
            union += Math.max(e.getValue(), other);
        }
        for (Map.Entry<String, Integer> e : gb.entrySet()) {
            if (!ga.containsKey(e.getKey())) {
                union += e.getValue();
            }
        }
        return union == 0 ? 0 : (double) intersection / union;
    }

    private static Map<String, Integer> grams(String s) {
        Map<String, Integer> grams = new HashMap<>();
        for (int i = 0; i <= s.length() - 3; i++) {
            grams.merge(s.substring(i, i + 3), 1, Integer::sum);
        }
        return grams;
    }
}
