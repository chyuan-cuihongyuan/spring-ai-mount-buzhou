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

    public TurnStallWatchdog(TurnHeartbeat heartbeat, Duration quietThreshold,
                             Duration interval, boolean enabled) {
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

    /** 单轮巡检（手动/调度共用；返回本轮停滞清单）。 */
    public List<TurnHeartbeat.Stalled> inspectOnce() {
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
