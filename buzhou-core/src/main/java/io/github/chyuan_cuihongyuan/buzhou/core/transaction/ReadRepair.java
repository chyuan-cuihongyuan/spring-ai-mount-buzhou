package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.Comparator;
import java.util.List;

/**
 * Read Repair 读修复（spec 5018 / T6137 / impl 2169）——
 * Dynamo/Cassandra read repair 思想：N 副本版本汇报中取
 * **最高版本**为胜者（读返回值），低于胜者者入陈旧清单
 * （副本名字典序确定性排序）供调用方回写胜者值——读到旧
 * 版本不处理（撕裂持续）的病解。版本并列时副本名字典序最小
 * 者胜（确定性 tie-break）。
 *
 * <p>与 HintedHandoff（S11）同族不同面：读路径修复 vs 投递
 * 暂代。
 */
public final class ReadRepair {

    /**
     * 副本版本汇报。
     *
     * @param replica 副本名
     * @param version 数据版本（≥0）
     */
    public record Report(String replica, long version) {

        public Report {
            if (replica == null || replica.isEmpty()) {
                throw new IllegalArgumentException("副本名非空");
            }
            if (version < 0) {
                throw new IllegalArgumentException("版本需非负：" + replica + "/" + version);
            }
        }
    }

    /**
     * 修复裁决。
     *
     * @param winner 胜者汇报（读返回值来源）
     * @param staleReplicas 陈旧副本名（字典序——回写目标）
     */
    public record Stitch(Report winner, List<String> staleReplicas) {
    }

    private ReadRepair() {
    }

    /** 择优 + 陈旧清单（空汇报 ISE fail-fast）。 */
    public static Stitch stitch(List<Report> reports) {
        if (reports == null || reports.isEmpty()) {
            throw new IllegalArgumentException("汇报非空");
        }
        Report winner = reports.stream()
                .max(Comparator.comparingLong(Report::version)
                        .thenComparing(Report::replica, Comparator.reverseOrder()))
                .orElseThrow();
        long bestVersion = winner.version();
        List<String> stale = reports.stream()
                .filter(report -> report.version() < bestVersion)
                .map(Report::replica)
                .sorted()
                .toList();
        return new Stitch(winner, stale);
    }
}
