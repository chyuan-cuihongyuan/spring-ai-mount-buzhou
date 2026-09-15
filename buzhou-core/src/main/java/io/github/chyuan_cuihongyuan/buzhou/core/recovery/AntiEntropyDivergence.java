package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.Map;

/**
 * 反熵分歧账（spec 1839 / T2879 / impl 1440）——Cassandra/Dynamo
 * anti-entropy repair + read repair 思想：副本健康不是「都活着」而是
 * 「数据一致」——分歧三型分开数：**只在主**（副本丢写——移交/重放候选）、
 * **只在副本**（主被清/回滚——对账决定去留）、**版本不合**（两边都有但
 * 版本不同——冲突解仲裁）。修复工作量 = 三型合计——反熵修复该排多大
 * 批、多久跑一轮，由分歧账说话。
 *
 * <p>纯函数零状态、只读不修复（修复执行归宿主）。
 */
public final class AntiEntropyDivergence {

    private AntiEntropyDivergence() {
    }

    /**
     * @param onlyInPrimary  只在主的键数（副本丢写）
     * @param onlyInReplica  只在副本的键数（主被清/回滚）
     * @param versionMismatch 两边都有但版本不合（冲突解候选）
     * @param matched        两边一致的键数
     */
    public record Divergence(long onlyInPrimary, long onlyInReplica,
                             long versionMismatch, long matched) {

        /** 修复工作量 = 三型分歧合计。 */
        public long repairWorkload() {
            return onlyInPrimary + onlyInReplica + versionMismatch;
        }

        /** 一致率 = matched/全键（空比对 -1 哨兵）。 */
        public double matchedRatio() {
            long total = repairWorkload() + matched;
            return total == 0 ? -1d : (double) matched / total;
        }
    }

    /**
     * 分歧账入口。null 按空表；键集与版本口径由调用方声明。
     */
    public static Divergence compare(Map<String, Long> primaryVersions,
                                     Map<String, Long> replicaVersions) {
        Map<String, Long> primary = primaryVersions == null ? Map.of() : primaryVersions;
        Map<String, Long> replica = replicaVersions == null ? Map.of() : replicaVersions;
        long onlyPrimary = 0;
        long mismatch = 0;
        long matched = 0;
        for (Map.Entry<String, Long> e : primary.entrySet()) {
            Long replicaVersion = replica.get(e.getKey());
            if (replicaVersion == null) {
                onlyPrimary++;
            } else if (replicaVersion.equals(e.getValue())) {
                matched++;
            } else {
                mismatch++;
            }
        }
        long onlyReplica = 0;
        for (String key : replica.keySet()) {
            if (!primary.containsKey(key)) {
                onlyReplica++;
            }
        }
        return new Divergence(onlyPrimary, onlyReplica, mismatch, matched);
    }
}
