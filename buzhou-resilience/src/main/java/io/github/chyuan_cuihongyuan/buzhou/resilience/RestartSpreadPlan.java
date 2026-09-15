package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 重启错峰计划（spec 1820 / T2841 / impl 1421）——配置热重载/故障恢复后
 * **同批实例同时重试**是惊群（重启风暴：下游瞬时被打爆、又集体退避循环）；
 * memberlist/consul 协同重启错峰 + AWS Builders' Library jitter 思想：按
 * 实例 id 的稳定哈希分槽，把同批重启摊在错峰窗内。**确定性**是关键——
 * 无随机数、同 id 永远同槽（可回放审计、无状态），碰撞读数诚实入档
 * （哈希槽不保证无碰撞，碰撞即同槽同刻重启的风险面）。
 *
 * <p>纯函数零状态、只排程不执行（重启动作归宿主）。
 */
public final class RestartSpreadPlan {

    private RestartSpreadPlan() {
    }

    /**
     * 单实例错峰延迟。契约：cohortSize ≥ 1、spreadWindowMillis ≥ 1、id
     * 非空白（fail-fast）；语义：slot = |id.hashCode| mod cohortSize，
     * delay = slot × spreadWindow / cohortSize（返回 [0, spreadWindow)）。
     */
    public static long delayFor(String instanceId, int cohortSize, long spreadWindowMillis) {
        validate(instanceId, cohortSize, spreadWindowMillis);
        int slot = Math.floorMod(instanceId.hashCode(), cohortSize);
        return (long) slot * spreadWindowMillis / cohortSize;
    }

    /**
     * 同批普查：逐实例延迟 + 槽碰撞账（collision = 同槽实例数 −1 累计）。
     * null 按空表；逐 id 核契约。
     */
    public static CohortReport cohort(int cohortSize, long spreadWindowMillis,
                                      List<String> instanceIds) {
        validate("probe", cohortSize, spreadWindowMillis);
        List<String> cohort = instanceIds == null ? List.of() : instanceIds;
        Map<String, Long> delays = new LinkedHashMap<>();
        int[] slotCounts = new int[cohortSize];
        long maxDelay = 0;
        for (String id : cohort) {
            long delay = delayFor(id, cohortSize, spreadWindowMillis);
            delays.put(id, delay);
            maxDelay = Math.max(maxDelay, delay);
            slotCounts[Math.floorMod(id.hashCode(), cohortSize)]++;
        }
        long collisions = 0;
        for (int count : slotCounts) {
            if (count > 1) {
                collisions += count - 1;
            }
        }
        return new CohortReport(cohort.size(), delays, maxDelay, collisions);
    }

    private static void validate(String id, int cohortSize, long spreadWindowMillis) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("instanceId 不能为空");
        }
        if (cohortSize < 1) {
            throw new IllegalArgumentException("cohortSize 不能小于 1：" + cohortSize);
        }
        if (spreadWindowMillis < 1) {
            throw new IllegalArgumentException(
                    "spreadWindowMillis 不能小于 1：" + spreadWindowMillis);
        }
    }

    /** @param delays 逐实例延迟（入参序）；collisions 槽碰撞累计（风险面读数） */
    public record CohortReport(int instances, Map<String, Long> delays,
                               long maxDelay, long collisions) {

        /** 碰撞率 = collisions/instances（空批 -1 哨兵）。 */
        public double collisionRatio() {
            return instances == 0 ? -1d : (double) collisions / instances;
        }
    }
}
