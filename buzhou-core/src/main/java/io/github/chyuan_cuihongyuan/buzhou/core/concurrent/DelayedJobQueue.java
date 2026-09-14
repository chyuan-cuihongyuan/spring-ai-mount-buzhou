package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 延迟作业队列（spec 413 / T717，Sidekiq delayed_jobs 借鉴——one-shot 到点
 * 执行一次）：{@code submit(jobKey, task, fireAt|delay)}——同 key 重复提交
 * <b>替换</b>旧任务（键即幂等锚，重提交刷新时间不双跑）；cancel 幂等；
 * 单 daemon 虚拟线程调度器；作业异常隔离（吞 + 计数 buzhou.jobs.failed
 * ——不炸调度线程）；pending() 待跑清单快照（键+fireAt 升序，任务引用
 * 不外泄）；close 停调度器（在途作业不等待——生命周期族口径）。进程内
 * 诚实边界：重启丢作业。
 */
public final class DelayedJobQueue implements AutoCloseable {

    /** 待跑作业行（观测面——任务引用不外泄）。 */
    public record PendingJob(String jobKey, Instant fireAt) {
    }

    private static final class Scheduled {
        final String jobKey;
        final ScheduledFuture<?> future;
        final Instant fireAt;

        Scheduled(String jobKey, ScheduledFuture<?> future, Instant fireAt) {
            this.jobKey = jobKey;
            this.future = future;
            this.fireAt = fireAt;
        }
    }

    private final ScheduledExecutorService scheduler;
    private final Clock clock;
    private final Map<String, Scheduled> jobs = new ConcurrentHashMap<>();
    private final AtomicLong failed = new AtomicLong();
    private final java.util.function.BiConsumer<String, Throwable> failureObserver;

    public DelayedJobQueue() {
        this(Clock.systemUTC());
    }

    public DelayedJobQueue(Clock clock) {
        this(clock, null);
    }

    /**
     * spec 809 / T1119：带失败观察者构造（死信台账挂接点）——作业异常时回调
     * {@code (jobKey, error)}（观察者内部异常亦被隔离，不影响调度线程）；
     * null = 原行为（吞 + 计数）。
     */
    public DelayedJobQueue(Clock clock, java.util.function.BiConsumer<String, Throwable> failureObserver) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.failureObserver = failureObserver;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                BuzhouThreadFactory.platform("buzhou-delayed-jobs"));
    }

    /** 到点执行一次（fireAt 已过 = 立即）；同 key 重复提交替换旧任务。 */
    public void submit(String jobKey, Runnable task, Instant fireAt) {
        requireJobKey(jobKey);
        if (task == null) {
            throw new IllegalArgumentException("task 非空（jobKey=" + jobKey + "）");
        }
        if (fireAt == null) {
            throw new IllegalArgumentException("fireAt 非空（jobKey=" + jobKey + "）");
        }
        Instant now = clock.instant();
        long delayMs = Math.max(0, Duration.between(now, fireAt).toMillis());
        ScheduledFuture<?> future = scheduler.schedule(() -> run(jobKey, () -> {
            // spec 1434 / T2169：调度漂移埋点——实际起跑 vs 计划 fireAt（Sidekiq
            // queue latency 思想：漂移大 = 调度线程饥饿，作业「准时性」承诺失守）
            long drift = Math.max(0, Duration.between(fireAt, clock.instant()).toMillis());
            recordDrift(drift);
            task.run();
        }), delayMs, TimeUnit.MILLISECONDS);
        Scheduled previous = jobs.put(jobKey, new Scheduled(jobKey, future, fireAt));
        if (previous != null) {
            previous.future.cancel(false); // 替换——旧任务不双跑
        }
    }

    /** 调度漂移读数（spec 1434 / T2169）：executed/lastDriftMillis/maxDriftMillis。 */
    public record DriftStats(long executed, long lastDriftMillis, long maxDriftMillis) {
    }

    private final java.util.concurrent.atomic.AtomicLong executedJobs =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong lastDrift =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong maxDrift =
            new java.util.concurrent.atomic.AtomicLong();

    private void recordDrift(long driftMillis) {
        lastDrift.set(driftMillis);
        maxDrift.accumulateAndGet(driftMillis, Math::max);
        executedJobs.incrementAndGet();
    }

    /** 只读快照：累计执行数 + 末次/最大调度漂移（毫秒）。 */
    public DriftStats driftStats() {
        return new DriftStats(executedJobs.get(), lastDrift.get(), maxDrift.get());
    }

    /** 测试归零口（调度器状态不动）。 */
    public void resetDriftForTest() {
        executedJobs.set(0);
        lastDrift.set(0);
        maxDrift.set(0);
    }

    /** 延迟执行一次（delay 非负）。 */
    public void submit(String jobKey, Runnable task, Duration delay) {
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("delay 非负（jobKey=" + jobKey + "）");
        }
        submit(jobKey, task, clock.instant().plus(delay));
    }

    /** 撤销（幂等；已跑/不在 = no-op）。 */
    public void cancel(String jobKey) {
        Scheduled existing = jobKey == null ? null : jobs.remove(jobKey);
        if (existing != null) {
            existing.future.cancel(false);
        }
    }

    /** 待跑清单（fireAt 升序）。 */
    public List<PendingJob> pending() {
        return jobs.values().stream()
                .map(s -> new PendingJob(s.jobKey, s.fireAt))
                .sorted(java.util.Comparator.comparing(PendingJob::fireAt))
                .toList();
    }

    /** 作业失败累计（异常隔离观测）。 */
    public long failedCount() {
        return failed.get();
    }

    private void run(String jobKey, Runnable task) {
        jobs.remove(jobKey);
        try {
            task.run();
        } catch (RuntimeException e) {
            failed.incrementAndGet();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.jobs.failed");
            if (failureObserver != null) {
                try {
                    failureObserver.accept(jobKey, e);
                } catch (RuntimeException ignored) {
                    // 观察者异常隔离——不炸调度线程（与作业异常同口径）
                }
            }
        }
    }

    /** 停调度器（幂等；在途作业不等待）。 */
    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    private static void requireJobKey(String jobKey) {
        if (jobKey == null || jobKey.isBlank()) {
            throw new IllegalArgumentException("jobKey 非空");
        }
    }
}
