package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
/** spec 1529 / T2309：结构化摘要生成 SPI——九段式摘要的产出契约（摘要模型驱动）。 */

public interface SummaryGenerator {

    NineSectionSummary merge(NineSectionSummary previous, List<BuzhouMessage> newTurns,
                             int coversUpToTurn, String extraInstruction, ChatModel model);
}
