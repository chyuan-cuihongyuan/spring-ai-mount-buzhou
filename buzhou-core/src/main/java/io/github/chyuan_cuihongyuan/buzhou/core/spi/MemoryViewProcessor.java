package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;

public interface MemoryViewProcessor {

    List<BuzhouMessage> process(String sessionId, List<BuzhouMessage> stored, int currentTurn);
}
