package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

/**
 * 快速重传触发器（spec 2043 / T3187 / impl 1594）——TCP fast
 * retransmit（3 dup-ACK）思想：不等重传超时——**连续 N 个重复信号**
 * 即提前触发动作（连续同签名失败提前换道/重启探针，不等慢超时）；
 * 信号切换即重计（不同问题各自计数）；触发后计数清零（新一轮从零）。
 *
 * <p>synchronized 小临界区；纯信号驱动无时钟（确定性可回放）。
 */
public final class FastRetransmitTrigger {

    /** 默认重复阈值（TCP 惯例 3-dup-ACK）。 */
    public static final int DEFAULT_DUP_THRESHOLD = 3;

    private final int dupThreshold;
    private String lastSignalId;
    private int consecutiveDups;
    private long triggers;
    private long dupSignals;
    private long uniqueSignals;

    /** 契约：dupThreshold ≥ 2（fail-fast——1 则首个信号即触发失去语义）。 */
    public FastRetransmitTrigger(int dupThreshold) {
        if (dupThreshold < 2) {
            throw new IllegalArgumentException("dupThreshold 须 ≥ 2：" + dupThreshold);
        }
        this.dupThreshold = dupThreshold;
    }

    public FastRetransmitTrigger() {
        this(DEFAULT_DUP_THRESHOLD);
    }

    /**
     * 记一个信号：与上一信号相同则连续重复计数 +1，达到阈值即**触发**
     *（返回 true，计数清零——新一轮）；不同信号切换重计（uniqueSignals
     * +1，consecutive 归 1）。首信号计 1 不触发。
     */
    public synchronized boolean signal(String signalId) {
        if (signalId == null || signalId.isBlank()) {
            throw new IllegalArgumentException("signalId 不能为空");
        }
        if (signalId.equals(lastSignalId)) {
            dupSignals++;
            consecutiveDups++;
        } else {
            uniqueSignals++;
            lastSignalId = signalId;
            consecutiveDups = 1;
        }
        if (consecutiveDups >= dupThreshold) {
            triggers++;
            consecutiveDups = 0; // 触发即清零——新一轮重新数
            return true;
        }
        return false;
    }

    /** 触发次数（动作已发生面）。 */
    public synchronized long triggerCount() {
        return triggers;
    }

    /** 账面快照：触发数/重复信号数/切换信号数/当前连续重复数。 */
    public synchronized FastRetransmitStats stats() {
        return new FastRetransmitStats(triggers, dupSignals, uniqueSignals, consecutiveDups);
    }

    /** 触发账快照。 */
    public record FastRetransmitStats(long triggers, long dupSignals,
                                      long uniqueSignals, int consecutiveDups) {
    }
}
