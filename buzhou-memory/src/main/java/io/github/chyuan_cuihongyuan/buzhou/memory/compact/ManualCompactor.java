package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryDegrader;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryFactReconciler;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

/**
 * 宿主侧手动压缩与摘要导出（spec 20 / T90 / impl-65）：与模型侧 {@code CompactNowTool}
 * 共用同一条压缩管线（工具委托本类）。
 *
 * <p><b>幂等</b>：alreadyCovered / summarizedMessageIds 跳过已折入消息；无待折返回
 * {@code skipped} 结果。<b>并发边界</b>：无锁——SummaryStore 版本化追加 + 幂等集与在途轮的
 * messageStore 追加天然并发安全；建议轮间隙调用（轮中调用安全但摘要可能少折最后一轮）。
 */
public final class ManualCompactor {

    /** 对账/压缩后的目标摘要 token 上限（安全网降级用；与既有 compact_now 同值）。 */
    static final int SUMMARY_TOKEN_BUDGET = 2000;

    /** 4 字符/token 启发式估算器（与 core CharHeuristicTokenEstimator 一致口径）。 */
    private static final TokenEstimator HEURISTIC = new TokenEstimator() {
        @Override
        public int estimate(String text) {
            return text == null ? 0 : Math.max(1, text.length() / 4);
        }

        @Override
        public int estimateMessages(List<org.springframework.ai.chat.messages.Message> messages) {
            return messages == null ? 0 : messages.stream().mapToInt(m -> estimate(m.getText())).sum();
        }

        @Override
        public String name() {
            return "manual-compactor-heuristic";
        }
    };

    private final MessageStore messageStore;
    private final SummaryStoreBridge summaryBridge;
    private final SummaryGenerator summaryGenerator;
    private final ChatModel summaryModel;
    private final int keepRecentTurns;
    private final SummaryFactReconciler reconciler;
    private final BiTemporalFactLedger biTemporal;
    private final java.util.function.Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> eventSink;

    public ManualCompactor(MessageStore messageStore, SummaryStoreBridge summaryBridge,
                           SummaryGenerator summaryGenerator, ChatModel summaryModel,
                           int keepRecentTurns) {
        this(messageStore, summaryBridge, summaryGenerator, summaryModel, keepRecentTurns,
                null, null, null);
    }

    public ManualCompactor(MessageStore messageStore, SummaryStoreBridge summaryBridge,
                           SummaryGenerator summaryGenerator, ChatModel summaryModel,
                           int keepRecentTurns, SummaryFactReconciler reconciler,
                           BiTemporalFactLedger biTemporal,
                           java.util.function.Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> eventSink) {
        this.messageStore = messageStore;
        this.summaryBridge = summaryBridge;
        this.summaryGenerator = summaryGenerator;
        this.summaryModel = summaryModel;
        this.keepRecentTurns = Math.max(0, keepRecentTurns);
        this.reconciler = reconciler;
        this.biTemporal = biTemporal;
        this.eventSink = eventSink;
    }

    /**
     * 手动压缩结果：skipped=true 表示无需压缩（无历史/已全折入）；error 非 null 表示压缩失败
     * （token 预算兜底自动压缩仍会在需要时触发）。
     */
    public record CompactResult(boolean skipped, int foldedMessages, int fromTurn, int toTurn,
                                int generation, int estimatedTokens, String error) {

        static CompactResult skip() {
            return new CompactResult(true, 0, 0, 0, 0, 0, null);
        }

        static CompactResult failed(String error) {
            return new CompactResult(false, 0, 0, 0, 0, 0, error);
        }
    }

    /** 手动压缩：把已完成早前轮次折入九段式结构化摘要（管线与 compact_now 完全一致）。 */
    public CompactResult compact(String sessionId) {
        List<BuzhouMessage> history = messageStore.load(sessionId);
        if (history.isEmpty()) {
            return CompactResult.skip();
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
            return CompactResult.skip();
        }
        try {
            NineSectionSummary merged = summaryGenerator.merge(previous, toSummarize, cutoffTurn,
                    "宿主侧手动压缩（ManualCompactor）", summaryModel);
            merged = new DefaultSummaryDegrader().degradeToFit(merged, SUMMARY_TOKEN_BUDGET, HEURISTIC);
            merged = merged.withSummarizedMessageIds(
                    NineSectionSummary.unionIds(summarizedIds,
                            toSummarize.stream().map(BuzhouMessage::id).toList()));
            if (reconciler != null) {
                merged = reconciler.reconcile(sessionId, previous, merged, summaryModel,
                        eventSink, biTemporal);
            }
            summaryBridge.save(sessionId, merged);
            return new CompactResult(false, toSummarize.size(), alreadyCovered + 1, cutoffTurn,
                    (int) merged.generation(), merged.render().length() / 4, null);
        } catch (RuntimeException e) {
            return CompactResult.failed(e.getMessage());
        }
    }

    /** 类型化导出：当前最新九段式摘要（无摘要 = empty）。 */
    public Optional<NineSectionSummary> exportSummary(String sessionId) {
        return summaryBridge.loadLatest(sessionId);
    }

    /** Markdown 导出：最新摘要的渲染文本（无摘要 = empty）。 */
    public Optional<String> exportSummaryMarkdown(String sessionId) {
        return exportSummary(sessionId).map(NineSectionSummary::render);
    }
}
