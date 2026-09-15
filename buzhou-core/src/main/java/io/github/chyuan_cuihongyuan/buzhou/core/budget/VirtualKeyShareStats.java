package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 虚拟键份额读面（L 会话 1700 系 R18 = effort #1717 / spec 1717 /
 * 票 T2635 + T2636 / impl 1317）——OpenRouter 多键路由遥测 + 经济学 HHI
 * （赫芬达尔–赫希曼指数）思想：多把 {@link VirtualKeys} 轮着用，份额
 * 是否真被摊开、还是被某一把独吞——HHI（平方份额和，0..1）一数定集中度。
 *
 * <p>实例面线程安全：`record(key)` 逐次计入；`census()` 吐降序份额表 +
 * 总数 + HHI。键数 1 → HHI=1（完全集中）；均分 n 把 → HHI=1/n。
 *
 * @since 1.0.0
 */
public final class VirtualKeyShareStats {

    private final Map<String, AtomicLong> counts = new LinkedHashMap<>();

    /** 记一次键使用（null/空键按 "_anonymous_" 桶）。 */
    public void record(String key) {
        String bucket = key == null || key.isBlank() ? "_anonymous_" : key;
        counts.computeIfAbsent(bucket, k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * @param total         使用总数
     * @param shareDesc     键→计数（降序）
     * @param hhi           赫芬达尔集中度 Σ(份额²)，0..1；无样本哨兵 −1
     */
    public record ShareCensus(long total, Map<String, Long> shareDesc, double hhi) {
    }

    /** 普查快照（份额降序 + HHI）。 */
    public ShareCensus census() {
        long total = counts.values().stream().mapToLong(AtomicLong::get).sum();
        if (total == 0) {
            return new ShareCensus(0, Map.of(), -1d);
        }
        List<Map.Entry<String, Long>> desc = counts.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .toList();
        double hhi = 0d;
        for (Map.Entry<String, Long> entry : desc) {
            double share = (double) entry.getValue() / total;
            hhi += share * share;
        }
        Map<String, Long> ordered = desc.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
        // unmodifiableMap（非 Map.copyOf）：LinkedHashMap 降序必须保序——
        // copyOf 会丢序毁掉「降序份额表」契约
        return new ShareCensus(total, Collections.unmodifiableMap(ordered), hhi);
    }

    /** 测试归零。 */
    public void resetForTest() {
        counts.clear();
    }
}
