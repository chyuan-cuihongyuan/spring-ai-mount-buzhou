package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;

public interface CompletedTurnDetector {

    List<TurnSpan> detectTurns(List<BuzhouMessage> history);
}
