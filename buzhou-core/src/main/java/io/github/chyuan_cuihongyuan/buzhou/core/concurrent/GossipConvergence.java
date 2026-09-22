package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 闲谈收敛估算（spec 1909 / T3019 / impl 1510）——SWIM/memberlist
 * gossip 传播语义：每知情节点每轮向 f 个同伴闲谈，知情集合每轮
 * ×(1+f) 指数增长——N 节点全量知情需 ⌈log_{f+1} N⌉ 轮。指数模型
 * 是下界估计（真实传播有重叠冗余，实际轮数 ≥ 估计——诚实口径）。
 *
 * <p>纯函数零状态；三函数互逆可交叉验证。
 */
public final class GossipConvergence {

    private GossipConvergence() {
    }

    /**
     * 全量知情轮数：⌈log_{f+1} N⌉。契约：nodes ≥ 1、fanout ≥ 1
     * （fail-fast）。
     */
    public static int roundsToConverge(int nodes, int fanout) {
        if (nodes < 1) {
            throw new IllegalArgumentException("nodes 不能小于 1：" + nodes);
        }
        if (fanout < 1) {
            throw new IllegalArgumentException("fanout 不能小于 1：" + fanout);
        }
        double base = fanout + 1.0;
        int rounds = (int) Math.ceil(Math.log(nodes) / Math.log(base));
        return Math.max(1, rounds); // nodes ≥ 2 时至少 1 轮
    }

    /**
     * r 轮后知情节点数估计：min(N, (1+f)^r)。契约：rounds ≥ 0
     * （fail-fast）。
     */
    public static int informedAfter(int nodes, int fanout, int rounds) {
        if (nodes < 1) {
            throw new IllegalArgumentException("nodes 不能小于 1：" + nodes);
        }
        if (fanout < 1) {
            throw new IllegalArgumentException("fanout 不能小于 1：" + fanout);
        }
        if (rounds < 0) {
            throw new IllegalArgumentException("rounds 不能为负：" + rounds);
        }
        long informed = 1;
        for (int i = 0; i < rounds; i++) {
            informed *= (fanout + 1L);
            if (informed >= nodes) {
                return nodes;
            }
        }
        return (int) informed;
    }

    /**
     * fanout 反解：要在 maxRounds 轮内传遍 N 节点，fanout 至少
     * ⌈N^{1/R} − 1⌉。契约：nodes ≥ 1、maxRounds ≥ 1（fail-fast）。
     */
    public static int fanoutFor(int nodes, int maxRounds) {
        if (nodes < 1) {
            throw new IllegalArgumentException("nodes 不能小于 1：" + nodes);
        }
        if (maxRounds < 1) {
            throw new IllegalArgumentException("maxRounds 不能小于 1：" + maxRounds);
        }
        int fanout = (int) Math.ceil(Math.pow(nodes, 1.0 / maxRounds) - 1.0);
        return Math.max(1, fanout);
    }
}
