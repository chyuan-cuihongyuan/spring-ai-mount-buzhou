package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 就绪等待门（spec 2021 / T3143 / impl 1572）——gRPC wait_for_ready
 * 思想：依赖未就绪时请求两种姿态——wait_for_ready=true 排队等待（就绪
 * 后放行，队列有预算上限——防无限积压），false 立即 fail-fast（调用
 * 方换道或熔断）。就绪翻转批量排空队列（drain 计数）——四态结局与
 * 四计数显形（排队纪律对账面）。
 *
 * <p>synchronized 小临界区；非阻塞询问式（Outcome 即指令，动作由
 * 调用方执行）。
 */
public final class WaitForReadyGate {

    /** 尝试结局四态。 */
    public enum Outcome {
        /** 就绪——直接放行。 */
        PASS,
        /** 未就绪但 waitForReady——入队等待（预算内）。 */
        QUEUED,
        /** 未就绪且 fail-fast 姿态——立即失败。 */
        FAIL_FAST,
        /** 排队预算已满——拒绝入队（防无限积压）。 */
        QUEUE_FULL
    }

    /** 默认排队预算（未就绪期最多在队请求数）。 */
    public static final int DEFAULT_MAX_QUEUED = 1000;

    private final int maxQueued;
    private boolean ready;
    private int queueDepth;
    private final AtomicLong passes = new AtomicLong();
    private final AtomicLong queued = new AtomicLong();
    private final AtomicLong failFasts = new AtomicLong();
    private final AtomicLong queueFullRejections = new AtomicLong();
    private final AtomicLong drains = new AtomicLong();
    private final AtomicLong drainBatches = new AtomicLong();

    /** 契约：maxQueued ≥ 0（fail-fast）。 */
    public WaitForReadyGate(boolean initialReady, int maxQueued) {
        if (maxQueued < 0) {
            throw new IllegalArgumentException("maxQueued 须 ≥ 0：" + maxQueued);
        }
        this.ready = initialReady;
        this.maxQueued = maxQueued;
    }

    public WaitForReadyGate() {
        this(false, DEFAULT_MAX_QUEUED);
    }

    /**
     * 询问通过：就绪 → PASS；未就绪：waitForReady 且队列预算内 →
     * QUEUED（队列深度 +1）；waitForReady 但预算满 → QUEUE_FULL；
     * 非 waitForReady → FAIL_FAST。
     */
    public synchronized Outcome tryAcquire(boolean waitForReady) {
        if (ready) {
            passes.incrementAndGet();
            return Outcome.PASS;
        }
        if (!waitForReady) {
            failFasts.incrementAndGet();
            return Outcome.FAIL_FAST;
        }
        if (queueDepth >= maxQueued) {
            queueFullRejections.incrementAndGet();
            return Outcome.QUEUE_FULL;
        }
        queueDepth++;
        queued.incrementAndGet();
        return Outcome.QUEUED;
    }

    /**
     * 就绪翻转：转就绪时**批量排空**队列（在队请求全放行——drain 计数
     * + 批次计数）；转未就绪时清零深度（在队者已由排空放行，新队重计）。
     */
    public synchronized void setReady(boolean ready) {
        if (ready && !this.ready) {
            if (queueDepth > 0) {
                drains.addAndGet(queueDepth);
                drainBatches.incrementAndGet();
            }
            queueDepth = 0;
        }
        this.ready = ready;
    }

    /** 当前排队深度（未就绪期积压水位）。 */
    public synchronized int queueDepth() {
        return queueDepth;
    }

    /** 是否就绪。 */
    public synchronized boolean isReady() {
        return ready;
    }

    /** 四计数 + 排空面快照（排队纪律对账）。 */
    public GateStats stats() {
        return new GateStats(passes.get(), queued.get(), failFasts.get(),
                queueFullRejections.get(), drains.get(), drainBatches.get());
    }

    /** 就绪门账快照。 */
    public record GateStats(long passes, long queued, long failFasts,
                            long queueFullRejections, long drained, long drainBatches) {
    }
}
