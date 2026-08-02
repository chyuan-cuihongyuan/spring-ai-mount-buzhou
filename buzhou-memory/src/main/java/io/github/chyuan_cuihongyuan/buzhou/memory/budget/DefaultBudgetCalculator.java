package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBudgetCalculator implements BudgetCalculator {

    private final ContextWindowResolver windowResolver;
    private final TokenEstimator estimator;
    private final Map<Integer, Integer> schemaTokensCache = new ConcurrentHashMap<>();

    public DefaultBudgetCalculator(ContextWindowResolver windowResolver, TokenEstimator estimator) {
        this.windowResolver = windowResolver;
        this.estimator = estimator;
    }

    @Override
    public BudgetReport evaluate(BudgetInput input) {
        int window = windowResolver.resolveWindow(input.modelName());
        int effective = window - input.reserveOutputTokens() - input.safetyBufferTokens();
        int fixedOverhead = estimator.estimate(input.systemPrompt())
                + toolSchemaTokens(input.toolCallbacks())
                + estimator.estimate(input.currentInput());
        int summaryTokens = input.currentSummary() == null ? 0
                : estimator.estimate(String.join("\n", input.currentSummary().sections().values()));
        int historyTokens = estimator.estimateMessages(input.historyAfterMicroCompaction());
        int total = fixedOverhead + summaryTokens + historyTokens;
        boolean needed = total > effective * input.threshold();
        return new BudgetReport(window, effective, fixedOverhead,
                Math.max(effective - fixedOverhead, 0), summaryTokens, historyTokens, total,
                input.threshold(), needed);
    }

    int toolSchemaTokens(List<ToolCallback> tools) {
        if (tools == null || tools.isEmpty()) {
            return 0;
        }
        int hash = tools.stream()
                .map(t -> t.getToolDefinition().name() + t.getToolDefinition().description()
                        + t.getToolDefinition().inputSchema())
                .sorted()
                .reduce("", String::concat)
                .hashCode();
        return schemaTokensCache.computeIfAbsent(hash, k -> tools.stream()
                .mapToInt(t -> estimator.estimate(t.getToolDefinition().name()
                        + t.getToolDefinition().description()
                        + t.getToolDefinition().inputSchema()))
                .sum());
    }

    int schemaCacheSize() {
        return schemaTokensCache.size();
    }
}
