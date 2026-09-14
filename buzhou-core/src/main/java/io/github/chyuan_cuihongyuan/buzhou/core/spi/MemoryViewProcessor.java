package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;

/**
 * 记忆视图处理器——RuntimeConfig 槽位，对注入模型的记忆视图做终改写。
 */
public interface MemoryViewProcessor {

    List<BuzhouMessage> process(String sessionId, List<BuzhouMessage> stored, int currentTurn);
}
