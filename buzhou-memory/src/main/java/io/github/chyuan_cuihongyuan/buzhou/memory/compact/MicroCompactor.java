package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;
import java.util.function.Function;

public interface MicroCompactor {

    MicroCompactionResult compact(List<BuzhouMessage> history,
                                  int currentTurnIndex,
                                  Function<String, MicroCompactionPolicy> policyByToolName,
                                  int protectRecentTurns);
}
