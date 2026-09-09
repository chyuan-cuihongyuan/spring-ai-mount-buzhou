package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 工具退役通告装饰器（spec 406 / T703，K8s API deprecation 借鉴——通告随
 * 定义、退役≠移除）：描述前缀模型可见 steering（不注入提示词）；每次调用
 * 发事件 + 计数（迁移进度面——计数衰减 = 真删除时机由数据说话）；不阻断
 * （阻断域归 325 kill switch）。定义透传除描述外（装饰器家族同款）。
 */
public final class DeprecatedToolCallback implements ToolCallback {

    /** 退役工具被调用事件（tool/successor/since）。 */
    public static final String EVENT_DEPRECATED_CALLED = "tool.deprecated-called";

    /** 退役声明（yml 形态的运行时面）。 */
    public record Deprecation(String since, String removalIn, String successor,
            String message) {

        public Deprecation {
            since = since == null || since.isBlank() ? "unknown" : since;
        }

        /** 描述前缀（模型可见——K8s 同法随定义下发）。 */
        public String descriptionPrefix() {
            StringBuilder sb = new StringBuilder("[DEPRECATED since ").append(since);
            if (removalIn != null && !removalIn.isBlank()) {
                sb.append(", removal ").append(removalIn);
            }
            sb.append("] ");
            if (message != null && !message.isBlank()) {
                sb.append(message).append(' ');
            }
            if (successor != null && !successor.isBlank()) {
                sb.append("优先使用 ").append(successor).append('。');
            }
            return sb.toString();
        }
    }

    private final ToolCallback delegate;
    private final Deprecation deprecation;
    private final Consumer<SessionEvent> emitter;

    public DeprecatedToolCallback(ToolCallback delegate, Deprecation deprecation,
            Consumer<SessionEvent> emitter) {
        this.delegate = delegate;
        this.deprecation = deprecation;
        this.emitter = emitter == null ? e -> { } : emitter;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition def = delegate.getToolDefinition();
        return DefaultToolDefinition.builder()
                .name(def.name())
                .description(deprecation.descriptionPrefix() + def.description())
                .inputSchema(def.inputSchema())
                .build();
    }

    @Override
    public String call(String toolInput) {
        recordCall();
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        recordCall();
        return delegate.call(toolInput, toolContext);
    }

    private void recordCall() {
        String tool = delegate.getToolDefinition().name();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", tool);
        payload.put("since", deprecation.since());
        if (deprecation.successor() != null && !deprecation.successor().isBlank()) {
            payload.put("successor", deprecation.successor());
        }
        emitter.accept(SessionEvent.of(EVENT_DEPRECATED_CALLED, payload));
        BuzhouMetricsHolder.metrics().counter("buzhou.tools.deprecated-calls", "tool", tool);
    }

    /** 退役声明只读面（装配/测试）。 */
    public Deprecation deprecation() {
        return deprecation;
    }

    @SuppressWarnings("unused")
    private static Instant now() {
        return Instant.now();
    }
}
