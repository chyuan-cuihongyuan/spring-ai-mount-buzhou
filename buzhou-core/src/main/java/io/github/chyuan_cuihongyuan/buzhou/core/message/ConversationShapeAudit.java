package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话历史形态审计（spec 1425 / T2151 / impl 1078）——MLflow 数据画像 /
 * OpenAI conversation shape 分析思想：会话历史的<b>角色分布与结构异常</b>
 * 是上下文健康的第一画像——USER/ASSISTANT 比例漂移（模型独白式多轮）、
 * 连续同角色非 TOOL 消息（管线写坏 history）、空内容条目（写入异常）——
 * 都在恶化为上下文污染前有形态信号。
 *
 * <p>纯函数零状态：吃 {@link BuzhouMessage} 列表（调用方自选口径——单会话
 * 全史/压缩窗口均可）。异常口径显式：连续同角色按「相邻且角色相同且非
 * TOOL」计（TOOL 链内连续是并行工具调用的正常形态）；空内容 = content
 * null/空白且无 toolCalls（带工具调用的 ASSISTANT 空 content 是正常形态）。
 */
public final class ConversationShapeAudit {

    private ConversationShapeAudit() {
    }

    /**
     * @param totalMessages      消息总数
     * @param roleHistogram      角色直方（数量降序平名典序）
     * @param consecutiveSameRole 连续同角色非 TOOL 消息的相邻对数（history 写坏信号）
     * @param emptyContent       空内容且无工具调用的条目数
     * @param maxTurnGap         相邻消息 turnSeq 最大跳变（0 = 单调连续；>1 = 回放/乱序写入嫌疑）
     */
    public record ShapeReport(int totalMessages, Map<String, Integer> roleHistogram,
                              int consecutiveSameRole, int emptyContent, int maxTurnGap) {
    }

    /** 审计入口：按 history 顺序喂入（乱序输入的 turnGap 显形即其审计目的之一）。 */
    public static ShapeReport analyze(List<BuzhouMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ShapeReport(0, Map.of(), 0, 0, 0);
        }
        Map<String, Integer> histogram = new LinkedHashMap<>();
        int consecutive = 0;
        int empty = 0;
        int maxGap = 0;
        int prevTurn = Integer.MIN_VALUE;
        Role prevRole = null;
        for (BuzhouMessage m : messages) {
            String role = m.role() == null ? "UNKNOWN" : m.role().name();
            histogram.merge(role, 1, Integer::sum);
            if (prevRole != null && prevRole == m.role() && m.role() != Role.TOOL) {
                consecutive++;
            }
            boolean hasToolCalls = m.toolCalls() != null && !m.toolCalls().isEmpty();
            if ((m.content() == null || m.content().isBlank()) && !hasToolCalls) {
                empty++;
            }
            if (prevTurn != Integer.MIN_VALUE) {
                maxGap = Math.max(maxGap, Math.abs(m.turnSeq() - prevTurn));
            }
            prevTurn = m.turnSeq();
            prevRole = m.role();
        }
        List<Map.Entry<String, Integer>> sorted = histogram.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        Map<String, Integer> ordered = new LinkedHashMap<>();
        sorted.forEach(e -> ordered.put(e.getKey(), e.getValue()));
        return new ShapeReport(messages.size(),
                java.util.Collections.unmodifiableMap(ordered),
                consecutive, empty, maxGap);
    }
}
