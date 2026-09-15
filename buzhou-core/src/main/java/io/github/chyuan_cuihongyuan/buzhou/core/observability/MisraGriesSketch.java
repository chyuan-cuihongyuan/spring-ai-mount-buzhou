package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Misra-Gries 频项素描（spec 1848 / T2897 / impl 1449）——流式 heavy
 * hitters 检测经典算法：O(k) 内存找**超过 N/k 的一切频繁项**——计满 k−1
 * 个计数器后新项出现即全员减一（「抵消一轮投票」），幸存者即候选。计数
 * 为**下界**（真实计数 − 估计 ≤ N/k），确定性（无哈希无随机——对照
 * Count-Min Sketch 的概率口径）。映射到会话/工具流：单遍扫出真热点
 * （频繁会话/热点工具），不用全量计数。
 *
 * <p>纯函数零状态、确定性可回放；只素描不裁决（热点处置归宿主）。
 */
public final class MisraGriesSketch {

    private MisraGriesSketch() {
    }

    /**
     * 素描入口。契约：k ≥ 2（k−1 个计数器——频项阈 1/k）、stream 元素非
     * null（fail-fast）；null 流按空表。
     *
     * @return 候选频项 → 低估计数（确定性；幸存者至多 k−1 个）
     */
    public static Map<String, Long> sketch(int k, List<String> stream) {
        if (k < 2) {
            throw new IllegalArgumentException("k 不能小于 2：" + k);
        }
        List<String> window = stream == null ? List.of() : stream;
        Map<String, Long> counters = new LinkedHashMap<>();
        for (String item : window) {
            if (item == null) {
                throw new IllegalArgumentException("流元素不能为 null");
            }
            if (counters.containsKey(item)) {
                counters.merge(item, 1L, Long::sum);
            } else if (counters.size() < k - 1) {
                counters.put(item, 1L);
            } else {
                // 计满：全员减一（抵消一轮投票），归零淘汰
                counters.replaceAll((key, v) -> v - 1);
                counters.values().removeIf(v -> v == 0);
            }
        }
        return Map.copyOf(counters);
    }

    /**
     * 候选查询：item 是否幸存为频项候选（count ≥ 1 即在素描中）。
     */
    public static boolean isHeavyCandidate(String item, Map<String, Long> sketch) {
        return sketch != null && sketch.getOrDefault(item, 0L) >= 1;
    }
}
