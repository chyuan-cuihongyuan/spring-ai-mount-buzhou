package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;

/**
 * 压缩结果监听器（spec 34 §A / T115；spec 38 §A / T135 扩展 evictRatio）：
 * 实际折入时回调（主路径 + 梯子每级）；异常吞（观测双写不影响视图主链）。
 *
 * <p>spec 95 §A / T355：新增 {@link #onSummaryFolded} 摘要折入通知（trigger 溯源：
 * budget=预算触发 / backlog=积压触发 / drift=漂移触发——spec 70/90 双信号的观测面）；
 * default 空实现保 lambda 兼容（只关心微压缩的监听器零改动）。
 *
 * @since 1.0.0
 */
public interface CompactionListener {

    /**
     * @param sessionId  会话
     * @param result     本次压缩结果（compactedMessageIds/reclaimedChars）
     * @param evictRatio 本次逐出比例（梯子加压时为当前级，如 0.8/0.9/1.0）
     */
    void onCompacted(String sessionId, MicroCompactionResult result, double evictRatio);

    /**
     * 摘要折入成功通知（spec 95 §A / T355；@since 1.0.0）。结构化摘要保存成功后
     * 回调——trigger 标注本次折入的判据来源。
     *
     * @param sessionId 会话
     * @param summary   折入后的新摘要（generation/coversUpToTurn 已推进）
     * @param trigger   触发判据：budget（预算压）/ backlog（积压阈值）/ drift（语义漂移）
     */
    default void onSummaryFolded(String sessionId, NineSectionSummary summary, String trigger) {
    }
}
