package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 轮次停滞巡检犬（spec 162 §A / T515，spec 152 fog「定期 stalled 查询 + 告警」
 * 收口，看门狗模式借鉴 Kubernetes liveness probe 的周期自检）：低频单线程轮询
 * {@link TurnHeartbeat#registered()} 全集——每轮把 quiet 超阈值者交给 listener
 * （告警/事件由宿主定夺，本犬只发现不处置）。
 *
 * <p><b>重复告警语义（诚实）</b>：每轮都报（不是只报一次）——停滞是持续状态，
 * 「还在停」每轮都是事实；去重/静默窗口是告警接收端（Alertmanager grouping）
 * 的职责，本层不做隐藏状态。单轮异常只记 ERROR 不杀调度线程（与
 * ArchivePurgeJob 同骨架纪律）。
 */
public final class TurnStallWatchdog implements SmartLifecycle, AutoCloseable {

    private static final System.Logger LOGGER =
            System.getLogger(TurnStallWatchdog.class.getName());

    private final TurnHeartbeat heartbeat;
    private final Duration quietThreshold;
    private final Duration interval;
    private final boolean enabled;
    private final List<java.util.function.Consumer<List<TurnHeartbeat.Stalled>>> listeners =
            new CopyOnWriteArrayList<>();

    private volatile ScheduledExecutorService scheduler;
    private volatile boolean running;
    /** spec 186 / T545：可选咨询锁 + 因锁跳过计数。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.retention.AdvisoryFileLock lock;
    private final String lockOwner;
    private final java.util.concurrent.atomic.AtomicLong skippedForLock =
            new java.util.concurrent.atomic.AtomicLong();

    public TurnStallWatchdog(TurnHeartbeat heartbeat, Duration quietThreshold,
                             Duration interval, boolean enabled) {
        this(heartbeat, quietThreshold, interval, enabled, null);
    }

    /**
     * spec 186 §A / T545：带咨询锁构造（多实例单跑档——lock 非空时每轮先抢锁；
     * 未获锁<b>零通知</b>（空表通知语义保留给「真巡检过没事」——跳过不是空）
     * 只计 {@link #skippedForLock()} + WARN）。lock null = 既有零变化。
     */
    public TurnStallWatchdog(TurnHeartbeat heartbeat, Duration quietThreshold,
                             Duration interval, boolean enabled,
                             io.github.chyuan_cuihongyuan.buzhou.core.retention.AdvisoryFileLock lock) {
        if (quietThreshold == null || quietThreshold.isNegative()) {
            throw new IllegalArgumentException("quietThreshold must be non-negative");
        }
        if (interval == null || (!enabled && interval.isNegative())) {
            throw new IllegalArgumentException("interval must be non-null");
        }
        if (enabled && (interval.isZero() || interval.isNegative())) {
            throw new IllegalArgumentException("interval must be positive when enabled: " + interval);
        }
        this.heartbeat = heartbeat == null ? new TurnHeartbeat() : heartbeat;
        this.quietThreshold = quietThreshold;
        this.interval = interval;
        this.enabled = enabled;
        this.lock = lock;
        this.lockOwner = "stall-watchdog@" + java.lang.management.ManagementFactory
                .getRuntimeMXBean().getName().replace('@', '-');
    }

    /** 停滞轮听众（每轮一调用：空表 = 本轮无停滞——「跑过没事」也是事实）。 */
    public void addListener(java.util.function.Consumer<List<TurnHeartbeat.Stalled>> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** 共享的心跳表视图（宿主注册/打点用）。 */
    public TurnHeartbeat heartbeat() {
        return heartbeat;
    }

    /** 单轮巡检（手动/调度共用；返回本轮停滞清单；未获锁返回 null——与空表
     * 「巡检过没事」区分）。 */
    public List<TurnHeartbeat.Stalled> inspectOnce() {
        if (lock != null) {
            try {
                if (!lock.tryAcquire(lockOwner, Instant.now())) {
                    skippedForLock.incrementAndGet();
                    LOGGER.log(System.Logger.Level.WARNING,
                            "停滞巡检未获锁——别的实例在跑，本轮跳过（零通知）");
                    return null;
                }
            } catch (java.io.IOException e) {
                skippedForLock.incrementAndGet();
                LOGGER.log(System.Logger.Level.WARNING, "停滞巡检抢锁失败（IO）——跳过本轮", e);
                return null;
            }
        }
        try {
            List<TurnHeartbeat.Stalled> stalled =
                    heartbeat.stalled(heartbeat.registered(), quietThreshold, Instant.now());
            listeners.forEach(listener -> listener.accept(stalled));
            if (!stalled.isEmpty()) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "轮次停滞巡检：{0} 个在飞轮次 quiet 超过 {1}s（最长 {2}s）",
                        stalled.size(), quietThreshold.toSeconds(),
                        stalled.isEmpty() ? 0 : stalled.get(0).quietFor().toSeconds());
            }
            return stalled;
        } finally {
            if (lock != null) {
                try {
                    lock.release(lockOwner);
                } catch (java.io.IOException e) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "停滞巡检释放锁失败（下轮按陈旧回收）", e);
                }
            }
        }
    }

    /** 因未获锁跳过的轮数（多实例部署的「本实例在歇」证据面）。 */
    public long skippedForLock() {
        return skippedForLock.get();
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(
                BuzhouThreadFactory.platform("turn-stall-watchdog"));
        scheduler.scheduleWithFixedDelay(this::inspectOnceSafe, interval.toMillis(),
                interval.toMillis(), TimeUnit.MILLISECONDS);
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        ScheduledExecutorService current = scheduler;
        if (current != null) {
            current.shutdownNow();
            scheduler = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void close() {
        stop();
    }

    private void inspectOnceSafe() {
        try {
            inspectOnce();
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR, "停滞巡检单轮失败（下一轮照常）", e);
        }
    }
}
