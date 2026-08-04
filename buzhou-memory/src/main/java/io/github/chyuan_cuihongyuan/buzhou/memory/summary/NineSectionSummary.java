package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import java.util.EnumMap;
import java.util.Map;

public record NineSectionSummary(long generation, int coversUpToTurn,
                                 EnumMap<SummarySection, SectionContent> sections) {

    public static NineSectionSummary empty() {
        return new NineSectionSummary(0, 0, new EnumMap<>(SummarySection.class));
    }

    public NineSectionSummary withSections(EnumMap<SummarySection, SectionContent> newSections,
                                           int newCoversUpToTurn) {
        return new NineSectionSummary(generation + 1, newCoversUpToTurn, newSections);
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SummarySection, SectionContent> entry : sections.entrySet()) {
            if (entry.getValue() == null || entry.getValue().body().isBlank()) {
                continue;
            }
            sb.append("## ").append(entry.getKey().name())
                    .append("（").append(entry.getKey().displayName()).append("）\n")
                    .append(entry.getValue().render()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 把文本追加到 CURRENT_STATE 段（保证事实经摘要 P0 段保留，压缩不丢现场）。
     * 若 CURRENT_STATE 段不存在则新建。返回新实例（不可变）。
     */
    public NineSectionSummary appendCurrentState(String text) {
        EnumMap<SummarySection, SectionContent> copy = new EnumMap<>(sections);
        SectionContent existing = copy.get(SummarySection.CURRENT_STATE);
        String newBody = text == null || text.isBlank() ? ""
                : (existing == null || existing.body() == null || existing.body().isBlank()
                        ? text : existing.body() + "\n" + text);
        SectionContent form = existing == null ? SectionContent.full(newBody)
                : new SectionContent(newBody, existing.form(), existing.evidenceIds());
        copy.put(SummarySection.CURRENT_STATE, form);
        return new NineSectionSummary(generation, coversUpToTurn, copy);
    }
}
