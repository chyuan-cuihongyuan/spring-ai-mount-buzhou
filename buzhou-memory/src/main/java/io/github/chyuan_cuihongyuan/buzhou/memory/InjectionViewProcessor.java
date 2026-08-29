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
    /** spec 34 §A / spec 38 §A：压缩结果监听器（sessionId + 结果 + 当前逐出比例；MemoryModule 接观测双写）。 */
    private io.github.chyuan_cuihongyuan.buzhou.memory.CompactionListener compactionListener;
    /** spec 66 §A / T283：前缀稳定注入序（默认关——稳定块前置最大化 KV-cache 前缀命中）。 */
    private boolean prefixStableInjection = false;

    /** spec 66 §A / T283：前缀稳定注入序开关（true = 清单块前置：catalog→summary→facts→recent）。 */
    public void setPrefixStableInjection(boolean prefixStableInjection) {
        this.prefixStableInjection = prefixStableInjection;
    }

    /** spec 70 §A / T291：边界机会压缩阈值（待摘积压 ≥ N 提前摘要；默认 0=关）。 */
    public void setBoundaryCompactBacklog(int boundaryCompactBacklog) {
        this.boundaryCompactBacklog = boundaryCompactBacklog;
    }

    /** spec 90 §A / T343：语义漂移触发边界压缩（null=关——默认零变化）。 */
    public void setSemanticDriftDetector(
            io.github.chyuan_cuihongyuan.buzhou.memory.compact.SemanticDriftDetector detector) {
        this.semanticDriftDetector = detector;
    }

    /** spec 90 §A / T343：漂移检测器（null=关；有摘要基准且话题漂移时提前边界压缩）。 */
    private io.github.chyuan_cuihongyuan.buzhou.memory.compact.SemanticDriftDetector semanticDriftDetector;

    /** spec 70 §A / T291：边界机会压缩积压阈值（0=关）。 */
    private int boundaryCompactBacklog = 0;

    /** impl-02：默认逐出比例（保留 30% 最新候选原文内联续接）。 */
    public static final double DEFAULT_EVICT_RATIO = 0.7d;
    /** impl-02：预算仍超时的步进梯子步长（0.7→0.8→…→1.0）。 */
    public static final double EVICT_RATIO_LADDER_STEP = 0.10d;
    /** impl-13 / T40：压缩前检查点与三档回滚（回滚 = 视图级，append-only 事实源不动）。 */
    private io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints checkpoints;

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

    /** impl-13 / T40：注入压缩前检查点（折叠前保存 + 回滚标记消费）。 */
    public void setCheckpoints(
            io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints checkpoints) {
        this.checkpoints = checkpoints;
    }

    /** 对账事件出口（T25 四态裁决可观测；未注入则仅日志）。 */
    public void setCompactionListener(
            io.github.chyuan_cuihongyuan.buzhou.memory.CompactionListener listener) {
        this.compactionListener = listener;
    }

    /** spec 34 §A / spec 38 §A：有实际折入才通知（空压缩零噪音；ratio 随级携带）。 */
    private void notifyCompaction(String sessionId,
            io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult result,
            double evictRatio) {
        if (compactionListener != null && result != null && !result.compactedMessageIds().isEmpty()) {
            try {
                compactionListener.onCompacted(sessionId, result, evictRatio);
            } catch (RuntimeException ignored) {
                // 观测双写失败不影响视图主链（lenient）
            }
        }
    }

    public void setEventSink(
            java.util.function.Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public List<BuzhouMessage> process(String sessionId, List<BuzhouMessage> stored, int currentTurn) {
        // impl-13 / T40：回滚按 Turn 消费——同一 Turn 的全部视图生成一致恢复为检查点窗口
        //（一次 chat 会触发多次 get，按 Turn 对齐避免「首次 get 恢复、后续 get 又折叠」的撕裂）
        java.util.Optional<io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints.RollbackLevel> rollbackMarker =
                checkpoints == null || sessionId == null
                        ? java.util.Optional.empty()
                        : checkpoints.consumeRollbackForTurn(sessionId, currentTurn);
        if (rollbackMarker.isPresent()) {
            return checkpoints.latestWindow(sessionId).orElseGet(() ->
                    compactor.compact(stored, currentTurn, policyFn, protectRecentTurns, evictRatio)
                            .compactedView());
        }
        io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult compaction =
                compactor.compact(stored, currentTurn, policyFn, protectRecentTurns, evictRatio);
        List<BuzhouMessage> compacted = compaction.compactedView();
        notifyCompaction(sessionId, compaction, evictRatio);
        // 先渲染事实块（maxInjectChars 截断 + 指针），供预算入账与注入共用（spec 07：先渲染后评估；
        // system-reminder 块与摘要 Current State 追加两通道共享同一文本，不重复超额）
        String factsBlock = renderFacts(sessionId, currentTurn);
        // 渲染技能清单块（spec 04：每轮现取，上架/解绑下一轮即生效；系统侧固定扣除计入预算；
        // spec 59 §A / T264：携带本轮问法（最新 USER 文本）——实现方可用作语义排序 hint）
        String catalogBlock = renderCatalog(sessionId, latestUserText(stored));
        if (summaryModel == null) {
            // 无摘要模型时事实/清单仍需注入（注入闭环不依赖摘要链路）
            return (factsBlock == null && catalogBlock == null) ? compacted
                    : assembleWithSummary(compacted, null, factsBlock, catalogBlock,
                            currentTurn, sessionId, FALLBACK_SUMMARY_TOKEN_BUDGET);
        }

        NineSectionSummary previous = summaryBridge.loadLatest(sessionId).orElse(null);
        // impl-13 / T40：摘要失效标记（回滚档 ≥2）——视为无摘要，直至重新压缩生成新摘要
        boolean summaryInvalidated = sessionStateStore != null && sessionId != null
                && io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints
                        .summaryInvalidated(sessionStateStore, sessionId);
        if (summaryInvalidated) {
            previous = null;
        }
        BudgetReport budget = evaluateBudget(compacted, previous, factsBlock, catalogBlock);
        // impl-02 / T36：10% 步进梯子——部分逐出后仍超预算时逐级加压（0.7→0.8→…→1.0）；
        // 梯子救回预算则免落摘要折叠（保连续优先于折摘要）
        double ladderRatio = evictRatio;
        while (budget.compactionNeeded() && ladderRatio < 1.0d) {
            ladderRatio = Math.min(1.0d, ladderRatio + EVICT_RATIO_LADDER_STEP);
            // spec 38 §A / T135：梯子每级实际折入都通知（payload 携带当前级 evictRatio）
            io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult ladderResult =
                    compactor.compact(stored, currentTurn, policyFn, protectRecentTurns, ladderRatio);
            compacted = ladderResult.compactedView();
            notifyCompaction(sessionId, ladderResult, ladderRatio);
            budget = evaluateBudget(compacted, previous, factsBlock, catalogBlock);
        }
        // T23：摘要 token 预算（动态拆解为每段字符预算页脚渲染给模型）
        int summaryTokenBudget = Math.max(budget.historyBudget(), 1000);
        // spec 70 §A / T291：边界机会压缩（Letta「自然边界压缩」的 Completed-Turn 代理）——
        // 待摘积压达阈值时在干净轮边界提前走增量摘要路径（预算尚宽松：摘要质量更高、
        // 豁免梯子紧急态）；默认 0=关零变化；无摘要模型时不适用（无摘要链路可提前）。
        boolean backlogTrigger = false;
        if (!budget.compactionNeeded() && boundaryCompactBacklog > 0 && summaryModel != null) {
            final int cutoffTurn = currentTurn - keepRecentTurns;
            final int alreadyCovered = previous == null ? 0 : previous.coversUpToTurn();
            final NineSectionSummary backlogPrevious = previous;
            backlogTrigger = compacted.stream()
                    .filter(m -> m.turnSeq() <= cutoffTurn && m.turnSeq() > alreadyCovered)
                    .filter(m -> backlogPrevious == null
                            || !backlogPrevious.summarizedMessageIds().contains(m.id()))
                    .count() >= boundaryCompactBacklog;
        }
        // spec 90 §A / T343：语义漂移触发（与积压触发同路径不同判据）——话题已漂移时
        // 旧话题轮次近期大概率不再被引用，是比积压计数更贴近「自然边界」的提前时机；
        // 需有摘要基准（previous 非空）才可比对；无待摘消息时走下游空折入自然无害。
        boolean driftTrigger = semanticDriftDetector != null && previous != null
                && semanticDriftDetector.drifted(latestUserText(stored), previous.render());
        if (!budget.compactionNeeded() && !backlogTrigger && !driftTrigger) {
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
                // impl-13 / T40：折叠提交前保存压缩前检查点（护栏：事故可回滚）
                if (checkpoints != null) {
                    checkpoints.save(sessionId, cutoffTurn, stored);
                }
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
                // impl-13 / T40：重新压缩成功 → 清除摘要失效标记（新一轮摘要生效）
                if (summaryInvalidated && sessionStateStore != null) {
                    io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints
                            .clearSummaryInvalidation(sessionStateStore, sessionId);
                }
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
    private String renderCatalog(String sessionId, String queryHint) {
        if (skillCatalogRenderer == null || sessionId == null) {
            return null;
        }
        return skillCatalogRenderer.renderCatalog(sessionId, queryHint).orElse(null);
    }

    /** spec 59 §A / T264：stored 尾部最新 USER 消息文本（无 = null——无问法语义）。 */
    private static String latestUserText(List<BuzhouMessage> stored) {
        if (stored == null) {
            return null;
        }
        for (int i = stored.size() - 1; i >= 0; i--) {
            BuzhouMessage m = stored.get(i);
            if (m.role() == Role.USER && m.content() != null && !m.content().isBlank()) {
                return m.content();
            }
        }
        return null;
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
        // spec 66 §A / T283：前缀稳定序（默认关）——最稳定块（技能清单）前置，最大化
        // provider 端 KV-cache 前缀命中（Anthropic prompt caching 最佳实践：稳定内容在前、
        // 易变内容在后）；默认序保持 spec 04 口径（摘要→事实→清单）零变化。
        if (prefixStableInjection && catalogBlock != null) {
            result.add(catalogMessage(catalogBlock, currentTurn));
        }
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
        if (catalogBlock != null && !prefixStableInjection) {
            result.add(catalogMessage(catalogBlock, currentTurn));
        }
        if (result.isEmpty()) {
            return recent;
        }
        result.addAll(recent);
        return result;
    }

    private static BuzhouMessage catalogMessage(String catalogBlock, int currentTurn) {
        return new BuzhouMessage(
                UUID.randomUUID().toString(), "", currentTurn, 0, Role.SYSTEM,
                "<system-reminder>\n" + catalogBlock + "\n</system-reminder>",
                List.of(), null, null, null, Map.of("skill-catalog", true), Instant.now());
    }

    /** 把未过期事实追加到摘要 CURRENT_STATE 段（保证压缩后事实仍保留，P0 不丢）。 */
    private NineSectionSummary enrichWithFacts(NineSectionSummary summary, String factsBlock) {
        if (factsBlock == null) {
            return summary;
        }
        return summary.appendCurrentState("\n[已采集事实]\n" + factsBlock);
    }
}
