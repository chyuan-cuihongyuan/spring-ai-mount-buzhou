package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拓扑约束放置（spec 4024 / T6049 / impl 2125）——跨域散布思想
 * （K8s TopologySpreadConstraints maxSkew 语义）：每域记现有数，
 * 放置裁决看**放置后的全域斜度**（max−min）是否仍 ≤ maxSkew——
 * 拥挤域被截止、宽松域可进（「都堆热域」病的静态硬约束解）；
 * 候选序按（现有数升序，域名字典序）——先填最空，确定性可回放。
 *
 * <p>与 ConsistentHashRing（键空间路由）正交：本件管**容量面**
 * 的域间均衡（按计数不按键）。纯裁决无调度。
 */
public final class TopologySpreadPlacer {

    private final int maxSkew;

    /** 定构（maxSkew≥1 否则 fail-fast）。 */
    public TopologySpreadPlacer(int maxSkew) {
        if (maxSkew < 1) {
            throw new IllegalArgumentException("maxSkew≥1：" + maxSkew);
        }
        this.maxSkew = maxSkew;
    }

    /** 放置可行性（放入 domain 后全域斜度 ≤ maxSkew 即真）。 */
    public boolean canPlace(Map<String, Integer> counts, String domain) {
        validate(counts);
        if (domain == null || !counts.containsKey(domain)) {
            throw new IllegalArgumentException("domain 须在 counts 中");
        }
        return skewAfterPlacing(counts, domain) <= maxSkew;
    }

    /** 可放置域清单（现有数升序、并列域名序——先填最空确定性）。 */
    public List<String> eligibleDomains(Map<String, Integer> counts) {
        validate(counts);
        Map<String, Integer> sorted = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(java.util.Map.Entry.comparingByKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted.keySet().stream()
                .filter(d -> skewAfterPlacing(sorted, d) <= maxSkew)
                .toList();
    }

    /** 当前全域斜度（max−min；空图 0）。 */
    public int skewOf(Map<String, Integer> counts) {
        validate(counts);
        if (counts.isEmpty()) {
            return 0;
        }
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int v : counts.values()) {
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        return max - min;
    }

    /** maxSkew 读数。 */
    public int maxSkew() {
        return maxSkew;
    }

    private int skewAfterPlacing(Map<String, Integer> counts, String domain) {
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            int v = e.getKey().equals(domain) ? e.getValue() + 1 : e.getValue();
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        return max - min;
    }

    private static void validate(Map<String, Integer> counts) {
        if (counts == null) {
            throw new IllegalArgumentException("counts 非 null");
        }
        for (int v : counts.values()) {
            if (v < 0) {
                throw new IllegalArgumentException("计数非负");
            }
        }
    }
}
