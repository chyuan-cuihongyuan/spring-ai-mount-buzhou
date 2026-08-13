package io.github.chyuan_cuihongyuan.buzhou.memory.consolidation;

import io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryFactReconciler;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * sleep-time 整理器（wayfinder2 impl-11 / T37 / docs/spec/12）：把对账/去冗余挪出热路径——
 * turn 后异步对最新摘要<b>重跑事实对账</b>（ADD/UPDATE/DELETE/NOOP，解析失败一律 NOOP 韧性），
 * 全走双时序台账（valid_from/until 时序回查天然支持）；失败退避由调度器吞掉、下周期重试。
 *
 * <p>来源 Letta sleep-time agent（去冗余/重排 memory blocks、后台 Run 不阻塞主响应）。
 * 本实现整理「事实一致性」（对账 + 台账），P0–P3 重排与摘要重写发生在下一轮压缩生成时
 * （既有链路），此处不做有损改写。
 */
public class SleepTimeConsolidator {

    /** 整理结果（监听者可观测）。 */
    public record ConsolidationOutcome(String sessionId, boolean ran, String detail) {
    }

    private final SummaryStoreBridge summaryBridge;
    private final ChatModel summaryModel;
    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore sessionStateStore;
    private final Consumer<ConsolidationOutcome> listener;

    public SleepTimeConsolidator(SummaryStoreBridge summaryBridge, ChatModel summaryModel,
                                 io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore sessionStateStore,
                                 Consumer<ConsolidationOutcome> listener) {
        this.summaryBridge = summaryBridge;
        this.summaryModel = summaryModel;
        this.sessionStateStore = sessionStateStore;
        this.listener = listener;
    }

    /** 对最新摘要重跑事实对账（无摘要 = 无事可整理）。 */
    public void consolidate(String sessionId) {
        Optional<NineSectionSummary> latest = summaryBridge.loadLatest(sessionId);
        if (latest.isEmpty()) {
            notify(new ConsolidationOutcome(sessionId, false, "no-summary"));
            return;
        }
        try {
            NineSectionSummary reconciled = new SummaryFactReconciler().reconcile(
                    sessionId, latest.get(), latest.get(), summaryModel, null,
                    sessionStateStore == null ? null
                            : new BiTemporalFactLedger(sessionStateStore));
            if (reconciled != latest.get()) {
                summaryBridge.save(sessionId, reconciled);
            }
            notify(new ConsolidationOutcome(sessionId, true, "reconciled"));
        } catch (RuntimeException e) {
            // 韧性：整理失败绝不影响会话（下个周期重试）
            notify(new ConsolidationOutcome(sessionId, false, "failed: " + e.getMessage()));
        }
    }

    private void notify(ConsolidationOutcome outcome) {
        if (listener != null) {
            listener.accept(outcome);
        }
    }
}
