package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionCapacityExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * spawn 闸（spec「背压与多层限流 · 维度① spawn 并发会话上限」+ spec 123 优先级排队）。
 *
 * <p>复用 {@code DefaultAgentRuntime.liveSessions} 台账计数源，在租约获取<b>之前</b>裁决：
 * <ul>
 *   <li>{@link OverloadPolicy#FAIL_FAST FAIL_FAST} —— 立即裁决，无空位即抛
 *       {@link SessionCapacityExceededException}（不排队，优先级无意义）；</li>
 *   <li>{@link OverloadPolicy#QUEUE QUEUE}（默认）—— 有界等待空位，超时抛
 *       {@link SessionCapacityExceededException}；drain 置位时唤醒等待者并抛
 *       {@link io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeDrainingException RuntimeDrainingException}。</li>
 * </ul>
 *
 * <p><b>优先级排队（spec 123 / T445）</b>：{@link SpawnPriority} 三级（HIGH/NORMAL/LOW，
 * 默认 NORMAL——既有单参调用零行为变化）。释放的空位<b>有向交接</b>给最高非空级的
 * 队首票据：高级抢占低级排队者、同级严格 FIFO、新到同级者不插队（票据队列杜绝
 * 唤醒抢跑——原公平信号量 {@code tryAcquire} 竞争窗的加塞可能就此修正）。
 * 借鉴：OS 调度多级队列 + Envoy 优先级面。
 *
 * <p>排队不持有租约——拿到空位后才走既有 {@code doSpawn} 全流程（租约 → 装配 → 注册）。
 * 空位由会话 close 释放时通知（经 {@link #releaseSlot()}）；drain 置位时经
 * {@link #signalDrainStarted()} 唤醒全部等待者。
 *
 * <p>事件：{@code backpressure.spawn-queued}（当前活跃/上限）/ {@code backpressure.spawn-rejected}
 * （原因：timeout / fail-fast / drain / interrupted）经运行时级事件通道发出（会话建立前，经
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
    private final Consumer<SessionEvent> emitter;
    /** spec 335：准入地板（null = 恒 LOW 全放行——存量构造器零变化）。 */
    private final java.util.function.Supplier<SpawnPriority> admissionFloor;

    /** spec 123：锁 + 每级票据队列（替代公平信号量——有向交接可排序）。 */
    private final ReentrantLock gateLock = new ReentrantLock();
    private final Condition[] priorityConditions;
    private final Deque<PriorityTicket>[] queues;
    /** 空闲空位数。不变量：{@code available > 0} ⇒ 所有队列为空（释放仅在无排队者时回增）。 */
    private int available;
    /** drain 状态镜像（volatile 读，避免每次排队都加锁探测）。 */
    private volatile boolean draining = false;

    /**
     * @param limit      活跃会话上限（必须 > 0）
     * @param queueTimeout QUEUE 档排队超时
     * @param policy     过载策略
     * @param emitter    运行时级事件发射器（{@code DefaultAgentRuntime::runtimeEmit}）
     */
    @SuppressWarnings("unchecked")
    public SpawnGate(int limit, Duration queueTimeout, OverloadPolicy policy,
                     Consumer<SessionEvent> emitter) {
        this(limit, queueTimeout, policy, emitter, null);
    }

    /**
     * spec 335 / T661：带准入地板的构造（地板先于容量/排队判定——低于地板的
     * 优先级立即拒；null = 恒 LOW 全放行，与四参构造完全一致）。
     *
     * @param admissionFloor 准入地板供给（如 {@link SpawnAdmissionFloor}）
     */
    @SuppressWarnings("unchecked")
    public SpawnGate(int limit, Duration queueTimeout, OverloadPolicy policy,
                     Consumer<SessionEvent> emitter,
                     java.util.function.Supplier<SpawnPriority> admissionFloor) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }
        this.limit = limit;
        this.queueTimeout = queueTimeout == null ? Duration.ofSeconds(30) : queueTimeout;
        this.policy = policy == null ? OverloadPolicy.QUEUE : policy;
        this.emitter = emitter == null ? event -> {} : emitter;
        this.admissionFloor = admissionFloor;
        this.available = limit;
        int levels = SpawnPriority.values().length;
        this.priorityConditions = new Condition[levels];
        this.queues = new Deque[levels];
        for (SpawnPriority priority : SpawnPriority.values()) {
            priorityConditions[priority.ordinal()] = gateLock.newCondition();
            queues[priority.ordinal()] = new ArrayDeque<>();
        }
    }

    /** 当前活跃会话数（上限 - 空闲；交接在飞的过渡态不敏感）。 */
    public int currentCount() {
        gateLock.lock();
        try {
            return limit - available;
        } finally {
            gateLock.unlock();
        }
    }

    /** 上限。 */
    public int limit() {
        return limit;
    }

    /**
     * 获取容量空位（slot），超限 / 超时 / drain 唤醒时抛对应异常。
     * 既有单参入口 = {@link SpawnPriority#NORMAL}（零行为变化）。
     *
     * @param sessionId 被裁决的会话 id（异常 message / 事件 payload 用）
     * @throws SessionCapacityExceededException 容量超限（FAIL_FAST 立即 / QUEUE 超时）
     * @throws BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED) 排队期间停机置位（与 main 既有 spawn 拒新语义同型）
     */
    public void acquireSlotOrThrow(String sessionId) {
        acquireSlotOrThrow(sessionId, SpawnPriority.NORMAL);
    }

    /**
     * spec 123：按优先级获取容量空位——QUEUE 档下高优先级排队者先得空位、
     * 同级 FIFO；FAIL_FAST 档不排队（优先级无意义）。
     *
     * @param sessionId 被裁决的会话 id
     * @param priority  排队优先级（null = NORMAL）
     */
    public void acquireSlotOrThrow(String sessionId, SpawnPriority priority) {
        SpawnPriority prio = priority == null ? SpawnPriority.NORMAL : priority;
        // spec 335 / T661：准入地板最先裁决（先于容量/排队/drain——语义顺序即
        // 保护优先级；SRE 冻结期只放行 >= 地板的优先级）
        SpawnPriority floor = admissionFloor == null ? SpawnPriority.LOW : admissionFloor.get();
        if (floor == null) {
            floor = SpawnPriority.LOW;
        }
        if (prio.ordinal() > floor.ordinal()) { // 语义低于地板才拒（HIGH 序数最小语义最高）
            emitRejected(sessionId, "admission-floor", Duration.ZERO);
            throw new SessionCapacityExceededException(sessionId, currentCount(), limit,
                    Duration.ZERO);
        }
        if (policy == OverloadPolicy.FAIL_FAST) {
            gateLock.lock();
            try {
                if (available > 0) {
                    available--;
                    // 获取后再次检查 drain（drain 可能在此瞬间置位）
                    if (draining) {
                        releaseUnderLock();
                        throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                                "Runtime 正在停机，拒绝排队中的新会话（sessionId=" + sessionId + "）");
                    }
                    return;
                }
                emitRejected(sessionId, REASON_FAIL_FAST, Duration.ZERO);
                throw new SessionCapacityExceededException(sessionId, currentCount(), limit, Duration.ZERO);
            } finally {
                gateLock.unlock();
            }
        }

        // QUEUE 档：有界排队（优先级票据队列）
        Instant start = Instant.now();
        gateLock.lock();
        try {
            // 先快速尝试一次（无空位时才进排队，避免无竞争场景发 queued 事件）
            if (available > 0) {
                available--;
                if (draining) {
                    releaseUnderLock();
                    throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                            "Runtime 正在停机，拒绝排队中的新会话（sessionId=" + sessionId + "）");
                }
                return;
            }
            // 进入排队——发 queued 事件 + 落票据
            emitQueued(sessionId);
            PriorityTicket ticket = new PriorityTicket();
            Deque<PriorityTicket> queue = queues[prio.ordinal()];
            queue.addLast(ticket);
            while (true) {
                if (draining) {
                    queue.remove(ticket);
                    emitRejected(sessionId, "drain", Duration.between(start, Instant.now()));
                    throw new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED,
                        "Runtime 正在停机，拒绝排队中的新会话（sessionId=" + sessionId + "）");
                }
                if (ticket.granted) {
                    // 空位已由 releaseSlot 有向交接（available 不经手）
                    return;
                }
                Duration remaining = queueTimeout.minus(Duration.between(start, Instant.now()));
                if (remaining.isZero() || remaining.isNegative()) {
                    queue.remove(ticket);
                    emitRejected(sessionId, REASON_TIMEOUT, Duration.between(start, Instant.now()));
                    throw new SessionCapacityExceededException(sessionId, currentCount(), limit,
                            Duration.between(start, Instant.now()));
                }
                try {
                    priorityConditions[prio.ordinal()].await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    queue.remove(ticket);
                    Thread.currentThread().interrupt();
                    emitRejected(sessionId, "interrupted", Duration.between(start, Instant.now()));
                    throw new SessionCapacityExceededException(sessionId, currentCount(), limit,
                            Duration.between(start, Instant.now()));
                }
                // 唤醒后重检：票据被授予即放行，否则继续等待（正确性不依赖抢跑顺序）
            }
        } finally {
            gateLock.unlock();
        }
    }

    /**
     * 释放容量空位（会话 close 时调用）——有向交接：最高非空级队首票据置 granted
     * 并唤醒该级；无排队者时回增空闲计数。
     */
    public void releaseSlot() {
        gateLock.lock();
        try {
            releaseUnderLock();
        } finally {
            gateLock.unlock();
        }
    }

    /** 锁内释放：先交接给最高非空级队首，无排队者才回增 available。 */
    private void releaseUnderLock() {
        for (SpawnPriority priority : SpawnPriority.values()) {
            PriorityTicket head = queues[priority.ordinal()].pollFirst();
            if (head != null) {
                head.granted = true;
                priorityConditions[priority.ordinal()].signalAll();
                return;
            }
        }
        available++;
    }

    /**
     * drain 开始时唤醒全部排队等待者。
     *
     * <p>在 {@code DefaultAgentRuntime.drain()} 内、{@code drainFuture.set(future)} 之后调用。
     * 被唤醒的等待者重新检查 {@code draining} 标志后抛 SHUTDOWN_INTERRUPTED 结构化异常。
     */
    public void signalDrainStarted() {
        draining = true;
        gateLock.lock();
        try {
            for (Condition condition : priorityConditions) {
                condition.signalAll();
            }
        } finally {
            gateLock.unlock();
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

    /** spec 123：排队票据（granted = 空位已被有向交接）。 */
    private static final class PriorityTicket {
        boolean granted;
    }
}
