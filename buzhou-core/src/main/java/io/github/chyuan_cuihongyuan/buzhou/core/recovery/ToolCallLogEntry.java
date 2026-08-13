package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * 事件溯源工具调用日志条目（wayfinder2 impl-07 / T33 / docs/spec/12）：
 * append-only 记录 (toolCallId, argsHash, outcome, result)——崩溃恢复时
 * 已落盘 outcome 的调用<b>按 id 短路不重跑</b>（exactly-once 的证据层）。
 *
 * @param sessionId 会话 id
 * @param toolCallId 工具调用 id（悬空修复的回放键）
 * @param toolName  工具名
 * @param argsHash  入参指纹（sha256(args)）——幂等键的请求侧证据
 * @param outcome   结局
 * @param result    结果内容（回放用；记录时已封顶，避免日志无限膨胀）
 * @param occurredAt 发生时间
 */
public record ToolCallLogEntry(
        String sessionId,
        String toolCallId,
        String toolName,
        String argsHash,
        ToolCallOutcome outcome,
        String result,
        Instant occurredAt) {

    /** 结果记录封顶（字符）：足够回放有用信息，又不让日志成为第二存储。 */
    public static final int RESULT_CAP_CHARS = 64_000;

    public ToolCallLogEntry {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        result = result == null ? "" : result;
        if (result.length() > RESULT_CAP_CHARS) {
            result = result.substring(0, RESULT_CAP_CHARS)
                    + "\n[事件日志记录已封顶，完整原文以持久层为准]";
        }
    }

    /** 入参指纹：sha256 hex（空入参按 {} 计）。 */
    public static String argsHash(String arguments) {
        String normalized = arguments == null || arguments.isBlank() ? "{}" : arguments.strip();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // SHA-256 恒可用；防御分支按内容哈希退化为恒等串
            return "unhashed:" + normalized.hashCode();
        }
    }

    /** 幂等键（随调用传给工具端做下游去重；sessionId+turnId 语义由调用方拼装）。 */
    public String idempotencyKey() {
        return sessionId + ":" + toolCallId;
    }
}
