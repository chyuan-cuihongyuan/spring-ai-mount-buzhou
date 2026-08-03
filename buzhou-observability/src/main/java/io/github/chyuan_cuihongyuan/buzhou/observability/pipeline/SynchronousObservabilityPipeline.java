package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;

import java.util.List;

/**
 * 同步直写管线（测试用）：每次 enqueue 立即落库，无异步 drain，便于测试即时断言。
 *
 * <p>语义与 {@link AsyncObservabilityPipeline} 对齐（同一 {@link BaseSpanRecorder}），仅在落库时机上同步。
 * 生产路径用 {@link AsyncObservabilityPipeline}。
 */
public class SynchronousObservabilityPipeline extends BaseSpanRecorder {

    private final ObservabilityStore store;

    public SynchronousObservabilityPipeline(ObservabilityStore store, ObservabilityConfig config,
                                            MicrometerDualWriter meters) {
        super(meters, config.includeStacktrace());
        this.store = store;
    }

    @Override
    public void enqueue(PendingItem item) {
        switch (item) {
            case PendingSpan s -> store.saveSpans(List.of(s.record()));
            case PendingEvent e -> store.saveEvents(List.of(e.record()));
            case PendingSnapshot snap -> store.saveInjectionSnapshot(snap.record());
            case FlushToken t -> t.complete();
        }
    }

    @Override
    public void flush() {
        // 同步管线无在途队列，flush 为空操作
    }
}
