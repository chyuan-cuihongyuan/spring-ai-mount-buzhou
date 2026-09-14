package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 实验分桶均衡审计（spec 1421 / T2143 / impl 1074）——A/A test（无处理
 * 对照实验）思想：哈希分桶的分配正确性最好的自证是「无差别的 key 流入后
 * 各桶份额是否均匀」——份额失衡 = 哈希/权重配置缺陷的先行信号，等指标
 * 显著性再发现就晚了。纯函数离线审计：吃观测到的分配计数（调用方从
 * {@code ExperimentBucketer} 暴露记录聚合），输出逐桶份额 + 均匀偏移 +
 * 均衡判定。
 *
 * <p>口径：均衡 = 每桶份额与均匀份额（1/bucketCount）的最大绝对偏差
 * ≤ {@link #BALANCE_TOLERANCE}（5pp——A/A 惯例容差）；单桶样本为零按失衡计
 * （空桶是最典型的权重配置错误）。纯函数零状态，不触 bucketer。
 */
public final class ExperimentBalanceAudit {

    /** 均衡容差：最大份额偏移（百分点，0.05 = 5pp）不超过即判均衡。 */
    public static final double BALANCE_TOLERANCE = 0.05;

    private ExperimentBalanceAudit() {
    }

    /**
     * @param buckets      桶标识（实验变体名）
     * @param assignments  分配计数
     * @param share        份额 = assignments/total
     * @param deviation    与均匀份额的绝对偏差（|share − 1/n|；按 0..1 计）
     */
    public record BucketShare(String bucket, long assignments, double share, double deviation) {
    }

    /**
     * @param buckets        逐桶份额（assignments 降序平名典序）
     * @param totalAssignments 总分配数（0 = 无样本，无从审计）
     * @param maxDeviation   最大份额偏移（无样本 -1 哨兵）
     * @param balanced       均衡判定（无样本 false——不冒充均衡）
     */
    public record BalanceReport(List<BucketShare> buckets, long totalAssignments,
                                double maxDeviation, boolean balanced) {
    }

    /** 审计入口：桶标识 → 观测分配计数（全部桶都须给出，缺桶 = 零分配失衡）。 */
    public static BalanceAuditBuilder forBuckets(List<String> bucketNames) {
        return new BalanceAuditBuilder(List.copyOf(bucketNames));
    }

    /** 两段式入口（先声明桶集合再喂计数——零桶计入失衡的关键）。 */
    public static final class BalanceAuditBuilder {
        private final List<String> bucketNames;

        private BalanceAuditBuilder(List<String> bucketNames) {
            this.bucketNames = bucketNames;
        }

        public BalanceReport withAssignments(Map<String, Long> assignments) {
            long total = assignments.values().stream().mapToLong(Long::longValue).sum();
            int n = bucketNames.size();
            Map<String, Long> counts = new LinkedHashMap<>();
            for (String name : bucketNames) {
                counts.put(name, assignments.getOrDefault(name, 0L));
            }
            if (total == 0 || n == 0) {
                return new BalanceReport(List.of(), total, -1d, false);
            }
            double uniform = (double) total / n;
            List<BucketShare> shares = counts.entrySet().stream()
                    .map(e -> {
                        double share = (double) e.getValue() / total;
                        double deviation = Math.abs(share - (double) 1 / n);
                        return new BucketShare(e.getKey(), e.getValue(), share, deviation);
                    })
                    .sorted(Comparator.comparingLong(BucketShare::assignments).reversed()
                            .thenComparing(BucketShare::bucket))
                    .toList();
            double maxDeviation = shares.stream()
                    .mapToDouble(BucketShare::deviation).max().orElse(0d);
            // 双比较卫生余量：份额恰在容差端点（如三桶 1150/1000/850 的整 5pp）不被
            // 二进制浮点表示误差推出界外
            return new BalanceReport(List.copyOf(shares), total, maxDeviation,
                    maxDeviation <= BALANCE_TOLERANCE + 1e-9);
        }
    }
}
