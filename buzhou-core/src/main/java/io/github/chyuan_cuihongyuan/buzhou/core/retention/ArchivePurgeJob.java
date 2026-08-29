package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * 归档定时清理（spec 127 §A / T455，spec 103 fog「归档 autoconfig 定时」收口，
 * ShedLock 单实例语义 + S3 lifecycle 触发思想）：低频单线程按 TTL 调
 * {@link SessionArchiver#purgeExpired(Duration, Instant)}——S3 lifecycle 的
 * 「到期规则自动兑现」，但诚实边界：<b>单进程内调度</b>，多实例部署每实例都会跑
 * （purgeExpired 幂等，重复跑无害但非零成本——多实例节流是宿主/后续 fog 的事）。
 *
 * <p>与 {@link RetentionSweeper} 同骨架（SmartLifecycle + 自持
 * ScheduledExecutorService）：{@code enabled=false} 只关自启动调度——
 * {@link #purgeOnce()} 手动面恒可用。每轮删除数经 listener 可观测不静默。
 */
public class ArchivePurgeJob implements SmartLifecycle, AutoCloseable {

    private static final System.Logger LOGGER =
            System.getLogger(ArchivePurgeJob.class.getName());

    private final SessionArchiver archiver;
    private final Duration ttl;
    private final Duration interval;
    private final boolean enabled;
    private final List<IntConsumer> listeners = new CopyOnWriteArrayList<>();

    private volatile ScheduledExecutorService scheduler;
    private volatile boolean running;

    public ArchivePurgeJob(SessionArchiver archiver, Duration ttl, Duration interval,
                           boolean enabled) {
        if (ttl == null || interval == null) {
            throw new IllegalArgumentException("ttl and interval must not be null");
        }
        if (enabled && (interval.isZero() || interval.isNegative())) {
            throw new IllegalArgumentException("interval must be positive when enabled: " + interval);
        }
        this.archiver = archiver;
        this.ttl = ttl;
        this.interval = interval;
        this.enabled = enabled;
    }

    /** 单轮清理（手动/调度共用；返回删除数——损坏归档跳过语义沿用 purgeExpired）。 */
    public int purgeOnce() {
        int purged = archiver.purgeExpired(ttl, Instant.now());
        listeners.forEach(listener -> listener.accept(purged));
        if (purged > 0) {
            LOGGER.log(System.Logger.Level.INFO,
                    "归档 TTL 清理完成：删除 {0} 条（ttl={1}s）", purged, ttl.toSeconds());
        }
        return purged;
    }

    /** 每轮删除数监听（可观测不静默；0 也通知——「跑过但无事可做」是事实）。 */
    public void addPurgeListener(IntConsumer listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(
                BuzhouThreadFactory.platform("archive-purge"));
        scheduler.scheduleWithFixedDelay(this::purgeOnceSafe, interval.toMillis(),
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

    /** 调度面安全壳：单轮异常只记日志不杀调度线程（下一轮照常）。 */
    private void purgeOnceSafe() {
        try {
            purgeOnce();
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR, "归档清理单轮失败（下一轮照常）", e);
        }
    }
}
