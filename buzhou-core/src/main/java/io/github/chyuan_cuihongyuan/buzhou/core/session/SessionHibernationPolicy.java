package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;

/**
 * 会话休眠分级（spec 1825 / T2851 / impl 1426）——k8s scale-to-zero /
 * duty-cycling 思想：空闲会话按闲置时长**分级回收足迹**——ACTIVE（全热，
 * 足迹 1.0）→ DROWSY（预降级，足迹减半、唤醒税轻）→ HIBERNATED（降冷，
 * 足迹一成、唤醒税重）。省下的内存/句柄是实时收益，唤醒税是显式代价
 * ——分级让「省多少 vs 醒多慢」可算而不是拍脑袋。
 *
 * <p>纯函数零状态、只判档不执行（降级/换页动作归宿主）。
 */
public final class SessionHibernationPolicy {

    /** DROWSY 档唤醒税（毫秒）——预降级恢复轻税。 */
    public static final long DROWSY_WAKE_TAX_MILLIS = 50L;

    /** HIBERNATED 档唤醒税（毫秒）——降冷恢复重税（重建工作集）。 */
    public static final long HIBERNATED_WAKE_TAX_MILLIS = 2000L;

    /** ACTIVE 档足迹比（全热基线）。 */
    public static final double ACTIVE_FOOTPRINT = 1.0d;

    /** DROWSY 档足迹比（预降级半足迹）。 */
    public static final double DROWSY_FOOTPRINT = 0.5d;

    /** HIBERNATED 档足迹比（降冷一成足迹）。 */
    public static final double HIBERNATED_FOOTPRINT = 0.1d;

    private SessionHibernationPolicy() {
    }

    /** 闲置档三态：ACTIVE 全热 / DROWSY 预降级 / HIBERNATED 降冷。 */
    public enum Band {

        /** 闲置未达软阈——全热服务。 */
        ACTIVE,

        /** 闲置过软阈——预降级（足迹减半、唤醒税轻）。 */
        DROWSY,

        /** 闲置过硬阈——降冷（足迹一成、唤醒税重）。 */
        HIBERNATED
    }

    /** 分级契约：0 ≤ drowsyAfter ≤ hibernateAfter（软阈不晚于硬阈）。 */
    public record Policy(long drowsyAfterMillis, long hibernateAfterMillis) {

        public Policy {
            if (drowsyAfterMillis < 0 || hibernateAfterMillis < drowsyAfterMillis) {
                throw new IllegalArgumentException(String.format(
                        "非法分级：drowsyAfter=%d, hibernateAfter=%d（要求 0 ≤ 软阈 ≤ 硬阈）",
                        drowsyAfterMillis, hibernateAfterMillis));
            }
        }
    }

    /**
     * 判档入口。契约：idleMillis ≥ 0（fail-fast）；语义：达软阈进 DROWSY、
     * 达硬阈进 HIBERNATED（边界含上）。
     */
    public static Band band(long idleMillis, Policy policy) {
        if (idleMillis < 0) {
            throw new IllegalArgumentException("idleMillis 不能为负：" + idleMillis);
        }
        if (idleMillis >= policy.hibernateAfterMillis()) {
            return Band.HIBERNATED;
        }
        if (idleMillis >= policy.drowsyAfterMillis()) {
            return Band.DROWSY;
        }
        return Band.ACTIVE;
    }

    /** 档位画像：唤醒税 + 足迹比（分级的两面对外可读）。 */
    public static BandProfile profile(long idleMillis, Policy policy) {
        Band band = band(idleMillis, policy);
        return switch (band) {
            case ACTIVE -> new BandProfile(band, 0L, ACTIVE_FOOTPRINT);
            case DROWSY -> new BandProfile(band, DROWSY_WAKE_TAX_MILLIS, DROWSY_FOOTPRINT);
            case HIBERNATED -> new BandProfile(band, HIBERNATED_WAKE_TAX_MILLIS,
                    HIBERNATED_FOOTPRINT);
        };
    }

    /**
     * 档位普查。null 按空表；逐会话核契约。
     *
     * @param active/drowsy/hibernated 三档计数（合计 = sessions）
     */
    public record Census(int sessions, long active, long drowsy, long hibernated) {

        /** 足迹节省率 = 1 − 实际足迹/全热足迹（无会话 -1 哨兵）。 */
        public double footprintReduction() {
            if (sessions == 0) {
                return -1d;
            }
            double actual = active * ACTIVE_FOOTPRINT + drowsy * DROWSY_FOOTPRINT
                    + hibernated * HIBERNATED_FOOTPRINT;
            return 1d - actual / sessions;
        }
    }

    /** 档位普查入口。 */
    public static Census census(Policy policy, List<Long> sessionIdleMillis) {
        List<Long> window = sessionIdleMillis == null ? List.of() : sessionIdleMillis;
        long active = 0;
        long drowsy = 0;
        long hibernated = 0;
        for (Long idle : window) {
            if (idle == null) {
                throw new IllegalArgumentException("闲置时长不能为 null");
            }
            switch (band(idle, policy)) {
                case ACTIVE -> active++;
                case DROWSY -> drowsy++;
                case HIBERNATED -> hibernated++;
            }
        }
        return new Census(window.size(), active, drowsy, hibernated);
    }

    /** 档位画像：band + wakeTaxMillis + footprintRatio。 */
    public record BandProfile(Band band, long wakeTaxMillis, double footprintRatio) {
    }
}
