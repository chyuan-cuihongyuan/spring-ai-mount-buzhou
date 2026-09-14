package io.github.chyuan_cuihongyuan.buzhou.observability.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObservabilitySessionState 分支补测（K 会话 R9 / spec 1208 / T1825——R7 逐类分支数据精定制导）：
 * 无 session 早退、长输入 200 截断与 null 输入、usage 累加/重置、onTurnError 标记、
 * 二次 onTurnEnd 幂等、onCancel CANCELLED 终态、carrier null 防御。
 */
class ObservabilitySessionStateTest {

    /** 录制型 recorder（无 Mockito 手写 fake——仓库惯例）。 */
    static final class RecordingRecorder implements SpanRecorder {
        record Opened(String kind, String name, SpanContext parent, Map<String, Object> attrs, SpanContext explicit) {
        }

        final List<Opened> opened = new ArrayList<>();
        final List<RecordingHandle> handles = new ArrayList<>();
        boolean flushed;

        @Override
        public SpanHandle openSpan(String kind, String name, SpanContext parent) {
            return openSpan(kind, name, parent, Map.of());
        }

        @Override
        public SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes) {
            return openSpan(kind, name, parent, attributes, null);
        }

        @Override
        public SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes,
                                   SpanContext explicit) {
            opened.add(new Opened(kind, name, parent, attributes, explicit));
            RecordingHandle handle = new RecordingHandle(
                    new SpanContext("sp-" + opened.size(), "sess-1", 1));
            handles.add(handle);
            return handle;
        }

        @Override
        public void emit(SpanContext span, String type, Map<String, Object> payload) {
            // 本类不经 emit 路径
        }

        @Override
        public void flush() {
            flushed = true;
        }
    }

    static final class RecordingHandle implements SpanHandle {
        final SpanContext context;
        final Map<String, Object> attrs = new LinkedHashMap<>();
        final List<String> closedWith = new ArrayList<>();
        Throwable error;

        RecordingHandle(SpanContext context) {
            this.context = context;
        }

        @Override
        public SpanContext context() {
            return context;
        }

        @Override
        public SpanHandle attribute(String key, Object value) {
            attrs.put(key, value);
            return this;
        }

        @Override
        public SpanHandle attributes(Map<String, Object> attributes) {
            attrs.putAll(attributes);
            return this;
        }

        @Override
        public void error(Throwable t) {
            this.error = t;
        }

        @Override
        public void close(String status) {
            closedWith.add(status);
        }

        @Override
        public void close() {
            closedWith.add("OK");
        }
    }

    private RecordingRecorder recorder;
    private ObservabilitySessionState state;

    @BeforeEach
    void setUp() {
        recorder = new RecordingRecorder();
        state = new ObservabilitySessionState(recorder, null, "sess-1", "agent", "app", "model");
    }

    @Test
    void onOpenOpensSessionRootAndBindsCarrier() {
        SpanContextCarrier carrier = new SpanContextCarrier();
        ObservabilitySessionState withCarrier = new ObservabilitySessionState(
                recorder, carrier, "sess-1", "agent", "app", "model");

        withCarrier.onOpen();

        RecordingRecorder.Opened opened = recorder.opened.get(0);
        assertThat(opened.kind()).isEqualTo(SpanKind.SESSION);
        assertThat(opened.name()).isEqualTo("session");
        assertThat(opened.parent()).isNull();
        assertThat(opened.attrs()).containsEntry("agent.name", "agent")
                .containsEntry("app.id", "app").containsEntry("model.name", "model");
        assertThat(opened.explicit()).isNotNull(); // SESSION 根显式携带 sessionId
        assertThat(carrier.sessionSpan()).isNotNull();
        assertThat(withCarrier.sessionSpan()).isNotNull();
    }

    @Test
    void onTurnStartWithoutOpenIsNoOp() {
        state.onTurnStart(1, "hi");

        assertThat(recorder.opened).isEmpty();
        assertThat(state.currentTurnSeq("sess-1")).isNull();
    }

    @Test
    void onTurnStartTruncatesLongInputAndResetsCounters() {
        state.onOpen();
        state.onTurnStart(1, "start");
        state.accumulateTurnUsage(10, 5);
        state.nextIteration("sess-1");
        state.onTurnEnd(1, "done");
        state.onTurnStart(2, "x".repeat(300)); // 长输入截断 + 计数器重置
        state.onTurnEnd(2, "done");

        RecordingHandle secondTurn = recorder.handles.get(1);
        assertThat(secondTurn.attrs.get("user.input.preview")).asString().hasSize(200);
        assertThat(secondTurn.attrs.get("usage.prompt_tokens")).isEqualTo(0);
        assertThat(secondTurn.attrs.get("usage.completion_tokens")).isEqualTo(0);
        assertThat(secondTurn.attrs.get("iteration.count")).isEqualTo(0);
    }

    @Test
    void onTurnStartWithNullInputOmitsPreview() {
        state.onOpen();
        state.onTurnStart(1, null);
        state.onTurnEnd(1, "done");

        RecordingHandle turn = recorder.handles.get(1);
        assertThat(turn.attrs).doesNotContainKey("user.input.preview");
    }

    @Test
    void onTurnEndWritesUsageAndIsIdempotent() {
        state.onOpen();
        state.onTurnStart(1, null);
        state.accumulateTurnUsage(7, 3);
        state.nextIteration("sess-1");
        state.nextIteration("sess-1");
        state.onTurnEnd(1, "reply");
        state.onTurnEnd(1, "reply"); // 二次幂等

        RecordingHandle turn = recorder.handles.get(1);
        assertThat(turn.attrs.get("turn.completed")).isEqualTo(true);
        assertThat(turn.attrs.get("usage.prompt_tokens")).isEqualTo(7);
        assertThat(turn.attrs.get("usage.completion_tokens")).isEqualTo(3);
        assertThat(turn.attrs.get("iteration.count")).isEqualTo(2);
        assertThat(turn.closedWith).containsExactly("OK");
    }

    @Test
    void accumulateTurnUsageIgnoresNullSides() {
        state.onOpen();
        state.onTurnStart(1, null);
        state.accumulateTurnUsage(null, 5);
        state.onTurnEnd(1, "done");

        RecordingHandle turn = recorder.handles.get(1);
        assertThat(turn.attrs.get("usage.prompt_tokens")).isEqualTo(0);
        assertThat(turn.attrs.get("usage.completion_tokens")).isEqualTo(5);
    }

    @Test
    void onTurnErrorMarksFailureAndCloses() {
        state.onOpen();
        state.onTurnStart(1, null);
        RuntimeException boom = new RuntimeException("boom");
        state.onTurnError(1, boom);

        RecordingHandle turn = recorder.handles.get(1);
        assertThat(turn.attrs.get("turn.completed")).isEqualTo(false);
        assertThat(turn.error).isSameAs(boom);
        assertThat(turn.closedWith).containsExactly("OK");
    }

    @Test
    void onTurnErrorWithoutOpenIsNoOp() {
        state.onTurnError(1, new RuntimeException("boom"));
        assertThat(recorder.opened).isEmpty();
    }

    @Test
    void onCloseFlushesAndOnCancelClosesWithCancelledStatus() {
        state.onOpen();
        state.onCancel();

        RecordingHandle session = recorder.handles.get(0);
        assertThat(session.closedWith).containsExactly("CANCELLED");
        assertThat(recorder.flushed).isTrue();

        state.onOpen();
        state.onClose();
        assertThat(recorder.flushed).isTrue();
    }
}
