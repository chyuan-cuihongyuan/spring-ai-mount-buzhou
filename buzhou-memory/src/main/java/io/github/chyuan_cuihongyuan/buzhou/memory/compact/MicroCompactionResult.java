package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;

public record MicroCompactionResult(
        List<BuzhouMessage> compactedView,
        List<String> compactedMessageIds,
        int reclaimedChars) {
}
