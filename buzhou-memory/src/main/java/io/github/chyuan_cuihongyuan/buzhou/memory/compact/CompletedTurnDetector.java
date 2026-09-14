package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;
/** spec 1529 / T2309：已完成轮检测 SPI——微压缩回收安全点的判定契约。 */

public interface CompletedTurnDetector {

    List<TurnSpan> detectTurns(List<BuzhouMessage> history);
}
