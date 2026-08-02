package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.EnumMap;
import java.util.List;

public class DefaultSummaryGenerator implements SummaryGenerator {

    @Override
    public NineSectionSummary merge(NineSectionSummary previous, List<BuzhouMessage> newTurns,
                                    int coversUpToTurn, String extraInstruction, ChatModel model) {
        String prompt = buildPrompt(previous, newTurns, extraInstruction);
        String output = model.call(new Prompt(List.of(new UserMessage(prompt))))
                .getResult().getOutput().getText();
        EnumMap<SummarySection, SectionContent> sections = parse(output);
        long generation = previous == null ? 1 : previous.generation() + 1;
        return new NineSectionSummary(generation, coversUpToTurn, sections);
    }

    String buildPrompt(NineSectionSummary previous, List<BuzhouMessage> newTurns,
                       String extraInstruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是会话压缩器。把对话历史压缩为九段结构化摘要，供后续轮次续接上下文。\n");
        sb.append("先在 <analysis> 中按时间序复盘要点，再按下列九段输出，每段以 `## 段名` 开头：\n");
        for (SummarySection section : SummarySection.values()) {
            sb.append("## ").append(section.name())
                    .append("（").append(section.displayName()).append("）\n");
        }
        if (extraInstruction != null && !extraInstruction.isBlank()) {
            sb.append("额外指令：").append(extraInstruction).append("\n");
        }
        if (previous != null && previous.generation() > 0) {
            sb.append("\n已有摘要（第 ").append(previous.generation())
                    .append(" 代），在其基础上**合并更新**，保留仍有效的事实：\n")
                    .append(previous.render());
        }
        sb.append("\n新增对话（逐条）：\n");
        for (BuzhouMessage message : newTurns) {
            sb.append("[").append(message.role()).append("] ")
                    .append(truncate(message.content())).append("\n");
        }
        return sb.toString();
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= 500 ? content : content.substring(0, 500) + "…";
    }

    EnumMap<SummarySection, SectionContent> parse(String output) {
        EnumMap<SummarySection, SectionContent> sections = new EnumMap<>(SummarySection.class);
        String body = output;
        int analysisEnd = body.indexOf("</analysis>");
        if (analysisEnd >= 0) {
            body = body.substring(analysisEnd + "</analysis>".length());
        }
        String[] chunks = body.split("(?m)^##\\s*");
        for (String chunk : chunks) {
            if (chunk.isBlank()) {
                continue;
            }
            int newline = chunk.indexOf('\n');
            String heading = (newline < 0 ? chunk : chunk.substring(0, newline)).strip();
            String sectionBody = newline < 0 ? "" : chunk.substring(newline + 1).strip();
            for (SummarySection section : SummarySection.values()) {
                if (heading.startsWith(section.name())) {
                    sections.put(section, SectionContent.full(sectionBody));
                }
            }
        }
        return sections;
    }
}
