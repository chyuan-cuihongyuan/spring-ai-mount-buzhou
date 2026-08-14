package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolErrorFeedback;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;
import java.util.UUID;

public class HookedToolCallback implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ToolCallback delegate;
    private final HookChain chain;
    private final HookEnvironment env;

    public HookedToolCallback(ToolCallback delegate, HookChain chain, HookEnvironment env) {
        this.delegate = delegate;
        this.chain = chain;
        this.env = env;
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
        DefaultToolCallContext ctx = new DefaultToolCallContext(
                env, UUID.randomUUID().toString(), toolName, parseArguments(toolInput));

        HookResult before = chain.beforeTool(ctx);
        if (before instanceof HookResult.Block block) {
            return block.reason();
        }

        String result;
        Throwable error = null;
        try {
            result = delegate.call(serializeArguments(ctx.arguments()), toolContext);
        } catch (RuntimeException e) {
            // 工具侧异常统一走「错误即反馈」通道：结构化错误文案（含原入参）回喂模型，Turn 不死。
            error = e;
            result = ToolErrorFeedback.format(toolName, toolInput,
                    "执行失败：" + e.getMessage());
        }
        ctx.markExecuted(result, error);
        // impl-41 / spec 13 §T66：工具调用指标（全部机制的工具都经本回调执行）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.tool.calls", "outcome", error == null ? "ok" : "failed");

        HookResult after = chain.afterTool(ctx);
        if (after instanceof HookResult.Block) {
            return result;
        }
        return String.valueOf(ctx.result());
    }

    private Map<String, Object> parseArguments(String toolInput) {
        try {
            return MAPPER.readValue(toolInput, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of("input", toolInput);
        }
    }

    private String serializeArguments(Map<String, Object> arguments) {
        try {
            return MAPPER.writeValueAsString(arguments);
        } catch (Exception e) {
            return "{}";
        }
    }
}
