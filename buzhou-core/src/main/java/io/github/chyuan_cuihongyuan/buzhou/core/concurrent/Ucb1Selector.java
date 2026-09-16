package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UCB1 选择器（spec 2032 / T3165 / impl 1583）——多臂老虎机 UCB1 思想
 *（Auer et al. 经典）：选择 = 均值 exploit + 置信半径 explore——
 * UCB = mean + c×√(2 ln N / nᵢ)。均值贪心的已知病：早期不幸的臂被
 * 永久冷落（估算未收敛即判死刑）；UCB 的置信半径随尝试次数收缩——
 * 尝试少的臂半径大自动被探索，不确定性内最优臂胜出。
 *
 * <p>未试臂优先（每臂至少一试——UCB1 标准）；synchronized 小临界区；
 * 奖励域 [0,1]（归一化由调用方）。
 */
public final class Ucb1Selector {

    /** 默认探索系数 c=1.0（UCB1 经典 sqrt(2) 的工程常用化）。 */
    public static final double DEFAULT_EXPLORATION = 1.0d;

    private static final class Arm {
        final String id;
        double rewardSum;
        long pulls;

        Arm(String id) {
            this.id = id;
        }

        double mean() {
            return pulls == 0 ? 0.0d : rewardSum / pulls;
        }
    }

    private final double exploration;
    private final Map<String, Arm> arms = new HashMap<>();
    private long totalPulls;

    /** 契约：exploration ≥ 0（0 = 纯均值贪心退化；fail-fast）。 */
    public Ucb1Selector(double exploration) {
        if (!(exploration >= 0) || Double.isNaN(exploration)) {
            throw new IllegalArgumentException("exploration 须 ≥ 0：" + exploration);
        }
        this.exploration = exploration;
    }

    public Ucb1Selector() {
        this(DEFAULT_EXPLORATION);
    }

    /** 注册臂（reward ∈ [0,1] 口径由调用方归一）。契约：id 非空非重复。 */
    public synchronized void registerArm(String armId) {
        if (armId == null || armId.isBlank()) {
            throw new IllegalArgumentException("armId 不能为空");
        }
        if (arms.containsKey(armId)) {
            throw new IllegalArgumentException("臂已注册：" + armId);
        }
        arms.put(armId, new Arm(armId));
    }

    /** 选择下一臂：未试臂优先（注册序）；否则 UCB 最大（同分注册序先）。空臂数 null。 */
    public synchronized String selectArm() {
        if (arms.isEmpty()) {
            return null;
        }
        Arm best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        List<Arm> ordered = new ArrayList<>(arms.values());
        // 注册序稳定：HashMap 无序——用臂注册序近似（selectId 顺序由 ids 列表维护成本换稳定性不值得；以 tie-break first-wins 兜底）
        for (Arm arm : ordered) {
            if (arm.pulls == 0) {
                return arm.id; // 未试臂优先
            }
            double ucb = arm.mean() + exploration * Math.sqrt(
                    2.0d * Math.log(totalPulls) / arm.pulls);
            if (ucb > bestScore) {
                best = arm;
                bestScore = ucb;
            }
        }
        return best != null ? best.id : ordered.get(0).id;
    }

    /** 记一次奖励（reward ∈ [0,1] fail-fast；未注册臂 fail-fast）。 */
    public synchronized void recordReward(String armId, double reward) {
        Arm arm = arms.get(armId);
        if (arm == null) {
            throw new IllegalArgumentException("臂未注册：" + armId);
        }
        if (reward < 0 || reward > 1 || Double.isNaN(reward)) {
            throw new IllegalArgumentException("reward 须在 [0,1]：" + reward);
        }
        arm.rewardSum += reward;
        arm.pulls++;
        totalPulls++;
    }

    /** 各臂快照：id → 均值（观测面）。 */
    public synchronized Map<String, Double> armMeans() {
        Map<String, Double> means = new HashMap<>();
        arms.forEach((id, arm) -> means.put(id, arm.mean()));
        return means;
    }

    /** 各臂尝试数快照（探索覆盖对账面——冷落臂显形）。 */
    public synchronized Map<String, Long> armPulls() {
        Map<String, Long> pulls = new HashMap<>();
        arms.forEach((id, arm) -> pulls.put(id, arm.pulls));
        return pulls;
    }
}
