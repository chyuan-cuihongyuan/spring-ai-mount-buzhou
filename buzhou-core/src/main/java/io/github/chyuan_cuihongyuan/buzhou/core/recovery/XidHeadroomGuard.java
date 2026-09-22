package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 事务号余量分级（spec 1896 / T2993 / impl 1497）——PostgreSQL
 * xid wraparound 防线：单调事务号逼近循环上限时分级告警，耗尽即
 * 停写自保（拒绝新事务强制清理）。分级让清理窗口有刻度：WARN 可
 * 排期、CRITICAL 必须动手、EXHAUSTED 已是最后防线。
 *
 * <p>纯函数零状态；判定只读不写（清理执行归 retention 面）。
 */
public final class XidHeadroomGuard {

    private XidHeadroomGuard() {
    }

    /** 紧迫度四级：OK → WARN → CRITICAL → EXHAUSTED（边界含上）。 */
    public enum Urgency { OK, WARN, CRITICAL, EXHAUSTED }

    /**
     * 分级判定：consumed ≥ limit → EXHAUSTED；≥ criticalAt →
     * CRITICAL；≥ warnAt → WARN；否则 OK。契约：limit ≥ 1、
     * 0 ≤ warnAt ≤ criticalAt ≤ limit、consumed ≥ 0（fail-fast）。
     */
    public static Urgency urgency(long consumed, long limit, long warnAt,
                                  long criticalAt) {
        validate(consumed, limit, warnAt, criticalAt);
        if (consumed >= limit) {
            return Urgency.EXHAUSTED;
        }
        if (consumed >= criticalAt) {
            return Urgency.CRITICAL;
        }
        if (consumed >= warnAt) {
            return Urgency.WARN;
        }
        return Urgency.OK;
    }

    /**
     * 余量读数：limit − consumed（负 = 已超发——诚实显示异常态）。
     */
    public static long headroom(long consumed, long limit) {
        if (consumed < 0) {
            throw new IllegalArgumentException("consumed 不能为负：" + consumed);
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit 不能小于 1：" + limit);
        }
        return limit - consumed;
    }

    private static void validate(long consumed, long limit, long warnAt,
                                 long criticalAt) {
        if (consumed < 0) {
            throw new IllegalArgumentException("consumed 不能为负：" + consumed);
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit 不能小于 1：" + limit);
        }
        if (warnAt < 0 || warnAt > criticalAt || criticalAt > limit) {
            throw new IllegalArgumentException(String.format(
                    "分级线须满足 0 ≤ warnAt ≤ criticalAt ≤ limit：%d, %d, %d",
                    warnAt, criticalAt, limit));
        }
    }
}
