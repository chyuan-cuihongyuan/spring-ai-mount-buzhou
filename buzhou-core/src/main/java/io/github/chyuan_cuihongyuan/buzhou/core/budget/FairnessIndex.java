package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多租户配额公平指数（spec 1409 / T2119 / impl 1062）——Kafka client quota
 * 的公平性口径思想：配额治理先要回答「用量在租户间是均匀还是倾斜」。Jain
 * 公平指数 J = (Σx)² / (n·Σx²) ∈ (0,1]：1 = 完全均匀；配套 dominant share
 * （最大租户占比——单点吃满检测）与逐租户份额降序（行动面）。
 *
 * <p>纯函数零状态：消费方自采用量样本（会话数/请求量/token 花费皆可作 x 轴，
 * 口径由调用方声明）；零总量哨兵（全零样本无从谈公平，返回 -1 不冒充 1.0）。
 * 与 RouteDistributionReadout（H 805，路由权重倾斜/gini）辨义：那是「声明 vs
 * 实际」路由比对，本面是纯用量分布的公平性度量。
 */
public final class FairnessIndex {

    /** Jain 指数公平下限（电信配额惯例 J≥0.9 视为公平）。 */
    public static final double FAIR_FLOOR = 0.9;

    private FairnessIndex() {
    }

    /**
     * @param consumers     租户数
     * @param total         总用量（0 = 全零样本）
     * @param jain          Jain 公平指数 ∈ (0,1]；全零样本 = -1 哨兵
     * @param dominantShare 最大租户用量占比 [0,1]
     * @param shares        逐租户份额（用量降序，份额 = 用量/总量）
     */
    public record FairnessReport(int consumers, long total, double jain,
                                 double dominantShare, List<TenantShare> shares) {

        /** 公平判定（全零样本 = false——无从谈公平）。 */
        public boolean isFair() {
            return jain >= FAIR_FLOOR;
        }
    }

    /**
     * @param tenant 租户标识
     * @param usage  用量
     * @param share  份额 = usage/total
     */
    public record TenantShare(String tenant, long usage, double share) {
    }

    /** 计算入口：租户 → 用量样本（map 语义无序，报告份额降序）。 */
    public static FairnessReport of(Map<String, Long> usageByTenant) {
        int n = usageByTenant.size();
        if (n == 0) {
            return new FairnessReport(0, 0, -1d, 0d, List.of());
        }
        long total = usageByTenant.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return new FairnessReport(n, 0, -1d, 0d, List.of());
        }
        double sum = usageByTenant.values().stream().mapToLong(Long::longValue).sum();
        double sumSquares = usageByTenant.values().stream()
                .mapToLong(Long::longValue).mapToDouble(x -> (double) x * x).sum();
        double jain = (sum * sum) / (n * sumSquares);
        long max = usageByTenant.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<TenantShare> shares = usageByTenant.entrySet().stream()
                .map(e -> new TenantShare(e.getKey(), e.getValue(),
                        (double) e.getValue() / total))
                .sorted(Comparator.comparingLong(TenantShare::usage).reversed()
                        .thenComparing(TenantShare::tenant))
                .toList();
        return new FairnessReport(n, total, jain, (double) max / total,
                List.copyOf(shares));
    }

    /** 便捷重载：按会话归属统计的用量直方（调用方聚合后入参）。 */
    public static FairnessReport of(long[] usages) {
        Map<String, Long> named = new LinkedHashMap<>();
        for (int i = 0; i < usages.length; i++) {
            named.put("consumer-" + i, usages[i]);
        }
        return of(named);
    }
}
