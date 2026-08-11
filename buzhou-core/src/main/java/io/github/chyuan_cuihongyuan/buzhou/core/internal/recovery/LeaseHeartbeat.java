package io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 租约心跳：轮次执行期按固定间隔续约会话租约（spec「崩溃中轮次恢复 / 基础项」）。
 *
 * <p>修补 {@code DefaultAgentRuntime} 取得租约后从不续约的缺陷——长轮次（&gt; 原 90s TTL）
 * 会被误判崩溃、被另一实例 steal，破坏「同会话单活跃实例」不变量。心跳让「租约过期=崩溃」
 * 的检测信号在长轮次上仍然成立。
 *
 * <p>续约失败（租约已易主 / fencing token 不匹配）时经 {@code onLeaseLost} 回调通知宿主
 * （宿主将会话置为失效，后续 chat 抛 {@code LeaseLostException}）。复用虚拟线程调度，
 * 不占平台线程；间隔 &lt; TTL（默认 30s vs 90s）。
 */
public final class LeaseHeartbeat implements AutoCloseable {

    private final SessionLeaseStore leaseStore;
    private final String sessionId;
    private final String ownerId;
    private final long fencingToken;
    private final Duration ttl;
    private final Duration interval;
    private final Consumer<Void> onLeaseLost;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean lost = new AtomicBoolean();
    private volatile ScheduledFuture<?> task;

    public LeaseHeartbeat(SessionLeaseStore leaseStore,
                          String sessionId, String ownerId, long fencingToken,
                          Duration ttl, Duration interval,
                          Consumer<Void> onLeaseLost) {
        this.leaseStore = leaseStore;
        this.sessionId = sessionId;
        this.ownerId = ownerId;
        this.fencingToken = fencingToken;
        this.ttl = ttl;
        this.interval = interval;
        this.onLeaseLost = onLeaseLost;
        // 虚拟线程调度器：心跳是轻量周期 IO，不占平台线程（CLAUDE.md 并发规约）
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    }

    /** 启动周期续约（首次延迟一个 interval）。幂等。 */
    public void start() {
        if (task != null) {
            return;
        }
        long periodMillis = interval.toMillis();
        task = scheduler.scheduleAtFixedRate(this::renewOnce, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    private void renewOnce() {
        if (lost.get() || Thread.currentThread().isInterrupted()) {
            return;
        }
        boolean renewed = leaseStore.renew(sessionId, ownerId, fencingToken, ttl);
        if (!renewed && lost.compareAndSet(false, true)) {
            // 租约已易主：通知宿主置会话失效（回调只触发一次）
            if (onLeaseLost != null) {
                onLeaseLost.accept(null);
            }
        }
    }

    /** 租约是否已丢失（续约失败 / 被 steal）。 */
    public boolean isLost() {
        return lost.get();
    }

    @Override
    public void close() {
        if (task != null) {
            task.cancel(false);
        }
        scheduler.shutdownNow();
    }
}
