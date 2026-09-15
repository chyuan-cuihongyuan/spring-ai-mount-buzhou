package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.util.List;

/**
 * TTL 探针状态机（spec 1813 / T2827 / impl 1414）——Consul health check
 * TTL 思想：探针注册时声明 TTL，被探方须在窗内心跳续命——**过期不是靠
 * 巡检发现，而是靠时间自然到期**（无心跳即判定，零轮询成本）。三态：
 * PASSING（新鲜）/ STALE（过预警线未到期——续命在即的先兆）/ CRITICAL
 * （到期——探针失败）。freshness 读数（剩余新鲜度占比）回答「离翻脸还
 * 有多远」，比二值健康多给一个可运营的梯度。
 *
 * <p>纯函数零状态、只判态不执行（探活/摘除动作归宿主）。
 */
public final class TtlProbeStateMachine {

    private TtlProbeStateMachine() {
    }

    /** 三态：PASSING 新鲜 / STALE 过预警线未到期 / CRITICAL 到期。 */
    public enum ProbeState {

        /** 新鲜（age &lt; warnFraction × ttl）。 */
        PASSING,

        /** 过预警线未到期（warnFraction × ttl ≤ age &lt; ttl）——续命在即先兆。 */
        STALE,

        /** 到期（age ≥ ttl）——探针失败。 */
        CRITICAL
    }

    /**
     * 单探针判态。契约：age ≥ 0、ttl ≥ 1、0 ≤ warnFraction ≤ 1（fail-fast）。
     * 语义：age ≥ ttl 即 CRITICAL；age ≥ warnFraction × ttl 即 STALE；否则
     * PASSING（边界含上不含下——预警线含、到期线含）。
     */
    public static ProbeState evaluate(long ageMillis, long ttlMillis, double warnFraction) {
        validate(ageMillis, ttlMillis, warnFraction);
        if (ageMillis >= ttlMillis) {
            return ProbeState.CRITICAL;
        }
        if (ageMillis >= (long) (warnFraction * ttlMillis)) {
            return ProbeState.STALE;
        }
        return ProbeState.PASSING;
    }

    /** 剩余新鲜度 = 1 − age/ttl，到期后钳 0（负值不外泄）。 */
    public static double freshness(long ageMillis, long ttlMillis) {
        validate(ageMillis, ttlMillis, 0d);
        double ratio = 1d - (double) ageMillis / ttlMillis;
        return Math.max(0d, ratio);
    }

    /**
     * 探针普查。null 按空表；样本逐条核契约，畸形即 fail-fast。
     */
    public static Census census(long ttlMillis, double warnFraction, List<ProbeSample> samples) {
        List<ProbeSample> window = samples == null ? List.of() : samples;
        long passing = 0;
        long stale = 0;
        long critical = 0;
        for (ProbeSample s : window) {
            switch (evaluate(s.ageMillis(), ttlMillis, warnFraction)) {
                case PASSING -> passing++;
                case STALE -> stale++;
                case CRITICAL -> critical++;
            }
        }
        return new Census(window.size(), passing, stale, critical);
    }

    private static void validate(long ageMillis, long ttlMillis, double warnFraction) {
        if (ageMillis < 0) {
            throw new IllegalArgumentException("ageMillis 不能为负：" + ageMillis);
        }
        if (ttlMillis < 1) {
            throw new IllegalArgumentException("ttlMillis 不能小于 1：" + ttlMillis);
        }
        if (Double.isNaN(warnFraction) || warnFraction < 0 || warnFraction > 1) {
            throw new IllegalArgumentException(
                    "warnFraction 须在 [0,1]：" + warnFraction);
        }
    }

    /** 单探针样本：id 非空白 + 距上次心跳的毫秒数。 */
    public record ProbeSample(String id, long ageMillis) {

        public ProbeSample {
            if (id == null || id.isBlank() || ageMillis < 0) {
                throw new IllegalArgumentException(
                        "非法探针样本：id=" + id + ", age=" + ageMillis
                                + "（要求 id 非空白且 age ≥ 0）");
            }
        }
    }

    /** @param passing/stale/critical 三态计数（合计 = probes） */
    public record Census(int probes, long passing, long stale, long critical) {

        /** 到期占比（无探针 -1 哨兵）。 */
        public double criticalRatio() {
            return probes == 0 ? -1d : (double) critical / probes;
        }
    }
}
