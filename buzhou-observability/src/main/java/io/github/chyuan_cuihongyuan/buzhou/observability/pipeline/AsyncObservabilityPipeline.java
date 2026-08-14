package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步批量落库管线（spec 03 异步落库管线）。
 *
 * <p>有界内存队列（默认容量 10000）→ 后台虚拟线程批量 drain → {@link ObservabilityStore}。
 * 批大小（默认 200）或 flush 间隔（默认 1s）先到先触发；会话 close 强制 flush（由
 * {@code ObservabilitySessionState.onClose} → {@link #flush()} 触发）。
 *
 * <p><b>无采样</b>（spec 推演 5）：队列满时 {@code put} 阻塞 = 背压不丢；写入等待记
 * {@code buzhou.observability.queue.wait} Timer。
 *
 * <p>写库异常：捕获、记日志、不抛（观测故障不拖死主链路）；累计 {@code buzhou.observability.persist.errors}。
 *
 * <p>JVM shutdown hook 兜底 flush（spec 03：会话 close 与 JVM shutdown hook 强制 flush）。
 * 单实例管线（per-JVM，由 {@link #configure} 创建一次）注册一个 hook；会话级 close 走
 * {@code SessionObserver.onClose} 而非此 hook。
 */
public class AsyncObservabilityPipeline extends BaseSpanRecorder implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(AsyncObservabilityPipeline.class.getName());

    private final ObservabilityStore store;
    private final ObservabilityConfig config;
    private final BlockingQueue<PendingItem> queue;
    private final Thread drainThread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread shutdownHook;

    public AsyncObservabilityPipeline(ObservabilityStore store, ObservabilityConfig config,
                                      MicrometerDualWriter meters) {
        this(store, config, meters, List.of());
    }

    /** 全参构造：额外接入旁路 {@link PipelineSink}（OTel 导出桥等）。 */
    public AsyncObservabilityPipeline(ObservabilityStore store, ObservabilityConfig config,
                                      MicrometerDualWriter meters, List<PipelineSink> sinks) {
        super(meters, config.includeStacktrace(), sinks);
        this.store = store;
        this.config = config;
        this.queue = new ArrayBlockingQueue<>(Math.max(1, config.queueCapacity()));
        this.drainThread = Thread.ofVirtual().name("buzhou-obs-drain").unstarted(this::drainLoop);
        this.drainThread.start();
        this.shutdownHook = new Thread(this::shutdownForJvmHook, "buzhou-obs-shutdown");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM 已在关停中，无需 hook
        }
    }

    @Override
    protected void doEnqueue(PendingItem item) {
        if (!running.get()) {
            // 已关闭：直接同步落库，避免丢数据
            applyOne(item);
            return;
        }
        long start = System.nanoTime();
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            applyOne(item);
            return;
        }
        meters.recordQueueWait(Duration.ofNanos(System.nanoTime() - start).toMillis());
    }

    @Override
    public void flush() {
        FlushToken token = new FlushToken();
        try {
            queue.put(token);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            drainBatch();
            return;
        }
        try {
            if (!token.done().await(config.flushTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                drainBatch();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            drainBatch();
        }
    }

    @Override
    public void close() {
        if (!running.get()) {
            return;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
        }
        // 先经 FlushToken 让存活 drain 线程处理完队列（FIFO 保证 token 之前的条目全部落库），
        // 再停线程——反序则 token 无人处理，close 必白等满 flushTimeout。
        flush();
        running.set(false);
        drainThread.interrupt();
        try {
            drainThread.join(config.flushTimeout().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // token 之后、running=false 之前入队的残留条目兜底 drain
        drainBatch();
    }

    private void shutdownForJvmHook() {
        running.set(false);
        drainBatch();
    }

    private void drainLoop() {
        List<PendingItem> batch = new ArrayList<>(config.batchSize());
        while (running.get()) {
            batch.clear();
            try {
                PendingItem first = queue.poll(config.flushInterval().toMillis(), TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                queue.drainTo(batch, config.batchSize() - 1);
                applyBatch(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 退出前再 drain 一次，避免残留
        drainBatch();
    }

    private void drainBatch() {
        List<PendingItem> batch = new ArrayList<>();
        queue.drainTo(batch);
        if (!batch.isEmpty()) {
            applyBatch(batch);
        }
    }

    private void applyBatch(List<PendingItem> batch) {
        List<SpanRecord> spans = new ArrayList<>();
        List<EventRecord> events = new ArrayList<>();
        List<FlushToken> tokens = new ArrayList<>();
        for (PendingItem item : batch) {
            switch (item) {
                case PendingSpan s -> spans.add(s.record());
                case PendingEvent e -> events.add(e.record());
                case PendingSnapshot snap -> safeStore(() -> store.saveInjectionSnapshot(snap.record()));
                case FlushToken t -> tokens.add(t);
            }
        }
        if (!spans.isEmpty()) {
            safeStore(() -> store.saveSpans(spans));
        }
        if (!events.isEmpty()) {
            safeStore(() -> store.saveEvents(events));
        }
        tokens.forEach(FlushToken::complete);
    }

    private void applyOne(PendingItem item) {
        switch (item) {
            case PendingSpan s -> safeStore(() -> store.saveSpans(List.of(s.record())));
            case PendingEvent e -> safeStore(() -> store.saveEvents(List.of(e.record())));
            case PendingSnapshot snap -> safeStore(() -> store.saveInjectionSnapshot(snap.record()));
            case FlushToken t -> t.complete();
        }
    }

    private void safeStore(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            // impl-46：观测自身故障必须可见（此前纯吞，生产排障不可诊断）
            meters.recordPersistError();
            LOGGER.log(System.Logger.Level.WARNING,
                    "观测数据落库失败（已隔离，不影响主链路）：" + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
        }
    }
}
