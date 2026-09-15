package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/** spec 1529 / T2309：分层预算计算默认实现——层级容量分配与压缩触发的判定。 */

public class DefaultBudgetCalculator implements BudgetCalculator {

    private final ContextWindowResolver windowResolver;
    private final TokenEstimator estimator;
    private final Map<Integer, Integer> schemaTokensCache = new ConcurrentHashMap<>();

    public DefaultBudgetCalculator(ContextWindowResolver windowResolver, TokenEstimator estimator) {
        this.windowResolver = windowResolver;
        this.estimator = estimator;
    }

    // —— spec 1133 / impl 871：钳位读面（DSP clip 检测思想；静态面理由同 R46–R113
    // 先例）。守恒：evaluations = negativeClamps + normalBudgets。
    private static final java.util.concurrent.atomic.AtomicLong EVALUATIONS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong NEGATIVE_CLAMPS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong NORMAL_BUDGETS =
            new java.util.concurrent.atomic.AtomicLong();
    // —— spec 1134 / impl 872：needed 判定分布（压缩触发压力信号）。
    private static final java.util.concurrent.atomic.AtomicLong NEEDED_TRUE =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong NEEDED_FALSE =
            new java.util.concurrent.atomic.AtomicLong();

    /** 预算钳位分布快照（spec 1133）。 */
    public record BudgetClampStats(long evaluations, long negativeClamps, long normalBudgets,
                                   long neededTrue, long neededFalse) {
    }

    /** 只读快照（守恒 evaluations = 两桶之和）。 */
    public static BudgetClampStats stats() {
        return new BudgetClampStats(EVALUATIONS.get(), NEGATIVE_CLAMPS.get(), NORMAL_BUDGETS.get(),
                NEEDED_TRUE.get(), NEEDED_FALSE.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        EVALUATIONS.set(0);
        NEGATIVE_CLAMPS.set(0);
        NORMAL_BUDGETS.set(0);
        NEEDED_TRUE.set(0);
        NEEDED_FALSE.set(0);
    }

    @Override
    public BudgetReport evaluate(BudgetInput input) {
        EVALUATIONS.incrementAndGet();
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
        if (needed) {
            NEEDED_TRUE.incrementAndGet();
        } else {
            NEEDED_FALSE.incrementAndGet();
        }
        if (effective - fixedOverhead < 0) {
            NEGATIVE_CLAMPS.incrementAndGet();
        } else {
            NORMAL_BUDGETS.incrementAndGet();
        }
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
