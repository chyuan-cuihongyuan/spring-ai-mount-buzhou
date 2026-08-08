package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class AsyncObservabilityPipelineTest {

    @Test
    void flushPersistsSpansAndEvents() {
        RecordingStore store = new RecordingStore();
        try (AsyncObservabilityPipeline pipeline = new AsyncObservabilityPipeline(store,
                ObservabilityConfig.testDefaults(), null)) {
            SpanContext ctx = new SpanContext("s1", "session-1", 1);
            pipeline.openSpan(SpanKind.TURN, "turn-1", null).close();
            pipeline.emit(ctx, EventType.FINAL_REPLY, Map.of("content", "hi"));
            pipeline.flush();
        }
        assertThat(store.spans).hasSizeGreaterThanOrEqualTo(1);
        assertThat(store.events).hasSize(1);
        assertThat(store.events.get(0).type()).isEqualTo(EventType.FINAL_REPLY);
    }

    @Test
    void batchSizeTriggersDrainWithoutFlush() {
        // 批大小 1：每条入队后很快被 drain（不等 flush）
        RecordingStore store = new RecordingStore();
        ObservabilityConfig config = new ObservabilityConfig(true, 1, Duration.ofSeconds(60),
                Duration.ofSeconds(5), 10000, true, List.of(), 32768, true, true, false);
        try (AsyncObservabilityPipeline pipeline = new AsyncObservabilityPipeline(store, config, null)) {
            pipeline.emit(new SpanContext("e1", "session-2", 1), EventType.TOOL_INPUT, Map.of());
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(store.events).hasSize(1));
        }
    }

    @Test
    void flushIntervalTriggersDrain() {
        RecordingStore store = new RecordingStore();
        ObservabilityConfig config = new ObservabilityConfig(true, 1000, Duration.ofMillis(50),
                Duration.ofSeconds(5), 10000, true, List.of(), 32768, true, true, false);
        try (AsyncObservabilityPipeline pipeline = new AsyncObservabilityPipeline(store, config, null)) {
            pipeline.emit(new SpanContext("e2", "session-3", 1), EventType.THINKING, Map.of());
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(store.events).hasSize(1));
        }
    }

    @Test
    void storeFailureDoesNotThrowAndCountsError() {
        RecordingStore store = new RecordingStore();
        store.failSpans = true;
        try (AsyncObservabilityPipeline pipeline = new AsyncObservabilityPipeline(store,
                ObservabilityConfig.testDefaults(), null)) {
            pipeline.openSpan(SpanKind.TURN, "t", null).close();
            pipeline.flush();
            // 不抛异常即通过
        }
    }

    @Test
    void backpressureBlocksWhenQueueFull() throws InterruptedException {
        // 容量 1；用 latch 把后台 drain 暂停，确保队列被填满，第三次 put 必须阻塞
        RecordingStore store = new RecordingStore();
        store.saveLatch = new CountDownLatch(1); // 让 drain 在 applyBatch 时阻塞
        ObservabilityConfig config = new ObservabilityConfig(true, 1, Duration.ofSeconds(30),
                Duration.ofSeconds(5), 1, true, List.of(), 32768, true, true, false);
        try (AsyncObservabilityPipeline pipeline = new AsyncObservabilityPipeline(store, config, null)) {
            // 第一项：drain 取走进入 applyBatch，被 saveLatch 阻塞 → 队列空
            pipeline.emit(new SpanContext("a", "s", 1), EventType.TOOL_INPUT, Map.of());
            await().atMost(2, TimeUnit.SECONDS).until(() -> store.saveLatchStarted.getCount() == 0);
            // 第二项：drain 仍阻塞中，队列容量 1，第三项 put 必须阻塞
            pipeline.emit(new SpanContext("b", "s", 1), EventType.TOOL_INPUT, Map.of());
            CountDownLatch blocked = new CountDownLatch(1);
            Thread blocker = Thread.ofVirtual().start(() -> {
                pipeline.emit(new SpanContext("c", "s", 1), EventType.TOOL_INPUT, Map.of());
                blocked.countDown();
            });
            Thread.sleep(150);
            assertThat(blocker.isAlive()).isTrue();
            assertThat(blocked.getCount()).isEqualTo(1L);
            // 释放 drain，让阻塞 put 完成
            store.saveLatch.countDown();
            blocker.interrupt();
        }
    }

    @Test
    void closeFlushesRemaining() {
        RecordingStore store = new RecordingStore();
        SpanContext ctx = new SpanContext("z", "session-z", 1);
        // 慢 flush 间隔，close 时仍应把在途数据落库
        ObservabilityConfig config = new ObservabilityConfig(true, 1000, Duration.ofSeconds(30),
                Duration.ofSeconds(5), 10000, true, List.of(), 32768, true, true, false);
        AsyncObservabilityPipeline pipeline = new AsyncObservabilityPipeline(store, config, null);
        pipeline.emit(ctx, EventType.ERROR, Map.of("message", "boom"));
        pipeline.close();
        assertThat(store.events).hasSize(1);
    }

    static class RecordingStore implements ObservabilityStore {
        final CopyOnWriteArrayList<SpanRecord> spans = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<EventRecord> events = new CopyOnWriteArrayList<>();
        volatile boolean failSpans;
        final AtomicInteger spanSaves = new AtomicInteger();
        /** 非空时，saveEvents 在标记 saveLatchStarted 后阻塞于此 latch，模拟慢 store 占用 drain 线程。 */
        volatile CountDownLatch saveLatch;
        final CountDownLatch saveLatchStarted = new CountDownLatch(1);

        @Override
        public void saveSpans(List<SpanRecord> spans) {
            spanSaves.incrementAndGet();
            if (failSpans) {
                throw new RuntimeException("simulated store failure");
            }
            this.spans.addAll(spans);
        }

        @Override
        public void saveEvents(List<EventRecord> events) {
            CountDownLatch latch = saveLatch;
            if (latch != null) {
                saveLatchStarted.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            this.events.addAll(events);
        }

        @Override
        public List<SpanRecord> spansOfSession(String sessionId) {
            return spans.stream().filter(s -> s.sessionId().equals(sessionId)).toList();
        }

        @Override
        public List<EventRecord> eventsOfSession(String sessionId) {
            return events.stream().filter(e -> e.sessionId().equals(sessionId)).toList();
        }

        @Override
        public void saveInjectionSnapshot(io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot snapshot) {
        }

        @Override
        public java.util.Optional<io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot>
                injectionSnapshot(String sessionId, int turnSeq) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary>
                listSessionSummaries(String cursor, int size) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<EventRecord> eventsOfSpan(String spanId) {
            return events.stream().filter(e -> e.spanId() != null && e.spanId().equals(spanId)).toList();
        }
    }
}
