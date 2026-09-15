package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseSpanRecorder sink 分发补测（K 会话 R15 / spec 1214 / T1837——R7 逐类分支数据精定制导）：
 * enqueue 时刻旁路分发（span/event 到达 sink）、逐 sink 异常隔离（落库不受污染、后续 sink 仍达）、
 * PendingSnapshot 不经旁路、sinks 空时直接落库。
 * 先例：DefaultSpanHandleBranchTest（RecordingBase extends BaseSpanRecorder）。
 */
class BaseSpanRecorderSinkDispatchTest {

    /** 录制型 sink：记到达的 span/event + 可选抛错。 */
    static final class RecordingSink implements PipelineSink {
        final List<String> received = new ArrayList<>();
        RuntimeException throwOn;

        @Override
        public void onSpan(SpanRecord record) {
            if (throwOn != null) {
                throw throwOn;
            }
            received.add("span:" + record.name());
        }

        @Override
        public void onEvent(EventRecord record) {
            if (throwOn != null) {
                throw throwOn;
            }
            received.add("event:" + record.type());
        }
    }

    static final class RecordingBase extends BaseSpanRecorder {
        final List<PendingItem> items = new ArrayList<>();

        RecordingBase(PipelineSink... sinks) {
            super(new MicrometerDualWriter(), false, List.of(sinks));
        }

        @Override
        protected void doEnqueue(PendingItem item) {
            items.add(item);
        }

        @Override
        public void flush() {
        }
    }

    private static SpanRecord toolSpan(String name, String status) {
        return new SpanRecord("sp-" + name, null, "sess-1", 1, SpanKind.TOOL_CALL, name,
                Instant.ofEpochSecond(1), Instant.ofEpochSecond(2), status, Map.of());
    }

    private static EventRecord evt(String type) {
        return new EventRecord("evt-" + type, "sp-1", "sess-1", type, Instant.ofEpochSecond(1), Map.of());
    }

    @Test
    void spanAndEventDispatchReachSinksAtEnqueueTime() {
        RecordingSink sink = new RecordingSink();
        RecordingBase recorder = new RecordingBase(sink);

        recorder.enqueue(new PendingSpan(toolSpan("read_file", "OK")));
        recorder.enqueue(new PendingEvent(evt("TOOL_INPUT")));

        assertThat(sink.received).containsExactly("span:read_file", "event:TOOL_INPUT");
        // 落库与旁路分发并行不悖
        assertThat(recorder.items).hasSize(2);
    }

    @Test
    void throwingSinkIsIsolatedAndLaterSinksStillReceive() {
        RecordingSink broken = new RecordingSink();
        broken.throwOn = new IllegalStateException("sink 爆炸");
        RecordingSink healthy = new RecordingSink();
        RecordingBase recorder = new RecordingBase(broken, healthy);

        recorder.enqueue(new PendingSpan(toolSpan("read_file", "OK")));
        recorder.enqueue(new PendingEvent(evt("TOOL_OUTPUT")));

        // 异常隔离：broken 抛错不污染落库；healthy 照常收到两笔
        assertThat(recorder.items).hasSize(2);
        assertThat(healthy.received).containsExactly("span:read_file", "event:TOOL_OUTPUT");
    }

    @Test
    void pendingSnapshotBypassesSinks() {
        RecordingSink sink = new RecordingSink();
        RecordingBase recorder = new RecordingBase(sink);

        io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot snapshot =
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot(
                        "sess-1", 1, List.of(), List.of(), Map.of(), "", Instant.now());
        recorder.enqueue(new PendingSnapshot(snapshot));

        // 快照不经旁路 sink（default 臂跳过），仅落库
        assertThat(sink.received).isEmpty();
        assertThat(recorder.items).hasSize(1);
    }

    @Test
    void noSinksMeansNoDispatchAndStoreStillHappens() {
        RecordingBase recorder = new RecordingBase();
        AtomicInteger counter = new AtomicInteger();

        recorder.enqueue(new PendingSpan(toolSpan("read_file", "OK")));

        assertThat(counter.get()).isZero();
        assertThat(recorder.items).hasSize(1);
    }

    @Test
    void dispatchOrderPreservesEnqueueSequence() {
        RecordingSink sink = new RecordingSink();
        RecordingBase recorder = new RecordingBase(sink);

        // open→event→close 调用顺序在旁路分发中保持（旁路消费者依赖原始时序）
        recorder.enqueue(new PendingSpan(toolSpan("read_file", "OK")));
        recorder.enqueue(new PendingEvent(evt("TOOL_INPUT")));
        recorder.enqueue(new PendingEvent(evt("TOOL_OUTPUT")));
        recorder.enqueue(new PendingSpan(toolSpan("read_file", "ERROR")));

        assertThat(sink.received).containsExactly(
                "span:read_file", "event:TOOL_INPUT", "event:TOOL_OUTPUT", "span:read_file");
    }
}
