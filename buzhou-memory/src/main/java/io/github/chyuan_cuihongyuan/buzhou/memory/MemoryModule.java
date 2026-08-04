package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.ToolPolicyMatcher;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.EvidenceLookupTool;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MemoryModule {

    private MemoryModule() {
    }

    public static RuntimeConfig configure(Map<String, Object> ymlConfig, MessageStore messageStore) {
        return configure(ymlConfig, null, messageStore, null, null, null);
    }

    public static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                          ChatModel mainModel, ChatModel summaryModel) {
        return configure(ymlConfig, stores, stores.messageStore(),
                mainModel, summaryModel == null ? mainModel : summaryModel, null);
    }

    /** 带 AttachmentRenderer 的重载（Hook→state→Attachment 闭环，ticket 13）。 */
    public static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                          ChatModel mainModel, ChatModel summaryModel,
                                          io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer) {
        return configure(ymlConfig, stores, stores.messageStore(),
                mainModel, summaryModel == null ? mainModel : summaryModel, attachmentRenderer);
    }

    private static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                           MessageStore messageStore,
                                           ChatModel mainModel, ChatModel summaryModel,
                                           io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer) {
        DefaultMicroCompactor compactor = new DefaultMicroCompactor(new DefaultCompletedTurnDetector());
        Function<String, MicroCompactionPolicy> policyFn = policyFn(ymlConfig);
        int protectRecentTurns = protectRecentTurns(ymlConfig);

        io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor processor;
        if (summaryModel == null || stores == null) {
            processor = (sessionId, stored, currentTurn) ->
                    compactor.compact(stored, currentTurn, policyFn, protectRecentTurns)
                            .compactedView();
        } else {
            DefaultBudgetCalculator budgetCalculator = new DefaultBudgetCalculator(
                    new TableContextWindowResolver(windowOverrides(ymlConfig)),
                    new CharHeuristicTokenEstimator());
            InjectionViewProcessor ivp = new InjectionViewProcessor(compactor, policyFn, protectRecentTurns,
                    budgetCalculator, new SummaryStoreBridge(stores.summaryStore()),
                    new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3), summaryModel,
                    modelName(ymlConfig), keepRecentTurns(ymlConfig), extraInstruction(ymlConfig));
            ivp.setAttachmentRenderer(attachmentRenderer);
            processor = ivp;
        }
        return new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                processor, List.of(new EvidenceLookupTool(messageStore)));
    }

    private static String modelName(Map<String, Object> ymlConfig) {
        Object value = ymlConfig.get("model-name");
        return value instanceof String s ? s : "unknown";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> windowOverrides(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object overrides = ((Map<String, Object>) memory).get("context-window");
            if (overrides instanceof Map) {
                Map<String, Integer> result = new java.util.HashMap<>();
                ((Map<String, Object>) overrides).forEach((k, v) -> {
                    if (v instanceof Number n) {
                        result.put(k, n.intValue());
                    }
                });
                return result;
            }
        }
        return Map.of();
    }

    private static int keepRecentTurns(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object value = ((Map<String, Object>) memory).get("keep-recent-turns");
            if (value instanceof Number n) {
                return n.intValue();
            }
        }
        return 2;
    }

    private static String extraInstruction(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object value = ((Map<String, Object>) memory).get("summary-extra-instruction");
            if (value instanceof String s) {
                return s;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Function<String, MicroCompactionPolicy> policyFn(Map<String, Object> ymlConfig) {
        Object section = ymlConfig.get("tool-policies");
        Map<String, Object> toolPolicies = section instanceof Map
                ? (Map<String, Object>) section : Map.of();
        return toolName -> {
            Map<String, Object> matched = ToolPolicyMatcher.match(toolPolicies, toolName);
            Object mc = matched.get("micro-compaction");
            if (!(mc instanceof Map)) {
                return MicroCompactionPolicy.defaults();
            }
            Map<String, Object> mcMap = (Map<String, Object>) mc;
            return new MicroCompactionPolicy(
                    bool(mcMap.get("never-compress"), false),
                    integer(mcMap.get("max-age-turns"), 3),
                    integer(mcMap.get("min-size-chars"), 200));
        };
    }

    @SuppressWarnings("unchecked")
    private static int protectRecentTurns(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object mc = ((Map<String, Object>) memory).get("micro-compaction");
            if (mc instanceof Map) {
                return integer(((Map<String, Object>) mc).get("protect-recent-turns"), 1);
            }
        }
        return 1;
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean b ? b : fallback;
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }
}
