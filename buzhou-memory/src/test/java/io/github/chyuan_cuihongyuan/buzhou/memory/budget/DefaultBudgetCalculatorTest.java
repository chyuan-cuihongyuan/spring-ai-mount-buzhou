package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBudgetCalculatorTest {

    private final DefaultBudgetCalculator calculator = new DefaultBudgetCalculator(
            new TableContextWindowResolver(Map.of("test-model", 12000)),
            new CharHeuristicTokenEstimator());

    private ToolCallback fakeTool(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d:" + name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{\"" + name + "\":{}}}")
                        .build();
            }

            @Override
            public String call(String toolInput) {
                return "";
            }
        };
    }

    @ParameterizedTest
    @CsvSource({
            "0,    0,   10,   false",
            "3000, 0,   10,   false",
            "0,    60,  10,   true",
            "30000,0,   100,  true",
    })
    void fixedOverheadDrivesCompactionDecision(int systemChars, int toolCount, int historyChars,
                                               boolean expectedNeeded) {
        String systemPrompt = "x".repeat(systemChars);
        List<ToolCallback> tools = java.util.stream.IntStream.range(0, toolCount)
                .mapToObj(i -> fakeTool("tool_" + i))
                .map(t -> (ToolCallback) t)
                .toList();
        List<org.springframework.ai.chat.messages.Message> history =
                List.of(new UserMessage("y".repeat(historyChars)));

        BudgetReport report = calculator.evaluate(BudgetInput.of(
                "test-model", systemPrompt, tools, "hi", null, history));

        int expectedOverhead = new CharHeuristicTokenEstimator().estimate(systemPrompt)
                + calculator.toolSchemaTokens(tools)
                + new CharHeuristicTokenEstimator().estimate("hi");
        assertThat(report.fixedOverhead()).isEqualTo(expectedOverhead);
        assertThat(report.compactionNeeded()).isEqualTo(expectedNeeded);
        assertThat(report.historyBudget()).isEqualTo(Math.max(report.effectiveWindow() - report.fixedOverhead(), 0));
    }

    @Test
    void summaryTokensCountTowardTotal() {
        StructuredSummary summary = new StructuredSummary("s", 1,
                Map.of("P0", "x".repeat(40000)), 0, Instant.now());
        BudgetReport without = calculator.evaluate(BudgetInput.of("test-model", "", List.of(), "", null, List.of()));
        BudgetReport with = calculator.evaluate(BudgetInput.of("test-model", "", List.of(), "", summary, List.of()));

        assertThat(with.estimatedTotal()).isGreaterThan(without.estimatedTotal());
        assertThat(with.summaryTokens()).isGreaterThan(0);
    }

    @Test
    void toolSchemaTokensAreCachedByToolsetHash() {
        List<ToolCallback> tools = List.of(fakeTool("a"), fakeTool("b"));
        int first = calculator.toolSchemaTokens(tools);
        int cacheSize = calculator.schemaCacheSize();
        int second = calculator.toolSchemaTokens(tools);

        assertThat(first).isEqualTo(second);
        assertThat(calculator.schemaCacheSize()).isEqualTo(cacheSize);
    }

    @Test
    void unknownModelFallsBackTo32K() {
        BudgetReport report = calculator.evaluate(BudgetInput.of(
                "some-unknown-model", "", List.of(), "", null, List.of()));
        assertThat(report.contextWindow()).isEqualTo(32768);
    }
}
