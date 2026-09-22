package io.github.chyuan_cuihongyuan.buzhou.core.health;

/**
 * 探测流量预算（spec 1912 / T3025 / impl 1513）——SRE 健康检查
 * 预算惯例：探测本身也是流量，实例数上涨后探测 QPS 线性膨胀——
 * 「探活变压死」的事故用占比读数事前可见。
 *
 * <p>纯函数零状态；不做真实探测执行（归健康面）。
 */
public final class ProbeBudget {

    private ProbeBudget() {
    }

    /** 预算判定：WITHIN 预算内 / OVER 超预算（降频或扩容）。 */
    public enum Verdict { WITHIN, OVER }

    /**
     * 探测流量占比：探测 QPS / 服务容量 QPS。契约：probeQps ≥ 0、
     * capacityQps ≥ 1（fail-fast）。
     */
    public static double probeShare(long probeQps, long capacityQps) {
        if (probeQps < 0) {
            throw new IllegalArgumentException("probeQps 不能为负：" + probeQps);
        }
        if (capacityQps < 1) {
            throw new IllegalArgumentException(
                    "capacityQps 不能小于 1：" + capacityQps);
        }
        return (double) probeQps / capacityQps;
    }

    /**
     * 预算判定：share ≤ maxShare → WITHIN；> maxShare → OVER
     * （边界含上——恰在预算内不算超）。契约：maxShare ∈ (0,1]
     * （fail-fast）。
     */
    public static Verdict verdict(double share, double maxShare) {
        if (maxShare <= 0.0 || maxShare > 1.0) {
            throw new IllegalArgumentException(
                    "maxShare 须在 (0,1]：" + maxShare);
        }
        return share <= maxShare ? Verdict.WITHIN : Verdict.OVER;
    }
}
