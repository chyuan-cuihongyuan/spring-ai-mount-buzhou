package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
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

    /** 无摘要模型路径的页脚预算回退值（token）。 */
    private static final int FALLBACK_SUMMARY_TOKEN_BUDGET = 2000;

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
    private SkillCatalogRenderer skillCatalogRenderer;
    // T25/T26：事实对账 + 双时序台账（会话状态经 setter 注入，避免构造器涟漪）
    private io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore sessionStateStore;
    private boolean factReconciliation = true;
    // impl-02 / T36：部分逐出比例（默认 0.7，Letta「evict only ~70%」）+ 10% 步进梯子
    private double evictRatio = DEFAULT_EVICT_RATIO;
    private java.util.function.Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> eventSink;

    /** impl-02：默认逐出比例（保留 30% 最新候选原文内联续接）。 */
    public static final double DEFAULT_EVICT_RATIO = 0.7d;
    /** impl-02：预算仍超时的步进梯子步长（0.7→0.8→…→1.0）。 */
    public static final double EVICT_RATIO_LADDER_STEP = 0.10d;

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

    /** 注入技能清单渲染器（spec 04 Skill Catalog 注入；系统侧固定扣除计入预算）。 */
    public void setSkillCatalogRenderer(SkillCatalogRenderer skillCatalogRenderer) {
        this.skillCatalogRenderer = skillCatalogRenderer;
    }

    /** 会话状态存储（T26 双时序台账用；未注入则对账照跑、不落台账）。 */
    public void setSessionStateStore(
            io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore sessionStateStore) {
        this.sessionStateStore = sessionStateStore;
    }

    /** T25 事实对账开关（默认开；解析失败一律 NOOP，不影响既有压缩链）。 */
    public void setFactReconciliation(boolean factReconciliation) {
        this.factReconciliation = factReconciliation;
    }

    /** impl-02 / T36：部分逐出比例（(0,1]，默认 0.7；1.0 即全量逐出旧行为）。 */
    public void setEvictRatio(double evictRatio) {
        this.evictRatio = evictRatio <= 0.0d || Double.isNaN(evictRatio)
                ? DEFAULT_EVICT_RATIO
                : Math.min(evictRatio, 1.0d);
    }

    /** 对账事件出口（T25 四态裁决可观测；未注入则仅日志）。 */
    public void setEventSink(
            java.util.function.Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public List<BuzhouMessage> process(String sessionId, List<BuzhouMessage> stored, int currentTurn) {
        List<BuzhouMessage> compacted = compactor
                .compact(stored, currentTurn, policyFn, protectRecentTurns, evictRatio)
                .compactedView();
        // 先渲染事实块（maxInjectChars 截断 + 指针），供预算入账与注入共用（spec 07：先渲染后评估；
        // system-reminder 块与摘要 Current State 追加两通道共享同一文本，不重复超额）
        String factsBlock = renderFacts(sessionId, currentTurn);
        // 渲染技能清单块（spec 04：每轮现取，上架/解绑下一轮即生效；系统侧固定扣除计入预算）
        String catalogBlock = renderCatalog(sessionId);
        if (summaryModel == null) {
            // 无摘要模型时事实/清单仍需注入（注入闭环不依赖摘要链路）
            return (factsBlock == null && catalogBlock == null) ? compacted
                    : assembleWithSummary(compacted, null, factsBlock, catalogBlock,
                            currentTurn, sessionId, FALLBACK_SUMMARY_TOKEN_BUDGET);
        }

        NineSectionSummary previous = summaryBridge.loadLatest(sessionId).orElse(null);
        BudgetReport budget = evaluateBudget(compacted, previous, factsBlock, catalogBlock);
        // impl-02 / T36：10% 步进梯子——部分逐出后仍超预算时逐级加压（0.7→0.8→…→1.0）；
        // 梯子救回预算则免落摘要折叠（保连续优先于折摘要）
        double ladderRatio = evictRatio;
        while (budget.compactionNeeded() && ladderRatio < 1.0d) {
            ladderRatio = Math.min(1.0d, ladderRatio + EVICT_RATIO_LADDER_STEP);
            compacted = compactor
                    .compact(stored, currentTurn, policyFn, protectRecentTurns, ladderRatio)
                    .compactedView();
            budget = evaluateBudget(compacted, previous, factsBlock, catalogBlock);
        }
        // T23：摘要 token 预算（动态拆解为每段字符预算页脚渲染给模型）
        int summaryTokenBudget = Math.max(budget.historyBudget(), 1000);
        if (!budget.compactionNeeded()) {
            return injectSummaryOnly(compacted, previous, factsBlock, catalogBlock,
                    currentTurn, sessionId, summaryTokenBudget);
        }

        int cutoffTurn = currentTurn - keepRecentTurns;
        int alreadyCovered = previous == null ? 0 : previous.coversUpToTurn();
        List<String> summarizedIds = previous == null ? List.of() : previous.summarizedMessageIds();
        // T24 增量摘要：轮次水位 + 消息 id 水位双保险，只折入「新消息」（不全量重摘要）
        List<BuzhouMessage> toSummarize = compacted.stream()
                .filter(m -> m.turnSeq() <= cutoffTurn && m.turnSeq() > alreadyCovered)
                .filter(m -> !summarizedIds.contains(m.id()))
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
                        summaryTokenBudget,
                        new CharHeuristicTokenEstimator());
                merged = merged.withSummarizedMessageIds(NineSectionSummary.unionIds(
                        summarizedIds, toSummarize.stream().map(BuzhouMessage::id).toList()));
                if (factReconciliation) {
                    // T25 事实对账（ADD/UPDATE/DELETE/NOOP；解析失败一律 NOOP）+ T26 双时序台账
                    merged = new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryFactReconciler()
                            .reconcile(sessionId, previous, merged, summaryModel, eventSink,
                                    sessionStateStore == null ? null
                                            : new io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger(
                                            sessionStateStore));
                }
                summaryBridge.save(sessionId, merged);
                breaker.onSuccess(sessionId);
            } catch (RuntimeException e) {
                breaker.onFailure(sessionId);
                return (factsBlock == null && catalogBlock == null) ? compacted
                        : assembleWithSummary(compacted, null, factsBlock, catalogBlock,
                                currentTurn, sessionId, summaryTokenBudget);
            }
        }
        return assembleWithSummary(recent, merged, factsBlock, catalogBlock, currentTurn, sessionId,
                summaryTokenBudget);
    }

    /** 渲染未过期事实为注入文本（含截断与指针）；无渲染器/无事实/无 sessionId 时返回 null。 */
    private String renderFacts(String sessionId, int currentTurn) {
        if (attachmentRenderer == null || sessionId == null) {
            return null;
        }
        return attachmentRenderer.render(sessionId, currentTurn, maxInjectChars).orElse(null);
    }

    /** 渲染当前会话可见的技能清单为注入文本；无渲染器/无 sessionId/无可见技能时返回 null。 */
    private String renderCatalog(String sessionId) {
        if (skillCatalogRenderer == null || sessionId == null) {
            return null;
        }
        return skillCatalogRenderer.renderCatalog(sessionId).orElse(null);
    }

    private BudgetReport evaluateBudget(List<BuzhouMessage> compacted, NineSectionSummary summary,
                                        String factsBlock, String catalogBlock) {
        // 事实块 + 技能清单块 token 均计「系统提示词一侧」固定扣除（spec 07/04：不挤历史预算）
        String systemSide = joinNonEmpty(factsBlock, catalogBlock);
        return budgetCalculator.evaluate(new BudgetInput(
                modelName, systemSide, List.of(), "",
                summary == null ? null : summaryBridgeSnapshot(summary),
                compacted.stream().map(m -> (org.springframework.ai.chat.messages.Message)
                        new org.springframework.ai.chat.messages.UserMessage(
                                m.content() == null ? "" : m.content())).toList(),
                8000, 3000, 0.90));
    }

    private static String joinNonEmpty(String a, String b) {
        if (a == null && b == null) {
            return "";
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a + "\n" + b;
    }

    private io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary summaryBridgeSnapshot(
            NineSectionSummary summary) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary(
                "", summary.generation(), Map.of("render", summary.render()), 0, Instant.now());
    }

    private List<BuzhouMessage> injectSummaryOnly(List<BuzhouMessage> compacted,
                                                  NineSectionSummary summary, String factsBlock,
                                                  String catalogBlock,
                                                  int currentTurn, String sessionId,
                                                  int summaryTokenBudget) {
        // 无摘要、无事实块且无清单块 → 直接返回（无需注入）
        if (summary == null && factsBlock == null && catalogBlock == null) {
            return compacted;
        }
        int cutoffTurn = currentTurn - keepRecentTurns;
        List<BuzhouMessage> recent = compacted.stream()
                .filter(m -> m.turnSeq() > cutoffTurn)
                .toList();
        return assembleWithSummary(recent.isEmpty() ? compacted : recent, summary, factsBlock,
                catalogBlock, currentTurn, sessionId, summaryTokenBudget);
    }

    private List<BuzhouMessage> assembleWithSummary(List<BuzhouMessage> recent,
                                                    NineSectionSummary summary, String factsBlock,
                                                    String catalogBlock,
                                                    int currentTurn, String sessionId,
                                                    int summaryTokenBudget) {
        List<BuzhouMessage> result = new ArrayList<>();
        if (summary != null) {
            // 把未过期事实追加到 CURRENT_STATE 段（P0 死保，压缩不丢现场）
            NineSectionSummary enriched = enrichWithFacts(summary, factsBlock);
            // T23：动态预算拆解渲染给模型——每段 chars_current/chars_limit 页脚，模型自削 P3
            String rendered = io.github.chyuan_cuihongyuan.buzhou.memory.budget.SegmentBudgetPlanner
                    .renderWithFooters(enriched, summaryTokenBudget);
            BuzhouMessage synthetic = new BuzhouMessage(
                    UUID.randomUUID().toString(), "", currentTurn, 0, Role.SYSTEM,
                    "<system-reminder>\n以下是早前对话的结构化摘要：\n" + rendered
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
        // 技能清单 Catalog 块（spec 04：系统提示词尾部，事实块之后、近期原文之前）
        if (catalogBlock != null) {
            BuzhouMessage catalogMsg = new BuzhouMessage(
                    UUID.randomUUID().toString(), "", currentTurn, 0, Role.SYSTEM,
                    "<system-reminder>\n" + catalogBlock + "\n</system-reminder>",
                    List.of(), null, null, null, Map.of("skill-catalog", true), Instant.now());
            result.add(catalogMsg);
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
