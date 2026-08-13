package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryDegrader;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryFactReconciler;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

/**
 * {@code compact_now}：语义边界压缩触发工具（wayfinder T27 / docs/spec/11 memory，来源
 * LangChain Deep Agents）。模型可在任务边界/长草稿前<b>自触发压缩</b>——把未摘要的完成轮
 * 折入九段摘要；token 阈值兜底仍照常生效（<b>双触发路径</b>：质量自触发 + token 安全网）。
 *
 * <p>会话绑定：{@code HarnessToolCallingManager} 注入的 ToolContext 携带 sessionId；
 * 幂等安全：复用 {@code coversUpToTurn} 轮次水位与 {@code summarizedMessageIds} 消息水位
 * （T24），已摘要内容不重复折入。
 */
public class CompactNowTool implements ToolCallback {

    /** 对账/压缩后的目标摘要 token 上限（安全网降级用）。 */
    static final int SUMMARY_TOKEN_BUDGET = 2000;

    /** 4 字符/token 启发式估算器（与 core CharHeuristicTokenEstimator 一致；避免跨模块引 internal 实现）。 */
    private static final TokenEstimator HEURISTIC = new TokenEstimator() {
        @Override
        public int estimate(String text) {
            return text == null ? 0 : Math.max(1, text.length() / 4);
        }

        @Override
        public int estimateMessages(java.util.List<org.springframework.ai.chat.messages.Message> messages) {
            return messages == null ? 0 : messages.stream().mapToInt(m -> estimate(m.getText())).sum();
        }

        @Override
        public String name() {
            return "compact-now-heuristic";
        }
    };

    private final MessageStore messageStore;
    private final SummaryStoreBridge summaryBridge;
    private final SummaryGenerator summaryGenerator;
    private final ChatModel summaryModel;
    private final int keepRecentTurns;
    private final SummaryFactReconciler reconciler;
    private final BiTemporalFactLedger biTemporal;
    private final java.util.function.Consumer<SessionEvent> eventSink;

    public CompactNowTool(MessageStore messageStore, SummaryStoreBridge summaryBridge,
                          SummaryGenerator summaryGenerator, ChatModel summaryModel,
                          int keepRecentTurns) {
        this(messageStore, summaryBridge, summaryGenerator, summaryModel, keepRecentTurns,
                null, null, null);
    }

    public CompactNowTool(MessageStore messageStore, SummaryStoreBridge summaryBridge,
                          SummaryGenerator summaryGenerator, ChatModel summaryModel,
                          int keepRecentTurns, SummaryFactReconciler reconciler,
                          BiTemporalFactLedger biTemporal,
                          java.util.function.Consumer<SessionEvent> eventSink) {
        this.messageStore = messageStore;
        this.summaryBridge = summaryBridge;
        this.summaryGenerator = summaryGenerator;
        this.summaryModel = summaryModel;
        this.keepRecentTurns = Math.max(0, keepRecentTurns);
        this.reconciler = reconciler;
        this.biTemporal = biTemporal;
        this.eventSink = eventSink;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("compact_now")
                .description("在任务边界或长草稿前主动触发会话压缩：把已完成的早前轮次折入九段式结构化摘要，"
                        + "为后续推理腾出上下文预算（保 P0 关键事实，压缩发生在干净边界、保真度更高）。"
                        + "可选传 reason 说明触发时机。")
                .inputSchema("{\"type\":\"object\",\"properties\":{\"reason\":{\"type\":\"string\"}},"
                        + "\"additionalProperties\":false}")
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String sessionId = HarnessToolCallingManager.sessionIdOf(toolContext);
        if (sessionId == null || sessionId.isBlank()) {
            return "[compact_now] 当前未绑定会话，无法定位待压缩历史。";
        }
        List<BuzhouMessage> history = messageStore.load(sessionId);
        if (history.isEmpty()) {
            return "[compact_now] 会话历史为空，无需压缩。";
        }
        int currentTurn = history.stream().mapToInt(BuzhouMessage::turnSeq).max().orElse(1);
        NineSectionSummary previous = summaryBridge.loadLatest(sessionId).orElse(null);
        int alreadyCovered = previous == null ? 0 : previous.coversUpToTurn();
        List<String> summarizedIds = previous == null ? List.of() : previous.summarizedMessageIds();
        int cutoffTurn = currentTurn - keepRecentTurns;
        List<BuzhouMessage> toSummarize = history.stream()
                .filter(m -> m.turnSeq() <= cutoffTurn && m.turnSeq() > alreadyCovered)
                .filter(m -> !summarizedIds.contains(m.id()))
                .toList();
        if (toSummarize.isEmpty()) {
            return "[compact_now] 无需压缩：第 1–" + cutoffTurn + " 轮已全部折入摘要（当前摘要代际 "
                    + (previous == null ? 0 : previous.generation()) + "，覆盖至第 " + alreadyCovered + " 轮）。";
        }
        try {
            NineSectionSummary merged = summaryGenerator.merge(previous, toSummarize, cutoffTurn,
                    "任务边界主动压缩（compact_now）", summaryModel);
            merged = new DefaultSummaryDegrader().degradeToFit(merged, SUMMARY_TOKEN_BUDGET,
                    HEURISTIC);
            merged = merged.withSummarizedMessageIds(
                    NineSectionSummary.unionIds(summarizedIds,
                            toSummarize.stream().map(BuzhouMessage::id).toList()));
            if (reconciler != null) {
                // 与 token 兜底路径同一对账 pass（T25）：主动压缩不绕过去重/证伪
                merged = reconciler.reconcile(sessionId, previous, merged, summaryModel,
                        eventSink, biTemporal);
            }
            summaryBridge.save(sessionId, merged);
            return "[compact_now] 压缩完成：新折入 " + toSummarize.size() + " 条消息（第 "
                    + (alreadyCovered + 1) + "–" + cutoffTurn + " 轮），摘要代际 "
                    + merged.generation() + "，估算 " + (merged.render().length() / 4)
                    + " token。近期 " + keepRecentTurns + " 轮原文保持内联；下一轮注入视图将携带最新摘要。";
        } catch (RuntimeException e) {
            return "[compact_now] 压缩失败：" + e.getMessage()
                    + "。token 预算兜底压缩仍会在需要时自动触发。";
        }
    }
}
