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

    public DelayedJobQueue() {
        this(Clock.systemUTC());
    }

    public DelayedJobQueue(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
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
        ScheduledFuture<?> future = scheduler.schedule(() -> run(jobKey, task),
                delayMs, TimeUnit.MILLISECONDS);
        Scheduled previous = jobs.put(jobKey, new Scheduled(jobKey, future, fireAt));
        if (previous != null) {
            previous.future.cancel(false); // 替换——旧任务不双跑
        }
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
