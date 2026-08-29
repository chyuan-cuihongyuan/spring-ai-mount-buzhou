package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import org.springframework.context.SmartLifecycle;

import java.lang.management.ManagementFactory;
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

    /** 未获锁跳过轮的哨兵返回值（listener 通知 -1——「别的实例在跑」也是事实）。 */
    public static final int SKIPPED_LOCKED = -1;

    private final SessionArchiver archiver;
    private final Duration ttl;
    private final Duration interval;
    private final boolean enabled;
    /** spec 184 / T542：可选咨询锁（多实例单跑档）。 */
    private final AdvisoryFileLock lock;
    /** 抢锁持有者标识（进程内单持有——hostName 派生，不必配置）。 */
    private final String lockOwner = "archive-purge@"
            + ManagementFactory.getRuntimeMXBean().getName().replace('@', '-');
    private final List<IntConsumer> listeners = new CopyOnWriteArrayList<>();

    private volatile ScheduledExecutorService scheduler;
    private volatile boolean running;

    public ArchivePurgeJob(SessionArchiver archiver, Duration ttl, Duration interval,
                           boolean enabled) {
        this(archiver, ttl, interval, enabled, null);
    }

    /**
     * spec 184 §A / T542：带咨询锁构造（多实例单跑档——lock 非空时每轮先抢锁，
     * 未获锁跳过本轮并通知 {@code -1}：「别的实例在跑」也是事实）。lock null =
     * 既有每实例各跑语义零变化。
     */
    public ArchivePurgeJob(SessionArchiver archiver, Duration ttl, Duration interval,
                           boolean enabled, AdvisoryFileLock lock) {
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
        this.lock = lock;
    }

    /** 单轮清理（手动/调度共用；返回删除数——损坏归档跳过语义沿用 purgeExpired；
     * 带锁未获锁返回 -1 且 archiver 零调用）。 */
    public int purgeOnce() {
        if (lock != null) {
            try {
                if (!lock.tryAcquire(lockOwner, Instant.now())) {
                    listeners.forEach(listener -> listener.accept(SKIPPED_LOCKED));
                    return SKIPPED_LOCKED;
                }
            } catch (java.io.IOException e) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "归档清理抢锁失败（IO）——按未获锁跳过本轮", e);
                listeners.forEach(listener -> listener.accept(SKIPPED_LOCKED));
                return SKIPPED_LOCKED;
            }
        }
        try {
            int purged = archiver.purgeExpired(ttl, Instant.now());
            listeners.forEach(listener -> listener.accept(purged));
            if (purged > 0) {
                LOGGER.log(System.Logger.Level.INFO,
                        "归档 TTL 清理完成：删除 {0} 条（ttl={1}s）", purged, ttl.toSeconds());
            }
            return purged;
        } finally {
            if (lock != null) {
                try {
                    lock.release(lockOwner);
                } catch (java.io.IOException e) {
                    LOGGER.log(System.Logger.Level.WARNING, "归档清理释放锁失败（下轮按陈旧回收）", e);
                }
            }
        }
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
