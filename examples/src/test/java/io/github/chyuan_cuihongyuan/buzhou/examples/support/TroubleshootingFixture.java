package io.github.chyuan_cuihongyuan.buzhou.examples.support;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 排障 Agent 共享夹具（ticket 21）：四簇 demo 与评测复用。
 *
 * <p>提供排障场景的预埋要点、20+ 轮混合大小工具返回历史构造、九段摘要文本（summaryModel 返回值）、
 * 小窗口 yml（触发压缩）与字符启发式 token 估算。所有 demo/评测基于公共可观察面，不触达 internal。
 */
public final class TroubleshootingFixture {

    /** 预埋要点（P0 锚定与事实召回断言用）。 */
    public static final String ORDER_ID = "ORD-2026-7";
    public static final String ERROR_CODE = "ERR_TIMEOUT_5421";
    public static final String PAYMENT_REF = "PAY-77";
    public static final String USER_INTENT = "排查订单 " + ORDER_ID + " 支付失败";

    /** summaryModel 固定返回的九段摘要（含 P0 三段预埋要点，验证压缩后 P0 锚定）。 */
    public static final String NINE_SECTIONS = """
            ## USER_INTENT
            """ + USER_INTENT + """
            ## CURRENT_STATE
            已定位到支付网关超时（错误码 """ + ERROR_CODE + "）\n" + """
            ## NEXT_STEP
            检查网关回调与重试策略
            ## PENDING_TASKS
            联系网关方确认超时原因
            ## ERRORS_FIXES
            网关层超时，错误码 """ + ERROR_CODE + "，流水号 " + PAYMENT_REF + "\n" + """
            ## KEY_ARTIFACTS
            流水号 """ + PAYMENT_REF + "\n" + """
            ## PROBLEM_SOLVING
            逐步定位到网关层
            ## TECHNICAL_CONCEPTS
            支付状态机、网关重试
            ## USER_MESSAGES_LOG
            多次追问订单状态
            """;

    private TroubleshootingFixture() {
    }

    /**
     * 构造排障场景历史：每轮 = 用户排查提问（含意图 + 大详情）→ assistant 调 query_logs →
     * tool 大日志返回（含事实探针，触发微压缩）→ assistant 小结。规模 20+ 轮混合大小返回。
     */
    public static List<BuzhouMessage> troubleshootingHistory(String sessionId, int turns) {
        List<BuzhouMessage> history = new ArrayList<>();
        for (int turn = 1; turn <= turns; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER,
                    "排查订单 " + ORDER_ID + " 第" + turn + "步：" + "详情字段".repeat(120),
                    List.of(), null, Map.of()));
            history.add(msg(sessionId, turn, 1, Role.ASSISTANT, "",
                    List.of(new ToolCallRecord("tc-" + turn, "query_logs", "{}")),
                    null, Map.of()));
            history.add(msg(sessionId, turn, 2, Role.TOOL,
                    "[日志] 订单 " + ORDER_ID + " 错误码 " + ERROR_CODE + " 流水 " + PAYMENT_REF
                            + " " + "查询行数据".repeat(160),
                    List.of(), "tc-" + turn, Map.of("toolName", "query_logs")));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT,
                    "第" + turn + "步结论：网关层超时", List.of(), null, Map.of()));
        }
        return history;
    }

    /** 小窗口 yml：压低上下文窗口触发摘要、缩 keep-recent-turns 让旧轮尽早微压缩。 */
    public static Map<String, Object> smallWindowYml() {
        return Map.of("model-name", "demo-model",
                "memory", Map.of("context-window", Map.of("demo-model", 12000),
                        "keep-recent-turns", 2));
    }

    /** 字符启发式 token 估算（对齐 core CharHeuristicTokenEstimator 语义，评测压缩率用）。 */
    public static int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }

    /** 固定返回结果的工具（demo 的 query_logs / 占位工具），最小 ToolCallback 实现。 */
    public static ToolCallback fixedTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    private static BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content,
                                     List<ToolCallRecord> toolCalls, String toolCallId,
                                     Map<String, Object> attributes) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role, content,
                toolCalls, toolCallId, null, null, attributes, Instant.now());
    }
}
