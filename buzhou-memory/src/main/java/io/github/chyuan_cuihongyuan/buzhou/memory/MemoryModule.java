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
        return configure(ymlConfig, null, messageStore, null, null, null, null);
    }

    public static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                          ChatModel mainModel, ChatModel summaryModel) {
        return configure(ymlConfig, stores, stores.messageStore(),
                mainModel, summaryModel == null ? mainModel : summaryModel, null, null);
    }

    /** 带 AttachmentRenderer 的重载（Hook→state→Attachment 闭环，ticket 13）。 */
    public static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                          ChatModel mainModel, ChatModel summaryModel,
                                          io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer) {
        return configure(ymlConfig, stores, stores.messageStore(),
                mainModel, summaryModel == null ? mainModel : summaryModel, attachmentRenderer, null);
    }

    /** 带 AttachmentRenderer + SkillCatalogRenderer 的重载（ticket 14 Skill Catalog 注入）。 */
    public static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                          ChatModel mainModel, ChatModel summaryModel,
                                          io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer,
                                          io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer skillCatalogRenderer) {
        return configure(ymlConfig, stores, stores.messageStore(),
                mainModel, summaryModel == null ? mainModel : summaryModel,
                attachmentRenderer, skillCatalogRenderer);
    }

    private static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                           MessageStore messageStore,
                                           ChatModel mainModel, ChatModel summaryModel,
                                           io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer,
                                           io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer skillCatalogRenderer) {
        DefaultMicroCompactor compactor = new DefaultMicroCompactor(new DefaultCompletedTurnDetector());
        Function<String, MicroCompactionPolicy> policyFn = policyFn(ymlConfig);
        int protectRecentTurns = protectRecentTurns(ymlConfig);
        // impl-02 / T36：部分逐出比例（默认 0.7；1.0 回到全量逐出旧行为）
        double evictRatio = evictRatio(ymlConfig);

        io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor processor;
        java.util.List<org.springframework.ai.tool.ToolCallback> tools =
                new java.util.ArrayList<>(java.util.List.of(new EvidenceLookupTool(messageStore)));
        if (stores == null) {
            processor = (sessionId, stored, currentTurn) ->
                    compactor.compact(stored, currentTurn, policyFn, protectRecentTurns, evictRatio)
                            .compactedView();
        } else {
            // summaryModel 可空：InjectionViewProcessor 在无摘要模型时仍注入事实块（spec 07 闭环）
            DefaultBudgetCalculator budgetCalculator = new DefaultBudgetCalculator(
                    new TableContextWindowResolver(windowOverrides(ymlConfig)),
                    new CharHeuristicTokenEstimator());
            SummaryStoreBridge summaryBridge = new SummaryStoreBridge(stores.summaryStore());
            InjectionViewProcessor ivp = new InjectionViewProcessor(compactor, policyFn, protectRecentTurns,
                    budgetCalculator, summaryBridge,
                    new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3), summaryModel,
                    modelName(ymlConfig), keepRecentTurns(ymlConfig), extraInstruction(ymlConfig),
                    maxInjectChars(ymlConfig));
            ivp.setAttachmentRenderer(attachmentRenderer);
            ivp.setSkillCatalogRenderer(skillCatalogRenderer);
            ivp.setEvictRatio(evictRatio);
            // T25/T26：事实对账 + 双时序台账（会话状态；对账默认开、韧性 NOOP）
            ivp.setSessionStateStore(stores.sessionStateStore());
            ivp.setFactReconciliation(factReconciliation(ymlConfig));
            processor = ivp;
            // T27：compact_now 语义边界压缩工具（有摘要模型才可自触发；token 阈值兜底不受影响）
            if (summaryModel != null && compactNowTool(ymlConfig)) {
                boolean reconcile = factReconciliation(ymlConfig);
                tools.add(new io.github.chyuan_cuihongyuan.buzhou.memory.tool.CompactNowTool(
                        stores.messageStore(), summaryBridge, new DefaultSummaryGenerator(),
                        summaryModel, keepRecentTurns(ymlConfig),
                        reconcile ? new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryFactReconciler() : null,
                        reconcile ? new io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger(
                                stores.sessionStateStore()) : null,
                        null));
            }
        }
        return new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                processor, tools);
    }

    /** T25 开关：{@code memory.fact-reconciliation}（默认开）。 */
    private static boolean factReconciliation(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object value = ((Map<?, ?>) memory).get("fact-reconciliation");
            return !(value instanceof Boolean b) || b;
        }
        return true;
    }

    /** T27 开关：{@code memory.compact-now-tool}（默认开；需配置摘要模型）。 */
    private static boolean compactNowTool(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object value = ((Map<?, ?>) memory).get("compact-now-tool");
            return !(value instanceof Boolean b) || b;
        }
        return true;
    }

    /** impl-02 / T36：{@code memory.micro-compaction.evict-ratio}（默认 0.7；(0,1] 钳制）。 */
    private static double evictRatio(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object mc = ((Map<?, ?>) memory).get("micro-compaction");
            if (mc instanceof Map) {
                Object value = ((Map<?, ?>) mc).get("evict-ratio");
                if (value instanceof Number n && n.doubleValue() > 0.0d && n.doubleValue() <= 1.0d) {
                    return n.doubleValue();
                }
            }
        }
        return InjectionViewProcessor.DEFAULT_EVICT_RATIO;
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

    /** spec 07 配置项 {@code buzhou.facts.max-inject-chars}（默认 4000；<=0 不截断）。 */
    @SuppressWarnings("unchecked")
    private static int maxInjectChars(Map<String, Object> ymlConfig) {
        Object facts = ymlConfig.get("facts");
        if (facts instanceof Map) {
            Object value = ((Map<String, Object>) facts).get("max-inject-chars");
            if (value instanceof Number n) {
                return n.intValue();
            }
        }
        return 4000;
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
