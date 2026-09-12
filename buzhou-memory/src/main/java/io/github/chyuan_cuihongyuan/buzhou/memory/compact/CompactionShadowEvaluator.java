package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 微压缩影子干跑评估器（spec 612 / T874，Istio mirroring 思想用于策略调参；纯内存
 * 零 LLM 调用——微压缩本就是确定性函数）：对真实历史跑 {@link MicroCompactor#compact}
 * 但<b>不应用结果</b>——只出报告（会压哪些消息、可回收多少字符），供 evictRatio /
 * maxAgeTurns 等策略安全调参。
 *
 * <p>{@link #sweep} 对 evictRatio 梯度（0.25/0.5/0.75/1.0）逐一干跑产出调参表；
 * {@link #evaluateAndEmit} 把单点干跑结果以 {@code memory.compaction-shadow} 事件外发
 * （payload：候选数/回收字符/比例——观测面与执行面同通道）。
 */
public final class CompactionShadowEvaluator {

    /** 事件类型：影子干跑报告（不应用的评估——调参观测面）。 */
    public static final String EVENT_SHADOW = "memory.compaction-shadow";

    /** sweep 默认梯度（Letta「~70% 逐出」语境下的调参域）。 */
    private static final double[] SWEEP_RATIOS = {0.25, 0.5, 0.75, 1.0};

    /** 干跑报告：会压的消息 id + 可回收字符（与 {@link MicroCompactionResult} 同口径）。 */
    public record ShadowReport(double evictRatio, List<String> wouldCompactIds, int reclaimedChars) {
    }

    private final MicroCompactor compactor;

    public CompactionShadowEvaluator(MicroCompactor compactor) {
        if (compactor == null) {
            throw new IllegalArgumentException("compactor 不能为空");
        }
        this.compactor = compactor;
    }

    /** 单点干跑（不应用）：报告会压的 id 与可回收字符。 */
    public ShadowReport evaluate(List<BuzhouMessage> history, int currentTurnIndex,
            Function<String, MicroCompactionPolicy> policyByToolName, int protectRecentTurns,
            double evictRatio) {
        MicroCompactionResult result = compactor.compact(
                history, currentTurnIndex, policyByToolName, protectRecentTurns, evictRatio);
        return new ShadowReport(evictRatio, result.compactedMessageIds(), result.reclaimedChars());
    }

    /** 梯度干跑（0.25/0.5/0.75/1.0）——evictRatio 调参表（reclaimedChars 随比例单调不减）。 */
    public List<ShadowReport> sweep(List<BuzhouMessage> history, int currentTurnIndex,
            Function<String, MicroCompactionPolicy> policyByToolName, int protectRecentTurns) {
        List<ShadowReport> reports = new java.util.ArrayList<>(SWEEP_RATIOS.length);
        for (double ratio : SWEEP_RATIOS) {
            reports.add(evaluate(history, currentTurnIndex, policyByToolName, protectRecentTurns, ratio));
        }
        return List.copyOf(reports);
    }

    /** 干跑 + 事件外发（观测面：候选数/回收字符/比例——不应用的评估声明在事件类型里）。 */
    public ShadowReport evaluateAndEmit(List<BuzhouMessage> history, int currentTurnIndex,
            Function<String, MicroCompactionPolicy> policyByToolName, int protectRecentTurns,
            double evictRatio, java.util.function.Consumer<SessionEvent> emitter) {
        ShadowReport report = evaluate(history, currentTurnIndex, policyByToolName,
                protectRecentTurns, evictRatio);
        if (emitter != null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("candidateCount", report.wouldCompactIds().size());
            payload.put("reclaimedChars", report.reclaimedChars());
            payload.put("evictRatio", report.evictRatio());
            payload.put("applied", false); // 显式声明：影子干跑不应用
            emitter.accept(new SessionEvent(EVENT_SHADOW, payload, java.time.Instant.now()));
        }
        return report;
    }
}
