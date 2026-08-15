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

import java.time.Duration;
import java.time.format.DateTimeParseException;
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

    /**
     * impl-30 / spec 13 §core-1：带模块自有资源收集器的重载——模块内联创建的后台设施
     * （如 sleep-time 调度器）登记进 {@code moduleOwnedResources}，由 memory 的
     * SmartLifecycle 在停机时关闭（既有重载不收集，行为不变）。
     */
    public static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                          ChatModel mainModel, ChatModel summaryModel,
                                          io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer,
                                          io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer skillCatalogRenderer,
                                          List<AutoCloseable> moduleOwnedResources) {
        return configure(ymlConfig, stores, stores.messageStore(),
                mainModel, summaryModel == null ? mainModel : summaryModel,
                attachmentRenderer, skillCatalogRenderer, moduleOwnedResources);
    }

    private static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                           MessageStore messageStore,
                                           ChatModel mainModel, ChatModel summaryModel,
                                           io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer,
                                           io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer skillCatalogRenderer) {
        return configure(ymlConfig, stores, messageStore, mainModel, summaryModel,
                attachmentRenderer, skillCatalogRenderer, null);
    }

    private static RuntimeConfig configure(Map<String, Object> ymlConfig, BuzhouStores stores,
                                           MessageStore messageStore,
                                           ChatModel mainModel, ChatModel summaryModel,
                                           io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer attachmentRenderer,
                                           io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer skillCatalogRenderer,
                                           List<AutoCloseable> moduleOwnedResources) {
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
                    new DefaultSummaryGenerator(), summaryCircuitBreaker(ymlConfig), summaryModel,
                    modelName(ymlConfig), keepRecentTurns(ymlConfig), extraInstruction(ymlConfig),
                    maxInjectChars(ymlConfig));
            ivp.setAttachmentRenderer(attachmentRenderer);
            ivp.setSkillCatalogRenderer(skillCatalogRenderer);
            ivp.setEvictRatio(evictRatio);
            // impl-13 / T40：压缩前检查点与三档回滚
            ivp.setCheckpoints(new io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints(
                    stores.sessionStateStore()));
            // spec 34 §A / T115 / impl-90：压缩事件观测双写（memory.compacted——视图读路径无
            // 会话事件通道，走 ObservabilityStore 侧写，RunawayCounters 同款通道）
            ivp.setCompactionListener((sessionId, result) -> stores.observabilityStore().saveEvents(
                    java.util.List.of(new io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord(
                            java.util.UUID.randomUUID().toString(), null, sessionId,
                            "memory.compacted", java.time.Instant.now(),
                            java.util.Map.of(
                                    "compactedCount", result.compactedMessageIds().size(),
                                    "reclaimedChars", result.reclaimedChars())))));
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
            // impl-12 / T38：自愈记忆工具（精确匹配 + 唯一性 + P0 只读 + taint 门 + 全量审计）
            if (reviseSectionTool(ymlConfig)) {
                tools.add(new io.github.chyuan_cuihongyuan.buzhou.memory.tool.ReviseSummarySectionTool(
                        summaryBridge, stores.sessionStateStore()));
            }
            // impl-15 / T41：模糊召回工具（text/time 恒可用；embedding/hybrid 经 provider 降级）
            tools.add(new io.github.chyuan_cuihongyuan.buzhou.memory.tool.RecallSearchTool(
                    messageStore, embeddingProvider(ymlConfig)));
        }
        java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer>
                sessionCustomizers = new java.util.ArrayList<>();
        return new RuntimeConfig(
                sleepTimeHooks(ymlConfig, stores, summaryModel, moduleOwnedResources, sessionCustomizers),
                java.util.Set.of(), java.util.Set.of(),
                processor, tools, java.util.Map.of(), sessionCustomizers);
    }

    /**
     * impl-11 / T37：sleep-time 后台整理钩子（默认开、每 5 Turn 一次；虚拟线程 + 每 session
     * 串行；热路径零阻塞）。无 stores/摘要模型时不注册。
     *
     * <p>impl-30 / spec 13 §core-1：调度器登记进 {@code moduleOwnedResources}（非 null 时）——
     * 此前内联创建从不关闭，本片接线进 memory SmartLifecycle 的 stop。
     *
     * <p>impl-38 / spec 13 §growth-8：会话结束摘除——注册 sessionCustomizer，会话 close 经
     * 资源注册表移除该会话的 pending 队列（防长跑进程 per-session 表泄漏）。
     */
    private static java.util.List<io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook> sleepTimeHooks(
            Map<String, Object> ymlConfig, BuzhouStores stores, ChatModel summaryModel,
            List<AutoCloseable> moduleOwnedResources,
            List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer> customizersOut) {
        if (stores == null || summaryModel == null || !sleepTimeEnabled(ymlConfig)) {
            return List.of();
        }
        io.github.chyuan_cuihongyuan.buzhou.memory.consolidation.SleepTimeScheduler scheduler =
                new io.github.chyuan_cuihongyuan.buzhou.memory.consolidation.SleepTimeScheduler();
        if (moduleOwnedResources != null) {
            moduleOwnedResources.add(scheduler);
        }
        if (customizersOut != null) {
            customizersOut.add((registry, appId, agentName, sessionId) ->
                    registry.register("sleep-time-queue", () -> scheduler.removeSession(sessionId)));
        }
        io.github.chyuan_cuihongyuan.buzhou.memory.consolidation.SleepTimeConsolidator consolidator =
                new io.github.chyuan_cuihongyuan.buzhou.memory.consolidation.SleepTimeConsolidator(
                        new SummaryStoreBridge(stores.summaryStore()), summaryModel,
                        stores.sessionStateStore(), null);
        return List.of(new io.github.chyuan_cuihongyuan.buzhou.memory.consolidation.SleepTimeConsolidationHook(
                scheduler, consolidator, sleepTimeEveryTurns(ymlConfig)));
    }

    /**
     * spec 13 §stores-7 / ticket 32：摘要熔断器配置——
     * {@code memory.summary-circuit-breaker.failure-threshold}（默认 3）与
     * {@code memory.summary-circuit-breaker.failure-window}（ISO-8601 或秒数，默认 PT10M）。
     * 窗口过后半开试探、成功清零、失败重计重新关窗（详见 {@link SummaryCircuitBreaker}）。
     */
    private static SummaryCircuitBreaker summaryCircuitBreaker(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        Map<?, ?> breaker = memory instanceof Map<?, ?> memoryMap
                && ((Map<?, ?>) memoryMap).get("summary-circuit-breaker") instanceof Map<?, ?> cb
                ? cb : Map.of();
        int threshold = integer(breaker.get("failure-threshold"),
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD);
        Duration window = failureWindow(breaker.get("failure-window"));
        return new SummaryCircuitBreaker(threshold, window);
    }

    /** 窗口解析：ISO-8601（如 {@code PT10M}）或秒数（如 {@code 600}）。 */
    private static Duration failureWindow(Object value) {
        if (value instanceof Number n && n.longValue() > 0L) {
            return Duration.ofSeconds(n.longValue());
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                Duration parsed = Duration.parse(s);
                if (!parsed.isNegative() && !parsed.isZero()) {
                    return parsed;
                }
            } catch (DateTimeParseException ignored) {
                // 非法格式按默认窗口降级（启动不炸；配置校验属 cross-12 范畴）
            }
        }
        return SummaryCircuitBreaker.DEFAULT_FAILURE_WINDOW;
    }

    /** impl-11 开关：{@code memory.sleep-time.enabled}（默认开）。 */
    private static boolean sleepTimeEnabled(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object st = ((Map<?, ?>) memory).get("sleep-time");
            if (st instanceof Map) {
                Object value = ((Map<?, ?>) st).get("enabled");
                return !(value instanceof Boolean b) || b;
            }
        }
        return true;
    }

    /** impl-11 频率：{@code memory.sleep-time.every-turns}（默认 5）。 */
    private static int sleepTimeEveryTurns(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object st = ((Map<?, ?>) memory).get("sleep-time");
            if (st instanceof Map) {
                Object value = ((Map<?, ?>) st).get("every-turns");
                if (value instanceof Number n && n.intValue() > 0) {
                    return n.intValue();
                }
            }
        }
        return 5;
    }

    /**
     * impl-15 / T41：EmbeddingProvider 解析——{@code memory.embedding-provider} 配置为
     * 实现类全名（部署侧注入真模型；测试用确定性词包）。缺省 null（embedding/hybrid 降级）。
     *
     * <p>impl-38 / spec 13 §growth-8：解析结果统一包 {@code CachedEmbeddingProvider}
     * （内容 hash 键、LRU 容量 {@code memory.embedding-cache-capacity} 默认 512）——
     * recall_search / EpisodeLedger / SemanticChunkIndex 经模块解析共享同一份 embed-once 缓存。
     */
    private static io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider embeddingProvider(
            Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object value = ((Map<?, ?>) memory).get("embedding-provider");
            if (value instanceof String className && !className.isBlank()) {
                try {
                    io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider resolved =
                            (io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider)
                                    Class.forName(className).getDeclaredConstructor().newInstance();
                    return new io.github.chyuan_cuihongyuan.buzhou.core.spi.CachedEmbeddingProvider(
                            resolved, embeddingCacheCapacity(ymlConfig));
                } catch (Exception e) {
                    System.getLogger(MemoryModule.class.getName()).log(System.Logger.Level.WARNING,
                            "embedding-provider 实例化失败（按未注入降级）：{0}", className);
                }
            }
        }
        return null;
    }

    /** impl-38：{@code memory.embedding-cache-capacity}（默认 512；非正回落默认）。 */
    private static int embeddingCacheCapacity(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object value = ((Map<?, ?>) memory).get("embedding-cache-capacity");
            if (value instanceof Number number && number.intValue() > 0) {
                return number.intValue();
            }
        }
        return io.github.chyuan_cuihongyuan.buzhou.core.spi.CachedEmbeddingProvider.DEFAULT_CAPACITY;
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

    /**
     * spec 20 / T90 / impl-65：宿主侧手动压缩器（与 compact_now 工具共用同一条管线；
     * summaryModel 为 null 时返回 null——无摘要模型不可用，调用方按缺省处理）。
     */
    public static io.github.chyuan_cuihongyuan.buzhou.memory.compact.ManualCompactor manualCompactor(
            io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores,
            org.springframework.ai.chat.model.ChatModel summaryModel,
            Map<String, Object> ymlConfig) {
        if (summaryModel == null) {
            return null;
        }
        boolean reconcile = factReconciliation(ymlConfig);
        return new io.github.chyuan_cuihongyuan.buzhou.memory.compact.ManualCompactor(
                stores.messageStore(), new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), summaryModel, keepRecentTurns(ymlConfig),
                reconcile ? new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryFactReconciler() : null,
                reconcile ? new io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger(
                        stores.sessionStateStore()) : null,
                null);
    }

    /** impl-12 开关：{@code memory.revise-section-tool}（默认开；自愈记忆 + 防投毒）。 */
    private static boolean reviseSectionTool(Map<String, Object> ymlConfig) {
        Object memory = ymlConfig.get("memory");
        if (memory instanceof Map) {
            Object value = ((Map<?, ?>) memory).get("revise-section-tool");
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
