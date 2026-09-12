package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具结果 schema 校验 hook（spec 409 / T709，MCP outputSchema 借鉴）：
 * afterTool——per-tool 结果 schema 声明即校验（<b>复用
 * {@link ToolArgsValidator} 同一校验器</b>——最小子集/未知关键字忽略/
 * schema 不具结构放行，零新校验逻辑）；违例 replaceResult 为结构化标记
 * 反馈（复用 {@code ToolFeedbackType.VALIDATION_FAILURE} 词汇档——校验
 * 档观测/预算语义不变，文案区隔「结果已执行」）；事件 + 计数；通过零
 * 改写（引用等）。模型收到反馈可换参重调或弃用（REASK 同语义，受 Turn
 * 预算约束）。
 */
public final class ToolResultSchemaHook implements BuzhouHook {

    /** 结果违例事件（tool + reason）。 */
    public static final String EVENT_VIOLATED = "tool.result-schema.violated";

    /** 结果校验位：PII 脱敏(70) 之前——先验原始契约再谈脱敏。 */
    public static final int ORDER = 65;

    private final Map<String, String> schemasByTool;

    public ToolResultSchemaHook(Map<String, String> schemasByTool) {
        this.schemasByTool = Map.copyOf(schemasByTool == null ? Map.of() : schemasByTool);
    }

    /** 已声明 schema 数（装配审计读数——spec 531）。 */
    public int schemasCount() {
        return schemasByTool.size();
    }

    @Override
    public String name() {
        return "ToolResultSchemaHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx.error() != null || ctx.result() == null) {
            return HookResult.CONTINUE; // 错误路径不校验
        }
        String schema = schemasByTool.get(ctx.toolName());
        if (schema == null || schema.isBlank()) {
            return HookResult.CONTINUE; // 未声明零变化
        }
        String result = String.valueOf(ctx.result());
        java.util.Optional<String> violation = ToolArgsValidator.validate(schema, result);
        if (violation.isEmpty()) {
            return HookResult.CONTINUE; // 通过零改写
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", ctx.toolName());
        payload.put("reason", violation.get());
        ctx.emitEvent(SessionEvent.of(EVENT_VIOLATED, payload));
        BuzhouMetricsHolder.metrics().counter("buzhou.tools.result-schema-violations",
                "tool", ctx.toolName());
        ctx.replaceResult(feedback(ctx.toolName(), result, violation.get()));
        return HookResult.CONTINUE;
    }

    /** 结构化反馈（标记词汇=校验档；文案区隔「结果已执行」vs 入参档「未执行」）。 */
    static String feedback(String toolName, String result, String reason) {
        String excerpt = result.length() > 300 ? result.substring(0, 300) + "…" : result;
        return ToolValidationFeedback.MARKER + "（结果未过 schema，工具已执行）\n"
                + "工具：" + toolName + "\n"
                + "结果（截断）：" + excerpt + "\n"
                + "原因：" + reason + "\n"
                + "建议：该工具本次输出不符合契约——请换参数重试，或改用其他工具/自行说明数据不可得；请勿基于上述残缺结果继续推理。";
    }
}
