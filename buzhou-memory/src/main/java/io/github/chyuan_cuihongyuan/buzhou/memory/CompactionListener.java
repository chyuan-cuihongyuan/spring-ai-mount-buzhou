package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult;

/**
 * 压缩结果监听器（spec 34 §A / T115；spec 38 §A / T135 扩展 evictRatio）：
 * 实际折入时回调（主路径 + 梯子每级）；异常吞（观测双写不影响视图主链）。
 */
@FunctionalInterface
public interface CompactionListener {

    /**
     * @param sessionId  会话
     * @param result     本次压缩结果（compactedMessageIds/reclaimedChars）
     * @param evictRatio 本次逐出比例（梯子加压时为当前级，如 0.8/0.9/1.0）
     */
    void onCompacted(String sessionId, MicroCompactionResult result, double evictRatio);
}
