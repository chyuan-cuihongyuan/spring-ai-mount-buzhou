package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 级联失败暴露读面（spec 1875 / T2951 / impl 1476）——级联失败
 *（cascading failure，Netflix/Hystrix 舱壁思想源头）分析惯例：依赖边
 * 权重 = 下游失败率 × 上游流量占比——**高权重边**即级联风险边（下游
 * 倒下砸死多少上游流量一目了然）；结合下游健康态分出「正在传导」的
 * 活风险与「埋着」的静风险。熔断器逐点防护，本读面回答「边拓扑上
 * 哪里最脆」。
 *
 * <p>纯函数零状态、只读不熔断（熔断动作归 ModelCircuitBreaker 族）。
 */
public final class CascadeExposure {

    private CascadeExposure() {
    }

    /** 依赖边契约：from/to 非空白、两率 ∈ [0,1]。 */
    public record Edge(String from, String to, double trafficShare,
                       double downstreamFailureRate) {

        public Edge {
            if (from == null || from.isBlank() || to == null || to.isBlank()
                    || Double.isNaN(trafficShare) || trafficShare < 0 || trafficShare > 1
                    || Double.isNaN(downstreamFailureRate) || downstreamFailureRate < 0
                    || downstreamFailureRate > 1) {
                throw new IllegalArgumentException(String.format(
                        "非法依赖边：%s -> %s（share=%s, failure=%s 须 ∈ [0,1]）",
                        from, to, trafficShare, downstreamFailureRate));
            }
        }

        /** 风险权重 = 流量占比 × 下游失败率（该边传导的期望损失面）。 */
        public double riskWeight() {
            return trafficShare * downstreamFailureRate;
        }
    }

    /** 下游健康态契约：node 非空白。 */
    public record NodeHealth(String node, boolean healthy) {

        public NodeHealth {
            if (node == null || node.isBlank()) {
                throw new IllegalArgumentException("node 不能为空白");
            }
        }
    }

    /**
     * @param edges            依赖边总数
     * @param activeRiskyEdges 命中不健康下游的风险边数（正在传导）
     * @param worstEdge        风险权重最高边 id（from->to；无边 null）
     * @param worstWeight      最高风险权重
     * @param totalWeight      全边权重合计（期望损失面总量）
     */
    public record Exposure(int edges, long activeRiskyEdges, String worstEdge,
                           double worstWeight, double totalWeight) {

        /** 活风险占比 = activeRisky/edges（无边 -1 哨兵）。 */
        public double activeRatio() {
            return edges == 0 ? -1d : (double) activeRiskyEdges / edges;
        }
    }

    /**
     * 暴露审计。null 任一按空表；并列最重取首（入参序）。
     */
    public static Exposure analyze(List<Edge> edges, List<NodeHealth> health) {
        List<Edge> edgeWindow = edges == null ? List.of() : edges;
        Map<String, Boolean> healthByNode = new HashMap<>();
        if (health != null) {
            for (NodeHealth h : health) {
                healthByNode.put(h.node(), h.healthy());
            }
        }
        long active = 0;
        double total = 0;
        String worst = null;
        double worstWeight = -1;
        for (Edge e : edgeWindow) {
            double weight = e.riskWeight();
            total += weight;
            boolean downstreamHealthy =
                    healthByNode.getOrDefault(e.to(), true);
            if (!downstreamHealthy && weight > 0) {
                active++;
            }
            if (weight > worstWeight) {
                worstWeight = weight;
                worst = e.from() + "->" + e.to();
            }
        }
        return new Exposure(edgeWindow.size(), active, worst, worstWeight, total);
    }
}
