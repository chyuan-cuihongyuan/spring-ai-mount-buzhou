package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * fork 树形态普查（L 会话 1700 系 R7 = effort #1706 / spec 1706 /
 * 票 T2613 + T2614 / impl 1306）——git DAG / GitHub network graph 的形态
 * 普查思想：fork 拓扑「有多深/多宽/多少叶子」是治理读面——深链提示级联
 * 上下文膨胀，宽节点提示扇出热点，叶子数给出活跃分支面。
 *
 * <p>纯函数零状态：吃 child→parent 全集映射（父缺省/为 null = 根），吐
 * 总数/根数/fork 数/最大深度/最大出度/叶子数。环路为调用方数据缺陷——
 * 抛 {@link IllegalArgumentException}（诚实拒绝而非死循环）。
 * 纯读面，与 {@link ForkLineageWalker}（行走/溯源）互补。
 *
 * @since 1.0.0
 */
public final class ForkShapeStats {

    private ForkShapeStats() {
    }

    /**
     * @param totalSessions 全部会话数（含根）
     * @param rootCount     根会话数（无父）
     * @param forkCount     fork 会话数（有父）
     * @param maxDepth      最长父链深度（根为 0；空表 −1 哨兵）
     * @param maxOutdegree  单节点最大子数（扇出热点）
     * @param leafCount     叶子数（无任何子节点）
     */
    public record ShapeReport(int totalSessions, int rootCount, int forkCount,
                              int maxDepth, int maxOutdegree, int leafCount) {
    }

    /** 普查入口：child→parent 映射全集（parent null = 根）。 */
    public static ShapeReport analyze(Map<String, String> childToParent) {
        Map<String, String> data = childToParent == null ? Map.of() : childToParent;
        int total = data.size();
        if (total == 0) {
            return new ShapeReport(0, 0, 0, -1, 0, 0);
        }
        int forkCount = 0;
        Map<String, Integer> outdegree = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                forkCount++;
                outdegree.merge(entry.getValue(), 1, Integer::sum);
            }
        }
        int maxOutdegree = outdegree.values().stream().max(Integer::compare).orElse(0);
        List<String> nodes = List.copyOf(data.keySet());
        int maxDepth = 0;
        for (String node : nodes) {
            int depth = 0;
            String cursor = data.get(node);
            while (cursor != null) {
                cursor = data.get(cursor);
                depth++;
                if (depth > total) {
                    throw new IllegalArgumentException("fork 拓扑含环（child→Parent 链不收敛）");
                }
            }
            maxDepth = Math.max(maxDepth, depth);
        }
        int leafCount = 0;
        for (String node : nodes) {
            if (!outdegree.containsKey(node)) {
                leafCount++;
            }
        }
        int rootCount = total - forkCount;
        return new ShapeReport(total, rootCount, forkCount, maxDepth, maxOutdegree, leafCount);
    }
}
