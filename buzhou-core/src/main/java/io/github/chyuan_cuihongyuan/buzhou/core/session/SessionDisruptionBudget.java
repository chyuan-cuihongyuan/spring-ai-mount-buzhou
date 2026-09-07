package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 会话扰乱预算（spec 318 / T627，Kubernetes PodDisruptionBudget 借鉴）：
 * voluntary 排水（维护/缩容）前领额度——ACTIVE 数 − minAvailable 为预算，
 * 预留制（available − reserved &gt; minAvailable 才放行）防并发超发。
 * 非主动扰乱（会话异常终结）不占额度。
 */
public final class SessionDisruptionBudget {

    private final LongSupplier activeCount;
    private final long minAvailable;
    private final AtomicLong reserved = new AtomicLong();
    private final AtomicLong allowed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    /**
     * @param activeCount  ACTIVE 会话计数源（会话索引）
     * @param minAvailable 保底可用数（≥0；0 = 不限——但本类仍预留计数，装配层 0 不装配）
     */
    public SessionDisruptionBudget(LongSupplier activeCount, long minAvailable) {
        if (activeCount == null) {
            throw new IllegalArgumentException("activeCount 必须非空");
        }
        if (minAvailable < 0) {
            throw new IllegalArgumentException("minAvailable >= 0（当前 " + minAvailable + "）");
        }
        this.activeCount = activeCount;
        this.minAvailable = minAvailable;
    }

    /**
     * 领一格扰乱额度（排水前调用）。
     *
     * @return true = 领到（排水可进行，完毕务必 {@link #completeDisruption()}）
     */
    public synchronized boolean tryAcquireDisruption() {
        long available = activeCount.getAsLong();
        if (available - reserved.get() > minAvailable) {
            reserved.incrementAndGet();
            allowed.incrementAndGet();
            return true;
        }
        rejected.incrementAndGet();
        return false;
    }

    /** 排水完成归还一格（未领多还不欠——floor 0）。 */
    public synchronized void completeDisruption() {
        reserved.updateAndGet(current -> Math.max(0, current - 1));
    }

    /** 当前可扰乱额度（available − reserved − minAvailable，floor 0——观测面）。 */
    public synchronized long disruptionsAvailable() {
        return Math.max(0, activeCount.getAsLong() - reserved.get() - minAvailable);
    }

    /** 领到计数（观测面）。 */
    public long allowedCount() {
        return allowed.get();
    }

    /** 拒绝计数（观测面——预算吃紧证据）。 */
    public long rejectedCount() {
        return rejected.get();
    }

    /** 保底配置（观测面）。 */
    public long minAvailable() {
        return minAvailable;
    }
}
