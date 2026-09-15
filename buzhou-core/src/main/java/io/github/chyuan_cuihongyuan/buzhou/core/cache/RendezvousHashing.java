package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rendezvous 哈希（spec 1862 / T2925 / impl 1463）——Highest Random
 * Weight（HRW）思想：每个键对每个节点打分 score = mix(key, node)，取
 * 最高者归属——**节点增删只影响原本归属它的那 1/n 键**（其余键的最高
 * 分获得者不变），对照一致性哈希环无需虚节点、无需环管理。确定性哈希
 *（无随机数——同键同节点同分，可回放审计）；并列取字典序最小节点
 *（稳定可复现）。
 *
 * <p>纯函数零状态；只指派不路由（路由归宿主）。
 */
public final class RendezvousHashing {

    private RendezvousHashing() {
    }

    /**
     * 键归属指派。契约：nodes 非空、元素非空白、key 非空白（fail-fast）；
     * 语义：取 score 最高节点；并列取字典序最小。
     */
    public static String assign(String key, List<String> nodes) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key 不能为空");
        }
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("nodes 不能为空");
        }
        String best = null;
        long bestScore = Long.MIN_VALUE;
        for (String node : nodes) {
            if (node == null || node.isBlank()) {
                throw new IllegalArgumentException("node 不能为空");
            }
            long score = score(key, node);
            if (score > bestScore || (score == bestScore
                    && (best == null || node.compareTo(best) < 0))) {
                bestScore = score;
                best = node;
            }
        }
        return best;
    }

    /** 全量指派：keys → nodes（确定性）。 */
    public static Map<String, String> assignAll(List<String> keys, List<String> nodes) {
        if (keys == null) {
            throw new IllegalArgumentException("keys 不能为 null");
        }
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            result.put(key, assign(key, nodes));
        }
        return result;
    }

    /** 确定性评分：key×node 混合（黄金比扩散 + 异或扰动——同布隆口径）。 */
    private static long score(String key, String node) {
        long h = key.hashCode() * 0x9E3779B97F4A7C15L
                + node.hashCode() * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        return h;
    }
}
