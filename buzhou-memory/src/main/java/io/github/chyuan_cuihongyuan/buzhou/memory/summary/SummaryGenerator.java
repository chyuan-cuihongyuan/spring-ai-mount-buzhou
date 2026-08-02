package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

public interface SummaryGenerator {

    NineSectionSummary merge(NineSectionSummary previous, List<BuzhouMessage> newTurns,
                             int coversUpToTurn, String extraInstruction, ChatModel model);
}
