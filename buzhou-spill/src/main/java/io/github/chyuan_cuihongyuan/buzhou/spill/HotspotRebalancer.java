package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 热点重平衡建议器（spec 1814 / T2829 / impl 1415）——K8s descheduler
 * 思想：不追求绝对均衡（代价大于收益），只消**越容差的热点**——贪心地
 * 从当前最热节点搬最小配额到最冷节点，直到 max−min 落入容差内。映射到
 * spill：节点可为分片/磁盘卷/存储层，负载可为 handle 数或字节数——热点
 * 分片拖慢全局时，建议单给出「从哪搬到哪搬多少」。
 *
 * <p>纯函数零状态、只建议不执行（搬迁动作归宿主）；确定性（并列取
 * id 字典序最小——同入参同出参，可回放审计）。
 */
public final class HotspotRebalancer {

    /** 搬迁步数上限（防长尾输入死循环的保险丝）。 */
    public static final int MAX_MOVES = 10_000;

    private HotspotRebalancer() {
    }

    /** 单节点负载契约：id 非空白、load ≥ 0。 */
    public record NodeLoad(String nodeId, long load) {

        public NodeLoad {
            if (nodeId == null || nodeId.isBlank() || load < 0) {
                throw new IllegalArgumentException(
                        "非法节点负载：id=" + nodeId + ", load=" + load
                                + "（要求 id 非空白且 load ≥ 0）");
            }
        }
    }

    /** 单条搬迁建议：从 from 搬 quantity 到 to。 */
    public record Move(String from, String to, long quantity) {
    }

    /**
     * @param moves        搬迁建议序列（执行序）
     * @param spreadBefore 建议前极差（max−min；少于两节点 0）
     * @param spreadAfter  全部执行后的极差
     */
    public record RebalancePlan(List<Move> moves, long spreadBefore, long spreadAfter) {

        /** 极差改善率 = (before−after)/before（before=0 时 -1 哨兵）。 */
        public double improvementRatio() {
            return spreadBefore == 0 ? -1d
                    : (double) (spreadBefore - spreadAfter) / spreadBefore;
        }
    }

    /**
     * 建议入口。契约：moveQuantum ≥ 1、tolerance ≥ 0（fail-fast）；null 按
     * 空表。语义：负载降序（并列 id 字典序）反复取最热/最冷，极差 ≤ 容差
     * 即停；单步量 = min(quantum, 极差−容差, 极差/2)（不越衡反转）；
     * 少于两节点或步量为 0 即空计划。
     */
    public static RebalancePlan suggest(long moveQuantum, long tolerance, List<NodeLoad> loads) {
        if (moveQuantum < 1) {
            throw new IllegalArgumentException("moveQuantum 不能小于 1：" + moveQuantum);
        }
        if (tolerance < 0) {
            throw new IllegalArgumentException("tolerance 不能为负：" + tolerance);
        }
        List<NodeLoad> window = new ArrayList<>();
        if (loads != null) {
            window.addAll(loads);
        }
        long spreadBefore = spread(window);
        if (window.size() < 2) {
            return new RebalancePlan(List.of(), spreadBefore, spreadBefore);
        }
        List<Move> moves = new ArrayList<>();
        List<NodeLoad> current = new ArrayList<>(window);
        current.sort(Comparator.comparingLong(NodeLoad::load).reversed()
                .thenComparing(NodeLoad::nodeId));
        while (moves.size() < MAX_MOVES) {
            NodeLoad hottest = current.get(0);
            NodeLoad coldest = current.get(current.size() - 1);
            long spread = hottest.load() - coldest.load();
            if (spread <= tolerance) {
                break;
            }
            long quantity = Math.min(moveQuantum, spread - tolerance);
            quantity = Math.min(quantity, spread / 2);
            if (quantity <= 0) {
                break;
            }
            moves.add(new Move(hottest.nodeId(), coldest.nodeId(), quantity));
            current.set(0, new NodeLoad(hottest.nodeId(), hottest.load() - quantity));
            current.set(current.size() - 1,
                    new NodeLoad(coldest.nodeId(), coldest.load() + quantity));
            current.sort(Comparator.comparingLong(NodeLoad::load).reversed()
                    .thenComparing(NodeLoad::nodeId));
        }
        return new RebalancePlan(List.copyOf(moves), spreadBefore, spread(current));
    }

    private static long spread(List<NodeLoad> loads) {
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        for (NodeLoad n : loads) {
            max = Math.max(max, n.load());
            min = Math.min(min, n.load());
        }
        return loads.isEmpty() ? 0 : max - min;
    }
}
