package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 保留清扫新鲜度追踪器（spec 1427 / T2153 替位编号说明：R28 = effort #1427 /
 * 票 T2155 + T2156 / impl 1080——见台账）——Airflow scheduler heartbeat
 * （调度器心跳消失即数据过期）思想：{@link RetentionSweeper} 周期清扫失败/
 * 停摆时**没人发现**——报告只推给 listener 不落水位，「上一次清扫是多久
 * 以前、清扫间隔最长拉到多大、失败了几次」无读面。调度类作业的新鲜度
 * 是数据保留承诺的前提。
 *
 * <p>opt-in：实现 {@code Consumer<RetentionSweepReport>}，经既有
 * {@code addSweepListener} 注册即生效（零 sweeper 改动）；{@link #freshness(Instant)}
 * 以调用方时钟出快照（确定性测试）；{@link #resetForTest()} 归零注入点。
 */
public final class RetentionSweepFreshness implements Consumer<RetentionSweepReport> {

    private final AtomicLong sweepCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private volatile Instant lastSweepAt;
    private volatile long maxGapMillis;

    @Override
    public void accept(RetentionSweepReport report) {
        Instant at = report.sweptAt();
        synchronized (this) {
            if (lastSweepAt != null) {
                long gap = Math.max(0, Duration.between(lastSweepAt, at).toMillis());
                maxGapMillis = Math.max(maxGapMillis, gap);
            }
            lastSweepAt = at;
        }
        sweepCount.incrementAndGet();
        if (!report.fullySucceeded() || !report.failures().isEmpty()) {
            failureCount.incrementAndGet();
        }
    }

    /**
     * 新鲜度快照：now − lastSweepAt 即当前 stale 程度（调用方时钟注入）。
     *
     * @param sweepCount       累计清扫次数
     * @param lastSweepAt      末次清扫时刻（epoch millis；从未清扫 = -1）
     * @param staleMillis      距末次清扫的经过时长（毫秒；从未清扫 = -1）
     * @param maxGapMillis     相邻清扫间隔最大值（毫秒——调度抖动/停摆水位）
     * @param failureCount     未完全成功的清扫次数
     */
    public Snapshot freshness(Instant now) {
        long last = lastSweepAt == null ? -1 : lastSweepAt.toEpochMilli();
        long stale = lastSweepAt == null ? -1
                : Math.max(0, now.toEpochMilli() - last);
        return new Snapshot(sweepCount.get(), last, stale, maxGapMillis,
                failureCount.get());
    }

    /** 只读快照。 */
    public record Snapshot(long sweepCount, long lastSweepAt, long staleMillis,
                           long maxGapMillis, long failureCount) {
    }

    /** 测试归零口。 */
    public void resetForTest() {
        sweepCount.set(0);
        failureCount.set(0);
        lastSweepAt = null;
        maxGapMillis = 0;
    }
}
