package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

/**
 * 有界旧读（spec 1918 / T3037 / impl 1519）——Azure Cosmos DB
 * bounded staleness 一致性档：读允许落后写至多 bound 毫秒——
 * 「旧但有界」是强一致与最终一致之间可售的合同。旧度读数 + 界
 * 判定，让读副本的每次读都能回答「这次旧了多少、是否在合同内」。
 *
 * <p>纯函数零状态；时钟偏斜（read < lastWrite）旧度钳 0——负旧度
 * 无意义，诚实归零。
 */
public final class BoundedStaleness {

    private BoundedStaleness() {
    }

    /** 旧度判定：WITHIN_BOUND 合同内 / STALE 超界。 */
    public enum Staleness { WITHIN_BOUND, STALE }

    /**
     * 旧度读数：read − lastWrite 与 0 取大。契约：时点 ≥ 0
     * （fail-fast）。
     */
    public static long stalenessMillis(long lastWriteMillis, long readMillis) {
        if (lastWriteMillis < 0 || readMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "时点不能为负：lastWrite=%d, read=%d",
                    lastWriteMillis, readMillis));
        }
        return Math.max(0, readMillis - lastWriteMillis);
    }

    /**
     * 合同判定：旧度 ≤ bound → WITHIN_BOUND（边界含上——恰在界上
     * 仍达标）；否则 STALE。契约：bound ≥ 0（fail-fast）。
     */
    public static Staleness verdict(long stalenessMillis, long boundMillis) {
        if (boundMillis < 0) {
            throw new IllegalArgumentException(
                    "bound 不能为负：" + boundMillis);
        }
        return stalenessMillis <= boundMillis
                ? Staleness.WITHIN_BOUND : Staleness.STALE;
    }
}
