package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
/** spec 1529 / T2309：预算计算输入——分层预算计算的输入载体（层级容量/当前占用）。 */

public record BudgetInput(
        String modelName,
        String systemPrompt,
        List<ToolCallback> toolCallbacks,
        String currentInput,
        StructuredSummary currentSummary,
        List<Message> historyAfterMicroCompaction,
        int reserveOutputTokens,
        int safetyBufferTokens,
        double threshold) {

    public BudgetInput {
        toolCallbacks = toolCallbacks == null ? List.of() : List.copyOf(toolCallbacks);
    }

    public static BudgetInput of(String modelName, String systemPrompt,
                                 List<ToolCallback> toolCallbacks, String currentInput,
                                 StructuredSummary currentSummary,
                                 List<Message> historyAfterMicroCompaction) {
        return new BudgetInput(modelName, systemPrompt, toolCallbacks, currentInput,
                currentSummary, historyAfterMicroCompaction, 8000, 3000, 0.90);
    }
}
