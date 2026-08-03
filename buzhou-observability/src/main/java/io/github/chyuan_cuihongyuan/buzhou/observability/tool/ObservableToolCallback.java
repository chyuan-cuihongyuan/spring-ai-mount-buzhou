package io.github.chyuan_cuihongyuan.buzhou.observability.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.observability.advisor.ObservabilityAdvisor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具调用可观测包装（spec 03 挂接点）：从 {@link ToolContext}（或会话级 {@link SpanContextCarrier}）
 * 取 SpanContext 作为 parent，开 TOOL_CALL span、发 TOOL_INPUT/TOOL_OUTPUT（或 error）Event。
 *
 * <p><b>并发抗串味</b>：parent SpanContext 取自会话级 carrier 的快照值（由
 * {@code HarnessToolCallingManager} 把 carrier 写入 ToolContext）；fan-out 各任务读到的是同一
 * Turn/ModelCall 父 span，故同轮并发工具各开 ToolCall span 且 parent 均正确（spec 推演 4 载体）。
 */
public class ObservableToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final SpanRecorder recorder;
    private final ObservabilityAdvisor.ObservabilitySessionHooks hooks;
    private final SpanContextCarrier carrier;

    public ObservableToolCallback(ToolCallback delegate, SpanRecorder recorder,
                                  ObservabilityAdvisor.ObservabilitySessionHooks hooks,
                                  SpanContextCarrier carrier) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.hooks = hooks;
        this.carrier = carrier;
    }

    public ToolCallback delegate() {
        return delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, new ToolContext(Map.of()));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String toolName = getToolDefinition().name();
        SpanContext parent = resolveParent(toolContext);
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("tool.name", toolName);
        attrs.put("tool.type", "function");
        if (carrier != null) {
            attrs.put("tool.parallel.index", carrier.nextParallelIndex());
        }
        SpanHandle span = recorder.openSpan(SpanKind.TOOL_CALL, "tool:" + toolName, parent, attrs);
        Map<String, Object> inputPayload = new LinkedHashMap<>();
        inputPayload.put("tool.name", toolName);
        inputPayload.put("arguments", toolInput);
        recorder.emit(span.context(), EventType.TOOL_INPUT, inputPayload);

        String result;
        try {
            result = delegate.call(toolInput, toolContext);
        } catch (RuntimeException e) {
            span.error(e);
            span.close();
            throw e;
        }
        Map<String, Object> outputPayload = new LinkedHashMap<>();
        outputPayload.put("tool.name", toolName);
        outputPayload.put("result", result);
        recorder.emit(span.context(), EventType.TOOL_OUTPUT, outputPayload);
        span.close();
        return result;
    }

    private SpanContext resolveParent(ToolContext toolContext) {
        Object fromContext = toolContext == null ? null : toolContext.getContext().get(SpanContextCarrier.KEY);
        if (fromContext instanceof SpanContextCarrier c) {
            SpanContext snapshot = c.snapshotTurn();
            if (snapshot != null) {
                return snapshot;
            }
        }
        if (carrier != null) {
            SpanContext snapshot = carrier.snapshotTurn();
            if (snapshot != null) {
                return snapshot;
            }
        }
        // 兜底：会话 span
        return hooks == null ? null : hooks.sessionSpan();
    }
}
