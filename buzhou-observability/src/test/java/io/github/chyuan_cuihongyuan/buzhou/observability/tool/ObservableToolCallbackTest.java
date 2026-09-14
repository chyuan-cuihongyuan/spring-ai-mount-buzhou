package io.github.chyuan_cuihongyuan.buzhou.observability.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.observability.advisor.ObservabilityAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ObservableToolCallback 分支补测（K 会话 R9 / spec 1208 / T1825——R7 逐类分支数据精定制导）：
 * 成功路径四段（开 span→TOOL_INPUT→委托→TOOL_OUTPUT→close）、委托异常 error+close+rethrow、
 * parent 解析三优先级（ToolContext 载体 > 字段载体 > hooks.sessionSpan 兜底）、carrier null 防御。
 */
class ObservableToolCallbackTest {

    /** 录制型 recorder（无 Mockito 手写 fake——仓库惯例）。 */
    static final class RecordingRecorder implements SpanRecorder {
        record Opened(String kind, String name, SpanContext parent, Map<String, Object> attrs) {
        }

        final List<Opened> opened = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        final List<String> errored = new ArrayList<>();
        final List<String> closed = new ArrayList<>();

        private SpanHandle handle(SpanContext ctx) {
            return new SpanHandle() {
                @Override
                public SpanContext context() {
                    return ctx;
                }

                @Override
                public SpanHandle attribute(String key, Object value) {
                    return this;
                }

                @Override
                public SpanHandle attributes(Map<String, Object> attributes) {
                    return this;
                }

                @Override
                public void error(Throwable t) {
                    errored.add(t.getClass().getSimpleName());
                }

                @Override
                public void close(String status) {
                    closed.add(status);
                }

                @Override
                public void close() {
                    closed.add("OK");
                }
            };
        }

        @Override
        public SpanHandle openSpan(String kind, String name, SpanContext parent) {
            return openSpan(kind, name, parent, Map.of());
        }

        @Override
        public SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes) {
            opened.add(new Opened(kind, name, parent, attributes));
            return handle(new SpanContext("sp-" + opened.size(), "sess-1", 1));
        }

        @Override
        public SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes,
                                   SpanContext explicit) {
            return openSpan(kind, name, parent, attributes);
        }

        @Override
        public void emit(SpanContext span, String type, Map<String, Object> payload) {
            events.add(type);
        }

        @Override
        public void flush() {
        }
    }

    private static ToolCallback delegateReturning(String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("read_file")
                        .description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    private static ToolCallback delegateThrowing() {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("read_file")
                        .description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("boom");
            }
        };
    }

    private static ObservabilityAdvisor.ObservabilitySessionHooks hooksWithSessionSpan(SpanContext ctx) {
        return new ObservabilityAdvisor.ObservabilitySessionHooks() {
            @Override
            public SpanContext sessionSpan() {
                return ctx;
            }

            @Override
            public SpanContext turnSpan() {
                return null;
            }

            @Override
            public Integer currentTurnSeq(String sessionId) {
                return null;
            }

            @Override
            public int nextIteration(String sessionId) {
                return 1;
            }
        };
    }

    @Test
    void successPathOpensSpanEmitsInputOutputAndCloses() {
        RecordingRecorder recorder = new RecordingRecorder();
        SpanContextCarrier carrier = new SpanContextCarrier();
        SpanContext turn = new SpanContext("turn-span", "sess-1", 1);
        carrier.bindTurn(turn);
        ObservableToolCallback callback = new ObservableToolCallback(
                delegateReturning("file body"), recorder,
                hooksWithSessionSpan(new SpanContext("root", "sess-1", 0)), carrier);

        String result = callback.call("{\"path\":\"/a\"}", new ToolContext(
                Map.of(SpanContextCarrier.KEY, carrier)));

        assertThat(result).isEqualTo("file body");
        assertThat(recorder.opened).hasSize(1);
        RecordingRecorder.Opened opened = recorder.opened.get(0);
        assertThat(opened.kind()).isEqualTo("TOOL_CALL");
        assertThat(opened.name()).isEqualTo("tool:read_file");
        assertThat(opened.parent()).isEqualTo(turn); // ToolContext 载体优先
        assertThat(opened.attrs()).containsEntry("tool.name", "read_file").containsEntry("tool.type", "function");
        assertThat(recorder.events).containsExactly("TOOL_INPUT", "TOOL_OUTPUT");
        assertThat(recorder.closed).containsExactly("OK");
    }

    @Test
    void delegateExceptionMarksErrorClosesAndRethrows() {
        RecordingRecorder recorder = new RecordingRecorder();
        ObservableToolCallback callback = new ObservableToolCallback(
                delegateThrowing(), recorder, null, null);

        assertThatThrownBy(() -> callback.call("{}"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(recorder.errored).containsExactly("IllegalStateException");
        assertThat(recorder.closed).containsExactly("OK");
        assertThat(recorder.events).containsExactly("TOOL_INPUT"); // 无 TOOL_OUTPUT
    }

    @Test
    void toolContextCarrierTakesPrecedenceOverFieldCarrier() {
        RecordingRecorder recorder = new RecordingRecorder();
        SpanContextCarrier fieldCarrier = new SpanContextCarrier();
        SpanContextCarrier ctxCarrier = new SpanContextCarrier();
        SpanContext ctxTurn = new SpanContext("ctx-turn", "sess-1", 2);
        ctxCarrier.bindTurn(ctxTurn);
        fieldCarrier.bindTurn(new SpanContext("field-turn", "sess-1", 1));
        ObservableToolCallback callback = new ObservableToolCallback(
                delegateReturning("ok"), recorder, null, fieldCarrier);

        callback.call("{}", new ToolContext(Map.of(SpanContextCarrier.KEY, ctxCarrier)));

        assertThat(recorder.opened.get(0).parent()).isEqualTo(ctxTurn);
    }

    @Test
    void fieldCarrierFallsBackWhenToolContextHasNoCarrier() {
        RecordingRecorder recorder = new RecordingRecorder();
        SpanContextCarrier fieldCarrier = new SpanContextCarrier();
        SpanContext fieldTurn = new SpanContext("field-turn", "sess-1", 1);
        fieldCarrier.bindTurn(fieldTurn);
        ObservableToolCallback callback = new ObservableToolCallback(
                delegateReturning("ok"), recorder, null, fieldCarrier);

        callback.call("{}", new ToolContext(Map.of()));

        assertThat(recorder.opened.get(0).parent()).isEqualTo(fieldTurn);
    }

    @Test
    void noCarrierAnywhereFallsBackToHooksSessionSpan() {
        RecordingRecorder recorder = new RecordingRecorder();
        SpanContext session = new SpanContext("root", "sess-1", 0);
        ObservableToolCallback callback = new ObservableToolCallback(
                delegateReturning("ok"), recorder, hooksWithSessionSpan(session), null);

        callback.call("{}", null); // toolContext null：resolveParent 第一分支

        assertThat(recorder.opened.get(0).parent()).isEqualTo(session);
        // null 载体：无 tool.parallel.index
        assertThat(recorder.opened.get(0).attrs()).doesNotContainKey("tool.parallel.index");
    }

    @Test
    void parallelIndexCountsFromCarrier() {
        RecordingRecorder recorder = new RecordingRecorder();
        SpanContextCarrier carrier = new SpanContextCarrier();
        carrier.bindTurn(new SpanContext("turn", "sess-1", 1));
        ObservableToolCallback callback = new ObservableToolCallback(
                delegateReturning("ok"), recorder, null, carrier);

        callback.call("{}", new ToolContext(Map.of(SpanContextCarrier.KEY, carrier)));
        callback.call("{}", new ToolContext(Map.of(SpanContextCarrier.KEY, carrier)));

        assertThat(recorder.opened.get(0).attrs().get("tool.parallel.index"))
                .isNotEqualTo(recorder.opened.get(1).attrs().get("tool.parallel.index"));
    }

    @Test
    void singleArgCallDelegatesWithContext() {
        RecordingRecorder recorder = new RecordingRecorder();
        ObservableToolCallback callback = new ObservableToolCallback(
                delegateReturning("ok"), recorder, null, null);

        String result = callback.call("input");

        assertThat(result).isEqualTo("ok");
        assertThat(recorder.opened).hasSize(1);
    }

    @Test
    void definitionsDelegate() {
        ToolCallback delegate = delegateReturning("ok");
        ObservableToolCallback callback = new ObservableToolCallback(delegate, new RecordingRecorder(), null, null);

        assertThat(callback.getToolDefinition()).isEqualTo(delegate.getToolDefinition());
        assertThat(callback.getToolMetadata()).isEqualTo(delegate.getToolMetadata());
        assertThat(callback.delegate()).isSameAs(delegate);
        assertThat(callback.getToolDefinition().name()).isEqualTo("read_file");
    }
}
