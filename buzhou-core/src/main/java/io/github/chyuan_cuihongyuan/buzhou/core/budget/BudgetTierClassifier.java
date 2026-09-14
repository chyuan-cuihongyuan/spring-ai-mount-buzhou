package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预算分档分类器（spec 1419 / T2139 / impl 1072）——k8s ResourceQuota
 * scope + Google SRE error budget headroom 思想：预算治理看板需要把
 * 「已用/上限」翻译成<b>行动档位</b>——GREEN（充足）/ WARN（软限预警，
 * 默认 80%）/ HARD（硬限已触或越过，默认 100%）；与预算闸
 * （ModelBudgetGate，执行时硬裁决）、J 会话预算闸读数（spec 1029，闸计数）、
 * BudgetRecommendation（spec 806，分位推荐档位）四者辨义：本面是无状态
 * 离线分类器，给看板/巡检报表用，不参与执行路径。
 *
 * <p>纯函数零状态：多预算维度（名 → used/limit 对）批量分类，输出逐维度
 * 档位 + 三档计数 + 最紧 Top（饱和度降序——「先看谁快烧完」第一眼）。
 * 阈值常量显式可辨；limit≤0 的畸形对判 UNKNOWN（不冒充 GREEN）。
 */
public final class BudgetTierClassifier {

    /** 软限预警阈值（用量/上限 ≥ 0.8 进入 WARN——SRE headroom 惯例）。 */
    public static final double WARN_RATIO = 0.8;

    /** 硬限阈值（≥ 1.0 即已触/越硬限）。 */
    public static final double HARD_RATIO = 1.0;

    /** 档位闭集：GREEN 充足 / WARN 软限预警 / HARD 硬限已触 / UNKNOWN 畸形输入。 */
    public enum Tier { GREEN, WARN, HARD, UNKNOWN }

    private BudgetTierClassifier() {
    }

    /**
     * @param budget   预算维度名
     * @param used     已用量
     * @param limit    上限（≤0 = 畸形对，UNKNOWN）
     * @param ratio    用量比 = used/limit（UNKNOWN 时 -1）
     * @param tier     档位
     */
    public record BudgetTierVerdict(String budget, long used, long limit,
                                    double ratio, Tier tier) {
    }

    /**
     * @param verdicts     逐维度判定（饱和度降序平名典序——最紧的排最前）
     * @param greenCount   GREEN 档计数
     * @param warnCount    WARN 档计数
     * @param hardCount    HARD 档计数
     * @param unknownCount UNKNOWN（畸形）计数
     */
    public record TierReport(List<BudgetTierVerdict> verdicts,
                             int greenCount, int warnCount, int hardCount,
                             int unknownCount) {

        /** 最紧 Top N（饱和度降序；UNKNOWN 不参与——无比率可排序）。 */
        public List<BudgetTierVerdict> tightest(int n) {
            return verdicts.stream()
                    .filter(v -> v.tier() != Tier.UNKNOWN)
                    .limit(Math.max(0, n))
                    .toList();
        }
    }

    /** 批量分类入口：预算维度名 → [used, limit]。 */
    public static TierReport classify(Map<String, long[]> usageByBudget) {
        List<BudgetTierVerdict> verdicts = usageByBudget.entrySet().stream()
                .map(e -> classifyOne(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(BudgetTierVerdict::ratio).reversed()
                        .thenComparing(BudgetTierVerdict::budget))
                .toList();
        int green = 0;
        int warn = 0;
        int hard = 0;
        int unknown = 0;
        for (BudgetTierVerdict v : verdicts) {
            switch (v.tier()) {
                case GREEN -> green++;
                case WARN -> warn++;
                case HARD -> hard++;
                case UNKNOWN -> unknown++;
            }
        }
        return new TierReport(List.copyOf(verdicts), green, warn, hard, unknown);
    }

    private static BudgetTierVerdict classifyOne(String budget, long[] pair) {
        long used = pair.length > 0 ? pair[0] : 0;
        long limit = pair.length > 1 ? pair[1] : 0;
        if (limit <= 0) {
            return new BudgetTierVerdict(budget, used, limit, -1d, Tier.UNKNOWN);
        }
        double ratio = (double) used / limit;
        Tier tier = ratio >= HARD_RATIO ? Tier.HARD
                : ratio >= WARN_RATIO ? Tier.WARN : Tier.GREEN;
        return new BudgetTierVerdict(budget, used, limit, ratio, tier);
    }
}
