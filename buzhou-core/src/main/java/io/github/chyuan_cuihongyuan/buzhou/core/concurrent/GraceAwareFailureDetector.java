package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 冷启动豁免的 φ 嫌疑门（spec 2020 / T3141 / impl 1571）——StartupGrace
 * Tracker × PhiAccrualFailureDetector 组合件（K8s startup probe +
 * Hayashibara φ-accrual 的工程合体）：豁免窗内的失败**不喂 φ 模型**
 * （冷启动噪声不稀释真故障统计），高 φ 但豁免中只 GRACE_HOLD 观望
 * 不判死；毕业（首个成功心跳）或窗外才 CONFIRMED——「宽容冷启动，
 * 不放过真死」。
 *
 * <p>单对端一件（一检测目标一实例）；时间全由调用方传入（确定性
 * 可回放）；synchronized 小临界区。
 */
public final class GraceAwareFailureDetector {

    /** 三态判定：健康 / 高嫌疑但豁免中（观望）/ 高嫌疑确认失联。 */
    public enum Verdict {
        HEALTHY,
        GRACE_HOLD,
        CONFIRMED
    }

    private final PhiAccrualFailureDetector phi;
    private final StartupGraceTracker grace;
    private final String targetId;

    /** 契约：graceMillis &gt; 0、targetId 非空（fail-fast）。 */
    public GraceAwareFailureDetector(String targetId, long graceMillis) {
        if (targetId == null) {
            throw new IllegalArgumentException("targetId 不能为 null");
        }
        this.targetId = targetId;
        this.phi = new PhiAccrualFailureDetector();
        this.grace = new StartupGraceTracker(graceMillis);
        this.grace.begin(targetId, 0L);
    }

    /** 成功心跳：喂 φ 模型 + 首个成功即毕业（豁免结束）。 */
    public synchronized void heartbeat(long nowMillis) {
        phi.heartbeat(nowMillis);
        grace.graduate(targetId);
    }

    /**
     * 失败报告：豁免分流——豁免窗内未毕业的失败**不喂 φ**（冷启动噪声
     * 不进故障统计样本）；计账/豁免由 grace 分流裁决。
     */
    public synchronized void failure(long nowMillis) {
        grace.reportFailure(targetId, nowMillis);
        boolean inGrace = grace.activeGraces(nowMillis) > 0;
        if (!inGrace) {
            phi.heartbeat(nowMillis); // 非豁免失败以「超长间隔」形态喂模型（间隔异常拉高 φ）
        }
    }

    /**
     * 三态判定：φ &lt; threshold → HEALTHY；φ ≥ threshold 且豁免中 →
     * GRACE_HOLD（观望——冷启动宽容）；φ ≥ threshold 且已毕业/窗外 →
     * CONFIRMED（判失联）。契约：threshold &gt; 0。
     */
    public synchronized Verdict verdict(long nowMillis, double threshold) {
        if (!(threshold > 0) || Double.isNaN(threshold)) {
            throw new IllegalArgumentException("threshold 须 > 0：" + threshold);
        }
        double suspicion = phi.phi(nowMillis);
        if (suspicion < threshold) {
            return Verdict.HEALTHY;
        }
        return grace.activeGraces(nowMillis) > 0
                ? Verdict.GRACE_HOLD
                : Verdict.CONFIRMED;
    }

    /** 原始嫌疑度透传（φ 读数——观测面）。 */
    public synchronized double suspicion(long nowMillis) {
        return phi.phi(nowMillis);
    }

    /** 豁免账透传（豁免/计账/毕业——观测面）。 */
    public synchronized StartupGraceTracker.GraceStats graceStats() {
        return grace.stats();
    }
}
