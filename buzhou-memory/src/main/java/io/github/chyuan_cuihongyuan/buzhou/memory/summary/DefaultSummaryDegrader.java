package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public class DefaultSummaryDegrader implements SummaryDegrader {

    @Override
    public NineSectionSummary degradeToFit(NineSectionSummary summary, int maxTokens,
                                           TokenEstimator estimator) {
        EnumMap<SummarySection, SectionContent> sections =
                new EnumMap<>(summary.sections());
        while (tokensOf(sections, estimator) > maxTokens) {
            SummarySection candidate = sections.entrySet().stream()
                    .filter(e -> e.getKey().priority() > 0)
                    .filter(e -> e.getValue().form() == SectionContent.Form.FULL)
                    .max(Comparator.comparingInt(e -> e.getKey().priority()))
                    .map(java.util.Map.Entry::getKey)
                    .orElse(null);
            if (candidate == null) {
                break;
            }
            SectionContent full = sections.get(candidate);
            sections.put(candidate, new SectionContent(gistOf(full.body()),
                    SectionContent.Form.GIST, full.evidenceIds()));
        }
        return new NineSectionSummary(summary.generation(), summary.coversUpToTurn(), sections);
    }

    private int tokensOf(EnumMap<SummarySection, SectionContent> sections, TokenEstimator estimator) {
        return sections.values().stream()
                .mapToInt(c -> estimator.estimate(c.render()))
                .sum();
    }

    private String gistOf(String body) {
        if (body == null) {
            return "";
        }
        int end = body.indexOf('。');
        if (end > 0 && end < 120) {
            return body.substring(0, end + 1);
        }
        return body.length() <= 60 ? body : body.substring(0, 60) + "…";
    }
}
