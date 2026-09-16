package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * 突发信用账户（spec 1872 / T2945 / impl 1473）——AWS CPU credit / T3
 * unlimited 语义：负载按**基准速率**免费蓄水（idle 期攒 credit），突发
 * 消费可透支存量（burst），存量见底后**降速回基准**（不拒、不崩——
 * 超基准部分开始付费/排队）。与限流（GCRA 拒绝）不同：信用账户的枯竭
 * 不是失败而是降速——「突发了多久、还能突发多久、枯竭了几次」是容量
 * 规划读数。
 *
 * <p>线程安全（synchronized 小临界区）；水位钳 [0, capacity]。
 */
public final class BurstCreditAccount {

    private final long capacityCredits;
    private final long baseRefillPerMillis;
    private long credits;
    private long lastRefillMillis;
    private long exhaustions;

    /**
     * 契约：capacity ≥ 1、baseRefill ≥ 1、initial ≥ 0（fail-fast）；
     * 初始水位钳到容量。
     */
    public BurstCreditAccount(long capacityCredits, long baseRefillPerMillis,
                              long initialCredits, long nowMillis) {
        if (capacityCredits < 1 || baseRefillPerMillis < 1 || initialCredits < 0
                || nowMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "非法信用账户参：capacity=%d, refill=%d, initial=%d, now=%d",
                    capacityCredits, baseRefillPerMillis, initialCredits, nowMillis));
        }
        this.capacityCredits = capacityCredits;
        this.baseRefillPerMillis = baseRefillPerMillis;
        this.credits = Math.min(initialCredits, capacityCredits);
        this.lastRefillMillis = nowMillis;
    }

    /** 按基准速率补水至当前时点（封顶容量）。 */
    public synchronized void refill(long nowMillis) {
        if (nowMillis < lastRefillMillis) {
            throw new IllegalArgumentException(
                    "时钟回拨：now=" + nowMillis + " < last=" + lastRefillMillis);
        }
        long elapsed = nowMillis - lastRefillMillis;
        credits = Math.min(capacityCredits, credits + elapsed * baseRefillPerMillis);
        lastRefillMillis = nowMillis;
    }

    /**
     * 消费：水位足即扣返 true；不足拒（调用方降速到基准——不拒任务）。
     */
    public synchronized boolean trySpend(long cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("cost 不能为负：" + cost);
        }
        if (credits < cost) {
            return false;
        }
        credits -= cost;
        if (credits == 0) {
            exhaustions++;
        }
        return true;
    }

    /** 只读快照：水位/容量/基准速率/枯竭次数/满水率。 */
    public synchronized Snapshot stats() {
        return new Snapshot(credits, capacityCredits, baseRefillPerMillis,
                exhaustions, (double) credits / capacityCredits);
    }

    /**
     * @param credits      当前水位
     * @param exhaustions  枯竭次数（水位见底计数——突发预算超支频率）
     * @param fillRatio    满水率
     */
    public record Snapshot(long credits, long capacityCredits,
                           long baseRefillPerMillis, long exhaustions,
                           double fillRatio) {

        /** 剩余突发余量（满水时长换算）：水位 ÷ 基准速率——满水还能全速
         * 突发多久（毫秒；基准速率 0 不会发生——构造已拒）。 */
        public long burstHeadroomMillis() {
            return credits / baseRefillPerMillis;
        }
    }
}
