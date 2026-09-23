package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 稳定匹配（spec 4027 / T6055 / impl 2128）——延迟接受思想
 * （Gale-Shapley 1962；NRMP 实配算法）：求婚方按序出价、受婚方
 * **持有当前最优**并拒绝余者（拒绝者继续下一选）——有限步收敛到
 * **稳定匹配**（不存在双方都更愿意离开现配的阻塞对）；结果对
 * 求婚方最优（对受婚方最劣——主动方优势的定量体现）。
 *
 * <p>模型-路由偏好对接、agent-槽位分派等双边偏好的「稳定」语义
 * 件——贪婪逐最大匹配会产出阻塞对（双方私下都更愿意换）病的
 * 根治。纯静态；自由求婚队列按入参序（确定性可回放）；不齐边
 * 与链尽者诚实不配。
 */
public final class StableMatching {

    private StableMatching() {
    }

    /** 求婚方→受婚方稳定匹配（未配者缺席于结果——不齐边/链尽诚实）。 */
    public static Map<String, String> match(Map<String, List<String>> proposerPrefs,
            Map<String, List<String>> acceptorPrefs) {
        if (proposerPrefs == null || acceptorPrefs == null) {
            throw new IllegalArgumentException("偏好表非 null");
        }
        for (List<String> prefs : proposerPrefs.values()) {
            if (prefs == null) {
                throw new IllegalArgumentException("求婚方偏好链非 null");
            }
        }
        for (List<String> prefs : acceptorPrefs.values()) {
            if (prefs == null) {
                throw new IllegalArgumentException("受婚方偏好链非 null");
            }
        }
        Map<String, Deque<String>> remaining = new LinkedHashMap<>();
        proposerPrefs.forEach((p, prefs) -> remaining.put(p, new ArrayDeque<>(prefs)));
        Map<String, Integer> rank = new HashMap<>();
        acceptorPrefs.forEach((a, prefs) -> {
            for (int i = 0; i < prefs.size(); i++) {
                rank.put(a + ">" + prefs.get(i), i);   // 受婚方序（小者优先）
            }
        });
        Deque<String> free = new ArrayDeque<>(proposerPrefs.keySet());
        Map<String, String> engagedTo = new LinkedHashMap<>();   // 受婚方 → 求婚方
        while (!free.isEmpty()) {
            String proposer = free.poll();
            Deque<String> wishlist = remaining.get(proposer);
            while (!wishlist.isEmpty()) {
                String acceptor = wishlist.poll();
                String current = engagedTo.get(acceptor);
                if (current == null) {
                    engagedTo.put(acceptor, proposer);   // 空位即持有
                    break;
                }
                Integer proposerRank = rank.get(acceptor + ">" + proposer);
                Integer currentRank = rank.get(acceptor + ">" + current);
                if (proposerRank != null && (currentRank == null || proposerRank < currentRank)) {
                    engagedTo.put(acceptor, proposer);   // 换更优——旧人重自由
                    free.add(current);
                    break;
                }   // 否则被拒——继续下一选
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        List<Map.Entry<String, String>> entries = new ArrayList<>(engagedTo.entrySet());
        for (Map.Entry<String, String> e : entries) {
            result.put(e.getValue(), e.getKey());   // 反转成 求婚方→受婚方
        }
        return result;
    }
}
