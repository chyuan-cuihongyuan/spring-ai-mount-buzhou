package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 合并压力读面（spec 1900 / T3001 / impl 1501）——ClickHouse
 * MergeTree「too many parts」语义：高频小写入攒出的活跃段逼近建议
 * 上限即告警、越过硬上限直接拒绝插入（安全阀）——写入碎片化的
 * 压力有刻度，拒绝不再是第一次暴露。
 *
 * <p>纯函数零状态；判定只读（合并执行归存储层）。
 */
public final class MergePressureReadout {

    private MergePressureReadout() {
    }

    /** 压力三态：OK → WARN → REJECT（边界含上）。 */
    public enum Pressure { OK, WARN, REJECT }

    /**
     * 压力占比读数：活跃段/建议上限。契约：activeParts ≥ 0、
     * recommendedMax ≥ 1（fail-fast）。
     */
    public static double pressure(int activeParts, int recommendedMax) {
        if (activeParts < 0) {
            throw new IllegalArgumentException(
                    "activeParts 不能为负：" + activeParts);
        }
        if (recommendedMax < 1) {
            throw new IllegalArgumentException(
                    "recommendedMax 不能小于 1：" + recommendedMax);
        }
        return (double) activeParts / recommendedMax;
    }

    /**
     * 三态判定：ratio ≥ 1.0 → REJECT；≥ warnAt → WARN；否则 OK。
     * 契约：warnAt ∈ (0,1]（fail-fast）。
     */
    public static Pressure verdict(double ratio, double warnAt) {
        if (warnAt <= 0.0 || warnAt > 1.0) {
            throw new IllegalArgumentException(
                    "warnAt 须在 (0,1]：" + warnAt);
        }
        if (ratio >= 1.0) {
            return Pressure.REJECT;
        }
        if (ratio >= warnAt) {
            return Pressure.WARN;
        }
        return Pressure.OK;
    }

    /**
     * 插入安全阀：活跃段越过硬上限即拒新写入（独立于建议线——
     * 建议线可 WARN 后仍写，硬上限是物理拒绝）。契约：hardMax ≥ 1
     * （fail-fast）。
     */
    public static boolean shouldRejectInsert(int activeParts, int hardMax) {
        if (activeParts < 0) {
            throw new IllegalArgumentException(
                    "activeParts 不能为负：" + activeParts);
        }
        if (hardMax < 1) {
            throw new IllegalArgumentException(
                    "hardMax 不能小于 1：" + hardMax);
        }
        return activeParts >= hardMax;
    }
}
