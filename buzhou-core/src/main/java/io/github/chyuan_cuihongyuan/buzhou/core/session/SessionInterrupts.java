package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会话中断/恢复（wayfinder2 impl-08 / T34 / docs/spec/12 §core-6，LangGraph
 * interrupt/Command(resume) 的<b>规避反模式</b>版）：pending 记录经 hook 维护
 * （beforeTool 登记、afterTool 完成清除）；{@link #resumeWith} 按 <b>toolCallId 精确注入</b>
 * 对应 ToolResponse（直接落库——<b>绝不重放 Turn 前段</b>：无 Turn 重跑、无节点重执行；
 * 多挂起可逐个 resume）。已应答/未知 id 的 resume 为无操作（幂等）。
 */
public final class SessionInterrupts {

    /** 挂起中的工具调用（内存态；持久层真相以消息应答为准——load 时推导）。 */
    public record PendingToolCall(String toolCallId, String toolName, int turn) {
    }

    private SessionInterrupts() {
    }

    /** 从持久历史推导未应答（挂起）的工具调用。 */
    public static List<PendingToolCall> pending(MessageStore messageStore, String sessionId) {
        List<BuzhouMessage> history = messageStore.load(sessionId);
        var responded = history.stream()
                .filter(m -> m.role() == Role.TOOL && m.toolCallId() != null)
                .map(BuzhouMessage::toolCallId)
                .collect(Collectors.toSet());
        return history.stream()
                .filter(m -> m.role() == Role.ASSISTANT && m.toolCalls() != null)
                .flatMap(m -> m.toolCalls().stream()
                        .filter(tc -> !responded.contains(tc.id()))
                        .map(tc -> new PendingToolCall(tc.id(), tc.name(), m.turnSeq())))
                .toList();
    }

    /**
     * 恢复：按 toolCallId 精确注入 ToolResponse（人审结果/重执行结果直接落库，
     * 下一轮模型即见——不重放 Turn 前段）。已应答或未知 id → false（幂等无操作）。
     */
    public static boolean resumeWith(MessageStore messageStore, String sessionId,
                                     String toolCallId, String resultText) {
        var dangling = pending(messageStore, sessionId).stream()
                .filter(p -> p.toolCallId().equals(toolCallId))
                .findFirst();
        if (dangling.isEmpty()) {
            return false;
        }
        messageStore.append(sessionId, List.of(new BuzhouMessage(
                UUID.randomUUID().toString(), sessionId, dangling.get().turn(),
                Integer.MAX_VALUE, Role.TOOL,
                resultText == null ? "" : resultText, List.of(), toolCallId, null, null,
                Map.of("toolName", dangling.get().toolName(), "resumed", true), Instant.now())));
        return true;
    }
}
