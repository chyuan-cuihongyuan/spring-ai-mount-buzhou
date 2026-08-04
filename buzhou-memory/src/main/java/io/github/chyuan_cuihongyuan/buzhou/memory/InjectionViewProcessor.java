package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetInput;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetReport;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryDegrader;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class InjectionViewProcessor implements MemoryViewProcessor {

    private final DefaultMicroCompactor compactor;
    private final Function<String, MicroCompactionPolicy> policyFn;
    private final int protectRecentTurns;
    private final DefaultBudgetCalculator budgetCalculator;
    private final SummaryStoreBridge summaryBridge;
    private final SummaryGenerator summaryGenerator;
    private final SummaryCircuitBreaker breaker;
    private final ChatModel summaryModel;
    private final String modelName;
    private final int keepRecentTurns;
    private final String extraInstruction;
    private final int maxInjectChars;
    private AttachmentRenderer attachmentRenderer;

    public InjectionViewProcessor(DefaultMicroCompactor compactor,
                                  Function<String, MicroCompactionPolicy> policyFn,
                                  int protectRecentTurns,
                                  DefaultBudgetCalculator budgetCalculator,
                                  SummaryStoreBridge summaryBridge,
                                  SummaryGenerator summaryGenerator,
                                  SummaryCircuitBreaker breaker,
                                  ChatModel summaryModel,
                                  String modelName,
                                  int keepRecentTurns,
                                  String extraInstruction) {
        this(compactor, policyFn, protectRecentTurns, budgetCalculator, summaryBridge,
                summaryGenerator, breaker, summaryModel, modelName, keepRecentTurns,
                extraInstruction, 4000);
    }

    public InjectionViewProcessor(DefaultMicroCompactor compactor,
                                  Function<String, MicroCompactionPolicy> policyFn,
                                  int protectRecentTurns,
                                  DefaultBudgetCalculator budgetCalculator,
                                  SummaryStoreBridge summaryBridge,
                                  SummaryGenerator summaryGenerator,
                                  SummaryCircuitBreaker breaker,
                                  ChatModel summaryModel,
                                  String modelName,
                                  int keepRecentTurns,
                                  String extraInstruction,
                                  int maxInjectChars) {
        this.compactor = compactor;
        this.policyFn = policyFn;
        this.protectRecentTurns = protectRecentTurns;
        this.budgetCalculator = budgetCalculator;
        this.summaryBridge = summaryBridge;
        this.summaryGenerator = summaryGenerator;
        this.breaker = breaker;
        this.summaryModel = summaryModel;
        this.modelName = modelName;
        this.keepRecentTurns = keepRecentTurns;
        this.extraInstruction = extraInstruction;
        this.maxInjectChars = maxInjectChars;
    }

    /** 注入事实 Attachment 渲染器（spec 07 Hook→state→Attachment 闭环）。 */
    public void setAttachmentRenderer(AttachmentRenderer attachmentRenderer) {
        this.attachmentRenderer = attachmentRenderer;
    }

    @Override
    public List<BuzhouMessage> process(String sessionId, List<BuzhouMessage> stored, int currentTurn) {
        List<BuzhouMessage> compacted = compactor
                .compact(stored, currentTurn, policyFn, protectRecentTurns)
                .compactedView();
        // 先渲染事实块（maxInjectChars 截断 + 指针），供预算入账与注入共用（spec 07：先渲染后评估；
        // system-reminder 块与摘要 Current State 追加两通道共享同一文本，不重复超额）
        String factsBlock = renderFacts(sessionId, currentTurn);
        if (summaryModel == null) {
            // 无摘要模型时事实仍需注入（事实闭环不依赖摘要链路）
            return factsBlock == null ? compacted
                    : assembleWithSummary(compacted, null, factsBlock, currentTurn, sessionId);
        }

        NineSectionSummary previous = summaryBridge.loadLatest(sessionId).orElse(null);
        BudgetReport budget = evaluateBudget(compacted, previous, factsBlock);
        if (!budget.compactionNeeded()) {
            return injectSummaryOnly(compacted, previous, factsBlock, currentTurn, sessionId);
        }

        int cutoffTurn = currentTurn - keepRecentTurns;
        int alreadyCovered = previous == null ? 0 : previous.coversUpToTurn();
        List<BuzhouMessage> toSummarize = compacted.stream()
                .filter(m -> m.turnSeq() <= cutoffTurn && m.turnSeq() > alreadyCovered)
                .toList();
        List<BuzhouMessage> recent = compacted.stream()
                .filter(m -> m.turnSeq() > cutoffTurn)
                .toList();

        NineSectionSummary merged = previous;
        if (!toSummarize.isEmpty() && breaker.allows(sessionId)) {
            try {
                merged = summaryGenerator.merge(previous, toSummarize, cutoffTurn,
                        extraInstruction, summaryModel);
                merged = new DefaultSummaryDegrader().degradeToFit(merged,
                        Math.max(budget.historyBudget(), 1000),
                        new CharHeuristicTokenEstimator());
                summaryBridge.save(sessionId, merged);
                breaker.onSuccess(sessionId);
            } catch (RuntimeException e) {
                breaker.onFailure(sessionId);
                return factsBlock == null ? compacted
                        : assembleWithSummary(compacted, null, factsBlock, currentTurn, sessionId);
            }
        }
        return assembleWithSummary(recent, merged, factsBlock, currentTurn, sessionId);
    }

    /** 渲染未过期事实为注入文本（含截断与指针）；无渲染器/无事实/无 sessionId 时返回 null。 */
    private String renderFacts(String sessionId, int currentTurn) {
        if (attachmentRenderer == null || sessionId == null) {
            return null;
        }
        return attachmentRenderer.render(sessionId, currentTurn, maxInjectChars).orElse(null);
    }

    private BudgetReport evaluateBudget(List<BuzhouMessage> compacted, NineSectionSummary summary,
                                        String factsBlock) {
        // 事实注入 token 计「系统提示词一侧」固定扣除（spec 07：不挤历史预算）
        return budgetCalculator.evaluate(new BudgetInput(
                modelName, factsBlock == null ? "" : factsBlock, List.of(), "",
                summary == null ? null : summaryBridgeSnapshot(summary),
                compacted.stream().map(m -> (org.springframework.ai.chat.messages.Message)
                        new org.springframework.ai.chat.messages.UserMessage(
                                m.content() == null ? "" : m.content())).toList(),
                8000, 3000, 0.90));
    }

    private io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary summaryBridgeSnapshot(
            NineSectionSummary summary) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary(
                "", summary.generation(), Map.of("render", summary.render()), 0, Instant.now());
    }

    private List<BuzhouMessage> injectSummaryOnly(List<BuzhouMessage> compacted,
                                                  NineSectionSummary summary, String factsBlock,
                                                  int currentTurn, String sessionId) {
        // 无摘要且无事实块 → 直接返回（无需注入）
        if (summary == null && factsBlock == null) {
            return compacted;
        }
        int cutoffTurn = currentTurn - keepRecentTurns;
        List<BuzhouMessage> recent = compacted.stream()
                .filter(m -> m.turnSeq() > cutoffTurn)
                .toList();
        return assembleWithSummary(recent.isEmpty() ? compacted : recent, summary, factsBlock,
                currentTurn, sessionId);
    }

    private List<BuzhouMessage> assembleWithSummary(List<BuzhouMessage> recent,
                                                    NineSectionSummary summary, String factsBlock,
                                                    int currentTurn, String sessionId) {
        List<BuzhouMessage> result = new ArrayList<>();
        if (summary != null) {
            // 把未过期事实追加到 CURRENT_STATE 段（P0 死保，压缩不丢现场）
            NineSectionSummary enriched = enrichWithFacts(summary, factsBlock);
            BuzhouMessage synthetic = new BuzhouMessage(
                    UUID.randomUUID().toString(), "", currentTurn, 0, Role.SYSTEM,
                    "<system-reminder>\n以下是早前对话的结构化摘要：\n" + enriched.render()
                            + "\n</system-reminder>",
                    List.of(), null, null, null, Map.of("summary", true), Instant.now());
            result.add(synthetic);
        }
        // 事实 Attachment 块（spec 07：摘要块在前、事实块随后、近期原文在后）
        if (factsBlock != null) {
            BuzhouMessage factBlock = new BuzhouMessage(
                    UUID.randomUUID().toString(), "", currentTurn, 0, Role.SYSTEM,
                    "<system-reminder>\n" + factsBlock + "\n</system-reminder>",
                    List.of(), null, null, null, Map.of("facts", true), Instant.now());
            result.add(factBlock);
        }
        if (result.isEmpty()) {
            return recent;
        }
        result.addAll(recent);
        return result;
    }

    /** 把未过期事实追加到摘要 CURRENT_STATE 段（保证压缩后事实仍保留，P0 不丢）。 */
    private NineSectionSummary enrichWithFacts(NineSectionSummary summary, String factsBlock) {
        if (factsBlock == null) {
            return summary;
        }
        return summary.appendCurrentState("\n[已采集事实]\n" + factsBlock);
    }
}
