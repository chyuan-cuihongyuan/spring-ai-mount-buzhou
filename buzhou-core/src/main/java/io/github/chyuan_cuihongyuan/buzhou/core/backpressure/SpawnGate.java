package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionCapacityExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * spawn 闸（spec「背压与多层限流 · 维度① spawn 并发会话上限」）。
 *
 * <p>复用 {@code DefaultAgentRuntime.liveSessions} 台账计数源，在租约获取<b>之前</b>裁决：
 * <ul>
 *   <li>{@link OverloadPolicy#FAIL_FAST FAIL_FAST} —— {@code tryAcquire()} 立即裁决，失败即抛
 *       {@link SessionCapacityExceededException}；</li>
 *   <li>{@link OverloadPolicy#QUEUE QUEUE}（默认）—— {@code tryAcquire(timeout)} 有界等待空位，
 *       超时抛 {@link SessionCapacityExceededException}；drain 置位时唤醒等待者并抛
 *       {@link io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeDrainingException RuntimeDrainingException}。</li>
 * </ul>
 *
 * <p>排队不持有租约——拿到空位后才走既有 {@code doSpawn} 全流程（租约 → 装配 → 注册）。
 * 空位由会话 close 释放时通知（经 {@link #releaseSlot()}）；drain 置位时经 {@link #signalDrainStarted()}
 * 唤醒全部等待者。禁止轮询 sleep——用信号量 + 条件变量。
 *
 * <p>事件：{@code backpressure.spawn-queued}（当前活跃/上限）/ {@code backpressure.spawn-rejected}
 * （原因：timeout / fail-fast）经运行时级事件通道发出（会话建立前，经
 * {@code DefaultAgentRuntime.runtimeEmit}）。
 *
 * <p>{@code spawn(steal=true)} 是已活跃会话的接管路径（易主续接），<b>不占新容量</b>——
 * 调用方在进入闸之前判定 steal=true 直接绕过（本类不参与 steal 判定）。
 */
public final class SpawnGate {

    /** 事件类型：spawn 进入排队（QUEUE 档，当前活跃 / 上限）。 */
    public static final String EVENT_SPAWN_QUEUED = "backpressure.spawn-queued";
    /** 事件类型：spawn 被拒绝（timeout / fail-fast / drain 唤醒）。 */
    public static final String EVENT_SPAWN_REJECTED = "backpressure.spawn-rejected";
    /** 拒绝原因：排队超时。 */
    public static final String REASON_TIMEOUT = "timeout";
    /** 拒绝原因：快速失败档。 */
    public static final String REASON_FAIL_FAST = "fail-fast";

    private final int limit;
    private final Duration queueTimeout;
    private final OverloadPolicy policy;
    private final Semaphore capacitySemaphore;
    private final Consumer<SessionEvent> emitter;

    /** drain 唤醒锁 + 条件：drain 置位时唤醒全部排队等待者（不睡死在信号量上）。 */
    private final ReentrantLock drainWakeLock = new ReentrantLock();
    private final java.util.concurrent.locks.Condition drainStarted = drainWakeLock.newCondition();
    /** drain 状态镜像（volatile 读，避免每次排队都读 AtomicReference）。 */
    private volatile boolean draining = false;

    /**
     * @param limit      活跃会话上限（必须 > 0）
     * @param queueTimeout QUEUE 档排队超时
     * @param policy     过载策略
     * @param emitter    运行时级事件发射器（{@code DefaultAgentRuntime::runtimeEmit}）
     */
    public SpawnGate(int limit, Duration queueTimeout, OverloadPolicy policy,
                     Consumer<SessionEvent> emitter) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }
        this.limit = limit;
        this.queueTimeout = queueTimeout == null ? Duration.ofSeconds(30) : queueTimeout;
        this.policy = policy == null ? OverloadPolicy.QUEUE : policy;
        this.capacitySemaphore = new Semaphore(limit, true);
        this.emitter = emitter == null ? event -> {} : emitter;
    }

    /** 当前活跃会话数（上限 - 可用许可）。 */
    public int currentCount() {
        return limit - capacitySemaphore.availablePermits();
    }

    /** 上限。 */
    public int limit() {
        return limit;
    }

    /**
     * 获取容量空位（slot），超限 / 超时 / drain 唤醒时抛对应异常。
     *
     * <p>调用方在进入此方法<b>之前</b>已判定 drain 未开始（{@code drainFuture.get() == null}）与
     * steal=false（接管路径绕过本闸）。本方法内部会再次检查 drain 状态（排队期间 drain 可能置位）。
     *
     * @param sessionId 被裁决的会话 id（异常 message / 事件 payload 用）
     * @throws SessionCapacityExceededException 容量超限（FAIL_FAST 立即 / QUEUE 超时）
     * @throws BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED) 排队期间停机置位（与 main 既有 spawn 拒新语义同型）
     */
    public void acquireSlotOrThrow(String sessionId) {
        if (policy == OverloadPolicy.FAIL_FAST) {
            if (!capacitySemaphore.tryAcquire()) {
                emitRejected(sessionId, REASON_FAIL_FAST, Duration.ZERO);
                throw new SessionCapacityExceededException(sessionId, currentCount(), limit, Duration.ZERO);
            }
            // 获取后再次检查 drain（drain 可能在此瞬间置位）
            if (draining) {
                capacitySemaphore.release();
                throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                        "Runtime 正在停机，拒绝排队中的新会话（sessionId=" + sessionId + "）");
            }
            return;
        }

        // QUEUE 档：有界排队
        Instant start = Instant.now();
        // 先快速尝试一次（无空位时才进排队，避免无竞争场景发 queued 事件）
        if (capacitySemaphore.tryAcquire()) {
            if (draining) {
                capacitySemaphore.release();
                throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                        "Runtime 正在停机，拒绝排队中的新会话（sessionId=" + sessionId + "）");
            }
            return;
        }

        // 进入排队——发 queued 事件
        emitQueued(sessionId);
        drainWakeLock.lock();
        try {
            while (true) {
                if (draining) {
                    emitRejected(sessionId, "drain", Duration.between(start, Instant.now()));
                    throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                        "Runtime 正在停机，拒绝排队中的新会话（sessionId=" + sessionId + "）");
                }
                if (capacitySemaphore.tryAcquire()) {
                    // 拿到空位——再次检查 drain（drain 可能在此瞬间置位）
                    if (draining) {
                        capacitySemaphore.release();
                        emitRejected(sessionId, "drain", Duration.between(start, Instant.now()));
                        throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                        "Runtime 正在停机，拒绝排队中的新会话（sessionId=" + sessionId + "）");
                    }
                    return;
                }
                Duration remaining = queueTimeout.minus(Duration.between(start, Instant.now()));
                if (remaining.isZero() || remaining.isNegative()) {
                    emitRejected(sessionId, REASON_TIMEOUT, Duration.between(start, Instant.now()));
                    throw new SessionCapacityExceededException(sessionId, currentCount(), limit,
                            Duration.between(start, Instant.now()));
                }
                try {
                    drainStarted.await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    emitRejected(sessionId, "interrupted", Duration.between(start, Instant.now()));
                    throw new SessionCapacityExceededException(sessionId, currentCount(), limit,
                            Duration.between(start, Instant.now()));
                }
            }
        } finally {
            drainWakeLock.unlock();
        }
    }

    /**
     * 释放容量空位（会话 close 时调用）。
     *
     * <p>信号量 release 后唤醒一个排队等待者（{@code signalAll} 保证 drain 唤醒不漏——
     * 排队等待者被唤醒后重新尝试 {@code tryAcquire}，成功即放行、失败继续等待）。
     */
    public void releaseSlot() {
        capacitySemaphore.release();
        drainWakeLock.lock();
        try {
            drainStarted.signalAll();
        } finally {
            drainWakeLock.unlock();
        }
    }

    /**
     * drain 开始时唤醒全部排队等待者。
     *
     * <p>在 {@code DefaultAgentRuntime.drain()} 内、{@code drainFuture.set(future)} 之后调用。
     * 被唤醒的等待者重新检查 {@code draining} 标志后抛 SHUTDOWN_INTERRUPTED 结构化异常。
     */
    public void signalDrainStarted() {
        draining = true;
        drainWakeLock.lock();
        try {
            drainStarted.signalAll();
        } finally {
            drainWakeLock.unlock();
        }
    }

    private void emitQueued(String sessionId) {
        emitter.accept(new SessionEvent(EVENT_SPAWN_QUEUED,
                Map.of("sessionId", sessionId, "currentActive", currentCount(), "limit", limit),
                Instant.now()));
    }

    private void emitRejected(String sessionId, String reason, Duration waited) {
        // impl-45：拒绝指标（reason tag 有界）+ INFO 日志（容量治理运维可见）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.backpressure.spawn-rejected", "reason", reason);
        emitter.accept(new SessionEvent(EVENT_SPAWN_REJECTED,
                Map.of("sessionId", sessionId, "reason", reason,
                        "currentActive", currentCount(), "limit", limit,
                        "waitedMs", waited.toMillis()),
                Instant.now()));
    }
}
