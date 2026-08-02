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
}
