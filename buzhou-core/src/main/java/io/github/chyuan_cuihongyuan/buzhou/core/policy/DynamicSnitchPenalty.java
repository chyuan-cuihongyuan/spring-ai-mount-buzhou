package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态 snitch 惩罚（spec 4028 / T6057 / impl 2129）——副本延迟动
 * 态降权思想（Cassandra DynamicEndpointSnitch badness）：每副本
 * EWMA 平滑延迟（抖动不惊弓）；**显著慢于最快副本 ×threshold**
  者加固定罚分——排序把病副本推到队尾（读修复/一致性读优先选
 * 健康副本），恢复后（EWMA 回落）自动免罚复用；未见副本零知识
 * 不罚（NaN 诚实）。
 *
 * <p>与 TwoChoiceSelector（到达时刻两问取轻）互补：本件是**持续
 * 观测**的动态偏好面（EWMA + 罚分），彼件是单次选择面。
 */
public final class DynamicSnitchPenalty {

    private static final double ALPHA = 0.5;   // EWMA 平滑系数

    private final double threshold;
    private final double penaltyMillis;
    private final Map<String, Double> ewma = new LinkedHashMap<>();

    /** 定构（threshold>1、penalty≥0 否则 fail-fast）。 */
    public DynamicSnitchPenalty(double threshold, double penaltyMillis) {
        if (threshold <= 1.0 || penaltyMillis < 0) {
            throw new IllegalArgumentException("threshold>1 / penalty≥0：" + threshold + "/" + penaltyMillis);
        }
        this.threshold = threshold;
        this.penaltyMillis = penaltyMillis;
    }

    /** 延迟观测入账（EWMA 平滑——首见即基线）。 */
    public void record(String replica, double latencyMillis) {
        if (replica == null || replica.isEmpty() || latencyMillis < 0) {
            throw new IllegalArgumentException("replica 非空且 latency≥0");
        }
        Double current = ewma.get(replica);
        ewma.put(replica, current == null ? latencyMillis : current * (1 - ALPHA) + latencyMillis * ALPHA);
    }

    /** EWMA 读数（未见副本 NaN 诚实）。 */
    public double ewmaOf(String replica) {
        return ewma.getOrDefault(replica, Double.NaN);
    }

    /** 罚判定（显著慢于最快副本 ×threshold；未见/最快者恒假）。 */
    public boolean penalized(String replica) {
        Double value = ewma.get(replica);
        if (value == null) {
            return false;
        }
        double min = minEwma();
        return value > min * threshold;
    }

    /** 排序分（EWMA + 罚分——病副本推队尾；未见 NaN 诚实）。 */
    public double scoreOf(String replica) {
        Double value = ewma.get(replica);
        if (value == null) {
            return Double.NaN;
        }
        return value + (penalized(replica) ? penaltyMillis : 0);
    }

    /** 副本排序（score 升序、并列按观测入账序——确定性）。 */
    public List<String> ranking() {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(ewma.entrySet());
        entries.sort(Comparator.comparingDouble(e -> scoreOf(e.getKey())));
        return entries.stream().map(Map.Entry::getKey).toList();
    }

    /** threshold 读数。 */
    public double threshold() {
        return threshold;
    }

    private double minEwma() {
        double min = Double.MAX_VALUE;
        for (double v : ewma.values()) {
            min = Math.min(min, v);
        }
        return min;
    }
}
