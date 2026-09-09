package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 健康时间线轮询记录器（spec 405 / T701）：SmartLifecycle 周期轮询 health
 * beans（与 312 告警引擎同源 supplier 口径）→ diff → 入环 + 可选 sink（导出）。
 * 独立调度——关时间线不影响告警引擎。
 */
public final class HealthTimelineRecorder implements org.springframework.context.SmartLifecycle {

    private final Supplier<Map<String, BuzhouHealth>> healthSource;
    private final Duration interval;
    private final Consumer<HealthTimeline.Entry> sink;
    private final HealthTimeline timeline;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ScheduledExecutorService scheduler;

    public HealthTimelineRecorder(Supplier<Map<String, BuzhouHealth>> healthSource,
            Duration interval, Consumer<HealthTimeline.Entry> sink) {
        this(healthSource, interval, sink, new HealthTimeline());
    }

    public HealthTimelineRecorder(Supplier<Map<String, BuzhouHealth>> healthSource,
            Duration interval, Consumer<HealthTimeline.Entry> sink, HealthTimeline timeline) {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval 为正");
        }
        this.healthSource = healthSource;
        this.interval = interval;
        this.sink = sink == null ? e -> { } : sink;
        this.timeline = timeline == null ? new HealthTimeline() : timeline;
    }

    /** 时间线（端点/测试读）。 */
    public HealthTimeline timeline() {
        return timeline;
    }

    /** 手动采一轮（测试确定性路径——不依赖调度器节拍）。 */
    public void pollOnce(Instant at) {
        Map<String, BuzhouHealth.Status> snapshot = new LinkedHashMap<>();
        healthSource.get().forEach((name, health) -> snapshot.put(name, health.status()));
        for (HealthTimeline.Entry entry : timeline.record(snapshot, at)) {
            sink.accept(entry);
        }
    }

    // ---- SmartLifecycle ----

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory
                            .platform("buzhou-health-timeline"));
            scheduler.scheduleAtFixedRate(() -> pollOnce(Instant.now()),
                    interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            ScheduledExecutorService s = scheduler;
            scheduler = null;
            if (s != null) {
                s.shutdownNow();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
