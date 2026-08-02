package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
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
    }

    @Override
    public List<BuzhouMessage> process(String sessionId, List<BuzhouMessage> stored, int currentTurn) {
        List<BuzhouMessage> compacted = compactor
                .compact(stored, currentTurn, policyFn, protectRecentTurns)
                .compactedView();
        if (summaryModel == null) {
            return compacted;
        }

        NineSectionSummary previous = summaryBridge.loadLatest(sessionId).orElse(null);
        BudgetReport budget = evaluateBudget(compacted, previous);
        if (!budget.compactionNeeded()) {
            return injectSummaryOnly(compacted, previous, currentTurn);
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
                return compacted;
            }
        }
        return assembleWithSummary(recent, merged, currentTurn);
    }

    private BudgetReport evaluateBudget(List<BuzhouMessage> compacted, NineSectionSummary summary) {
        return budgetCalculator.evaluate(new BudgetInput(
                modelName, "", List.of(), "",
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
                                                  NineSectionSummary summary, int currentTurn) {
        if (summary == null) {
            return compacted;
        }
        int cutoffTurn = currentTurn - keepRecentTurns;
        List<BuzhouMessage> recent = compacted.stream()
                .filter(m -> m.turnSeq() > cutoffTurn)
                .toList();
        return assembleWithSummary(recent.isEmpty() ? compacted : recent, summary, currentTurn);
    }

    private List<BuzhouMessage> assembleWithSummary(List<BuzhouMessage> recent,
                                                    NineSectionSummary summary, int currentTurn) {
        if (summary == null) {
            return recent;
        }
        BuzhouMessage synthetic = new BuzhouMessage(
                UUID.randomUUID().toString(), "", currentTurn, 0, Role.SYSTEM,
                "<system-reminder>\n以下是早前对话的结构化摘要：\n" + summary.render()
                        + "\n</system-reminder>",
                List.of(), null, null, null, Map.of("summary", true), Instant.now());
        List<BuzhouMessage> result = new ArrayList<>();
        result.add(synthetic);
        result.addAll(recent);
        return result;
    }
}
