package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;
/** spec 1529 / T2309：微压缩结果——回收计数与占位映射的不可变快照。 */

public record MicroCompactionResult(
        List<BuzhouMessage> compactedView,
        List<String> compactedMessageIds,
        int reclaimedChars) {
}
