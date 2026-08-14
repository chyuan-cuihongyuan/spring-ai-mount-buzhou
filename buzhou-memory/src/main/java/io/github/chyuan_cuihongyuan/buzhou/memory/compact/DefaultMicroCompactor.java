package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class DefaultMicroCompactor implements MicroCompactor {

    private final CompletedTurnDetector detector;

    public DefaultMicroCompactor(CompletedTurnDetector detector) {
        this.detector = detector;
    }

    @Override
    public MicroCompactionResult compact(List<BuzhouMessage> history,
                                         int currentTurnIndex,
                                         Function<String, MicroCompactionPolicy> policyByToolName,
                                         int protectRecentTurns,
                                         double evictRatio) {
        // impl-41 / spec 13 §T66：压缩指标（outcome=ok|failed）
        try {
            return compactInternal(history, currentTurnIndex, policyByToolName,
                    protectRecentTurns, evictRatio);
        } catch (RuntimeException e) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.compaction", "outcome", "failed");
            throw e;
        }
    }

    private MicroCompactionResult compactInternal(List<BuzhouMessage> history,
                                         int currentTurnIndex,
                                         Function<String, MicroCompactionPolicy> policyByToolName,
                                         int protectRecentTurns,
                                         double evictRatio) {
        Set<Integer> completedTurns = new HashSet<>();
        for (TurnSpan span : detector.detectTurns(history)) {
            if (span.completed()) {
                completedTurns.add(history.get(span.startMessageOffset()).turnSeq());
            }
        }

        // 第一遍：识别可回收候选（判定语义与既有完全一致）
        List<BuzhouMessage> candidates = new ArrayList<>();
        for (BuzhouMessage message : history) {
            if (isReclaimable(message, currentTurnIndex, completedTurns,
                    policyByToolName, protectRecentTurns)) {
                candidates.add(message);
            }
        }

        // impl-02 / T36：部分逐出保连续——只逐出最旧的 ceil(n×ratio)，最新 (1-ratio) 原文留内联
        // 续接（Letta：一次压到底有断崖风险；ratio 由注入视图按预算 10% 步进梯子加压）
        int evictCount = candidates.isEmpty()
                ? 0
                : (int) Math.ceil(candidates.size() * clampRatio(evictRatio));
        Set<String> evictIds = new HashSet<>();
        for (int i = 0; i < evictCount; i++) {
            evictIds.add(candidates.get(i).id());
        }

        // 第二遍：构建视图
        List<BuzhouMessage> view = new ArrayList<>(history.size());
        List<String> compactedIds = new ArrayList<>();
        int reclaimed = 0;
        for (BuzhouMessage message : history) {
            if (evictIds.contains(message.id())) {
                reclaimed += message.content() == null ? 0 : message.content().length();
                compactedIds.add(message.id());
                view.add(placeholder(message));
            } else {
                view.add(message);
            }
        }
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.compaction", "outcome", "ok");
        return new MicroCompactionResult(view, compactedIds, reclaimed);
    }

    private static double clampRatio(double evictRatio) {
        if (Double.isNaN(evictRatio) || evictRatio <= 0.0d) {
            return 0.0d;
        }
        return Math.min(evictRatio, 1.0d);
    }

    private boolean isReclaimable(BuzhouMessage message,
                                  int currentTurnIndex,
                                  Set<Integer> completedTurns,
                                  Function<String, MicroCompactionPolicy> policyByToolName,
                                  int protectRecentTurns) {
        if (message.role() != Role.TOOL) {
            return false;
        }
        if (!completedTurns.contains(message.turnSeq())) {
            return false;
        }
        if (message.turnSeq() > currentTurnIndex - protectRecentTurns) {
            return false;
        }
        String toolName = (String) message.metadata().getOrDefault("toolName", "");
        MicroCompactionPolicy policy = policyByToolName.apply(toolName);
        if (policy.neverCompress()) {
            return false;
        }
        if (currentTurnIndex - message.turnSeq() <= policy.maxAgeTurns()) {
            return false;
        }
        return message.content() != null && message.content().length() >= policy.minSizeChars();
    }

    private BuzhouMessage placeholder(BuzhouMessage original) {
        String text = "[旧工具结果已清理，可按 evidence-id=" + original.id() + " 回查]";
        return new BuzhouMessage(original.id(), original.sessionId(), original.turnSeq(),
                original.seqInTurn(), original.role(), text, original.toolCalls(),
                original.toolCallId(), original.reasoningContent(), original.reasoningSignature(),
                original.metadata(), original.createdAt());
    }
}
